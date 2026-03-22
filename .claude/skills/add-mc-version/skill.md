---
name: add-mc-version
description: 为 BlackBoxPro 新增一个 Minecraft 版本的测试支持。自动完成 mod 代码生成、环境搭建、构建验证、测试执行、验收判断全流程。验收标准：全量测试 failed=0 且 passed ≥ 参考版本。
---

# BlackBoxPro 新版本接入技能

## 用法

```
/add-mc-version <mc_version> [loader=fabric|neoforge|both] [ref_version=1.21.11]
```

示例：
- `/add-mc-version 1.21.1` → 接入 1.21.1 Fabric（默认）
- `/add-mc-version 1.21.1 loader=both` → 同时接入 Fabric + NeoForge

---

## 验收标准

**通过条件（必须同时满足）：**
1. `failed == 0`（无失败用例）
2. `passed >= ref_passed - 2`（通过数不低于参考版本，允许 ±2 容差）
3. 服务端正常启动（日志出现 `Done`）
4. 客户端 mod 正常加载（日志出现 `All actions registered`）
5. 连接服务器成功（服务端日志出现 `joined the game`）

**不通过时的处理原则：**
- 编译错误 → 分析 API 差异，修复代码后重试（最多 3 次）
- 测试失败 → 判断是版本不支持（加豁免列表）还是 bug（修复重试）
- 环境问题 → 报告具体错误，给出操作指引

---

## 执行流程

### 阶段 0：前置检查

1. 停止当前服务端（若有）：
   `curl -s POST http://localhost:38080/execute {"action":"stop_server"}`
2. 检查端口 25565 无占用
3. 确认 mod/{mc_version} 是否已存在（存在则跳过代码生成）

---

### 阶段 1：代码生成（mod 侧）

**1.1 查询版本号**

```bash
MC_VER="{mc_version}"

# Yarn mappings
YARN=$(curl -s "https://meta.fabricmc.net/v2/versions/yarn/${MC_VER}" | python3 -c "
import json,sys; d=json.load(sys.stdin); print(d[0]['version'] if d else 'NOT_FOUND')")

# Fabric loader
LOADER=$(curl -s "https://meta.fabricmc.net/v2/versions/loader/${MC_VER}" | python3 -c "
import json,sys; d=json.load(sys.stdin); print(d[0]['loader']['version'] if d else 'NOT_FOUND')")

# Fabric API
FABRIC_API=$(curl -s "https://api.modrinth.com/v2/project/fabric-api/version?game_versions=%5B%22${MC_VER}%22%5D&loaders=%5B%22fabric%22%5D" | python3 -c "
import json,sys; d=json.load(sys.stdin); print(d[0]['version_number'] if d else 'NOT_FOUND')")

echo "yarn=$YARN loader=$LOADER fabric_api=$FABRIC_API"
```

**1.2 复制目录并替换版本号**

```bash
cd /f/minecraft/mod/BlackBoxPro/mod
cp -r 1.21.11 {mc_version}

# 修改 gradle.properties
# 替换字段：minecraft_version, yarn_mappings, loader_version, fabric_version, neoforge_version
```

修改 `mod/{mc_version}/gradle.properties`（关键字段）：
```properties
minecraft_version={mc_version}
yarn_mappings={YARN}
loader_version={LOADER}
fabric_version={FABRIC_API}+{mc_version}
```

**1.3 更新 mod 元数据**

- `fabric/src/main/resources/fabric.mod.json`：更新 `minecraft` 依赖版本范围为 `>={mc_version}`
- `neoforge/src/main/resources/META-INF/mods.toml`：更新版本范围

**1.4 注册到 mod/settings.gradle.kts**

在文件末尾追加：
```kotlin
include("{mc_version}:runtime")
include("{mc_version}:fabric")

project(":{mc_version}").projectDir = file("{mc_version}")
project(":{mc_version}:runtime").projectDir = file("{mc_version}/runtime")
project(":{mc_version}:fabric").projectDir = file("{mc_version}/fabric")
```

---

### 阶段 2：环境搭建

**2.1 下载 Paper 服务端**

```bash
MC_VER="{mc_version}"
SERVER_DIR="F:/minecraft/server/paper-${MC_VER}"

BUILD=$(curl -s "https://api.papermc.io/v2/projects/paper/versions/${MC_VER}/builds" | \
  python3 -c "import json,sys; d=json.load(sys.stdin); print(d['builds'][-1]['build'] if d.get('builds') else 'NOT_FOUND')")

JAR=$(curl -s "https://api.papermc.io/v2/projects/paper/versions/${MC_VER}/builds/${BUILD}" | \
  python3 -c "import json,sys; d=json.load(sys.stdin); print(d['downloads']['application']['name'])")

powershell -NoProfile -Command "New-Item -ItemType Directory -Force '${SERVER_DIR}' | Out-Null"
curl -o "${SERVER_DIR}/${JAR}" "https://api.papermc.io/v2/projects/paper/versions/${MC_VER}/builds/${BUILD}/downloads/${JAR}"
echo "eula=true" > "${SERVER_DIR}/eula.txt"
```

**2.2 首次启动生成配置，然后关闭在线验证**

```bash
# Terminal 1：首次启动（生成 server.properties）
cd /f/minecraft/server/paper-{mc_version}
"/c/Program Files/Java/jdk-21/bin/java.exe" -Xms2G -Xmx2G -jar {jar} nogui
# 等待 Done 后立即停服
curl -X POST http://localhost:38080/execute -d '{"id":"s","action":"stop_server"}'

# 关闭在线验证
powershell -NoProfile -Command "
(Get-Content '${SERVER_DIR}/server.properties') -replace 'online-mode=true','online-mode=false' | Set-Content '${SERVER_DIR}/server.properties'"
powershell -NoProfile -Command "
(Get-Content '${SERVER_DIR}/server.properties') -replace 'enforce-secure-profile=true','enforce-secure-profile=false' | Set-Content '${SERVER_DIR}/server.properties'"
# enforce-secure-profile: 1.19.1+ 新增，开发账号必须关闭，否则离线玩家被拒连接
```

**2.3 部署 plugin**

```bash
powershell -NoProfile -Command "
Copy-Item 'F:\minecraft\mod\BlackBoxPro\plugin\build\libs\BlackBoxPro-Plugin-*.jar' '${SERVER_DIR}\plugins\' -Force"
```

---

### 阶段 3：构建验证

```bash
cd /f/minecraft/mod/BlackBoxPro/mod
JAVA_HOME="/c/Program Files/Java/jdk-21" ./gradlew :{mc_version}:fabric:build --no-daemon 2>&1
```

**编译错误处理规则：**

| 错误特征 | 处理方式 |
|---|---|
| `Unresolved reference: XxxScreen/XxxPacket` | 查 Yarn 新映射，更新 import 或类名 |
| `None of the following candidates` | 方法签名变化，查新版源码调整参数 |
| `type mismatch: actual 'T?' expected 'T'` | nullable 变化，加 `?: fallback` |
| `Could not resolve…fabric-api` | 更新 fabric_version 为正确版本 |
| daemon 内存/锁文件错误 | `./gradlew --stop` 后重试 |

最多修复重试 3 次，否则输出报告等待人工介入。

---

### 阶段 4：测试执行

**Step 1：启动服务端（Terminal 1）**
```bash
cd /f/minecraft/server/paper-{mc_version}
"/c/Program Files/Java/jdk-21/bin/java.exe" -Xms2G -Xmx4G -XX:+UseG1GC -jar {jar} nogui
```
等待日志出现 `Done`。

**Step 2：启动客户端（Terminal 2）**
```bash
cd /f/minecraft/mod/BlackBoxPro/mod
JAVA_HOME="/c/Program Files/Java/jdk-21" ./gradlew :{mc_version}:fabric:runClient --no-daemon
```
轮询 `netstat :38081 LISTENING`（每 2s，最多 120s）。
记录日志中 `Setting user: PlayerXXX` 的玩家名。

**Step 3：连接服务器**
```bash
curl -s --max-time 35 -X POST http://localhost:38081/execute \
  -H "Content-Type: application/json" \
  -d '{"id":"c1","action":"connect_to_server","params":{"ip":"127.0.0.1","port":25565}}'
```
等待服务端日志出现 `joined the game`。
若出现 `DisconnectedScreen`，检查 online-mode 并重连。

**Step 4：运行全量测试**
```bash
curl -s -X POST http://localhost:38080/execute \
  -H "Content-Type: application/json" \
  -d "{\"id\":\"run\",\"action\":\"run_test\",\"params\":{\"player\":\"{PLAYER}\",\"scope\":\"full\"}}" \
  -o result_{mc_version}.json
```
阻塞等待完成（约 40-60s）。

**Step 5：停服停客户端**
```bash
curl -s -X POST http://localhost:38080/execute -d '{"id":"stop","action":"stop_server"}'
# Terminal 2: Ctrl-C
```

---

### 阶段 5：验收判断

```python
import json

r = json.load(open(f"result_{mc_version}.json", encoding="utf-8"))["data"]
passed, failed, skipped = r["passed"], r["failed"], r["skipped"]
ref_passed = 54  # 参考版本（1.21.11）

print(f"结果：{passed}/{failed}/{skipped}")

if failed == 0 and passed >= ref_passed - 2:
    print("✅ 验收通过！")
else:
    print("❌ 验收未通过，失败分析：")
    for c in r["results"]:
        if c["status"] == "failed":
            msg = c.get("message", "")
            a = c["action"]
            if "Not connected" in msg or "Player not available" in msg:
                print(f"  [断线] {a} → 参考已有断线处理（add to skip list）")
            elif "Invalid params" in msg or "Missing required field" in msg:
                print(f"  [参数] {a}: {msg} → 在 BlackBoxTestCatalog 增加版本分支")
            elif "不支持" in msg or "not supported" in msg.lower():
                print(f"  [版本] {a} → 加入 unsupportedOn 豁免列表")
            else:
                print(f"  [未知] {a}: {msg}")
```

**自动修复策略：**
1. 参数格式变化 → 在 `BlackBoxTestCatalog` execute lambda 的 `when(actionId)` 中，参照 1.12.2 分支增加新版本分支
2. 版本不支持的 action → 在 `BlackBoxTestProfile.kt` 增加 `unsupportedOnXxx` 集合，在 `buildCase` 的 `prepare` 中判断
3. 断线问题 → 加入 prepareFixture 的"待排查"跳过分支
4. 修复后重新构建测试，回到阶段 3 循环（最多 3 次）

---

### 阶段 6：收尾

**更新 blackbox-runner/SKILL.md 版本速查表**

在版本环境速查章节添加：
```markdown
### {mc_version}

| 项目 | 值 |
|------|-----|
| Java（Gradle/运行时） | `C:\Program Files\Java\jdk-21` |
| 服务端目录 | `F:\minecraft\server\paper-{mc_version}` |
| 服务端 JAR | `{jar_name}` |
| 客户端启动 | `cd mod && JAVA_HOME="C:/Program Files/Java/jdk-21" ./gradlew :{mc_version}:fabric:runClient` |
| 玩家名 | `{player_name}`（runClient 开发模式随机） |
| 最新测试结果 | `{passed}/{failed}/{skipped}` |
```

**更新本 skill 的版本接入记录表（末尾）**

**Git commit**
```bash
git add mod/{mc_version}/ .claude/skills/ plugin/
git commit -m "feat: 新增 {mc_version} Fabric 测试支持，通过率 {passed}/{total}"
git push
```

---

## 常见问题

### Q: Paper 没有该版本

检查：`curl -s "https://api.papermc.io/v2/projects/paper" | python3 -c "import json,sys; print(json.load(sys.stdin)['versions'])"`

若无 Paper 则考虑用 Folia 或等待 Paper 支持。

### Q: Fabric API 找不到（返回 NOT_FOUND）

该版本 Fabric API 可能尚未发布或版本号格式不同，手动去 https://modrinth.com/mod/fabric-api/versions 查找。

### Q: NeoForge 版本号格式

- 1.21.11 → `21.11.x`
- 1.21.1 → `21.1.x`
- 规律：`{MC_MAJOR}.{MC_MINOR}.x`

### Q: 玩家名动态获取

连接后查 tab 列表：
curl -X POST http://localhost:38081/execute -H Content-Type:application/json -d action=query_tab_list

---

## 版本接入记录

| MC 版本 | 加载器 | 通过/失败/跳过 | 接入日期 | 备注 |
|---|---|---|---|---|
| 1.12.2 | Forge | 52/0/54 | 2026-03-22 | 基线版本 |
| 1.21.11 | Fabric | 54/0/52 | 2026-03-22 | 主测版本，参考基准 |

Base directory for this skill: F:\minecraft\mod\BlackBoxPro\.claude\skillsdd-mc-version
