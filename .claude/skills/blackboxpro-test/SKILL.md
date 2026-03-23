---
name: blackboxpro-test
description: 部署并测试 BlackBoxPro 黑盒测试环境。两种模式：客户端自测（单人世界 HTTP 直达 mod:38081）和服务端联调（plugin:38080 转发 mod:38081）。覆盖构建、部署、启动、测试、截图视觉分析。
---

BlackBoxPro 自动化测试部署技能。根据用户指定的版本和模式，执行构建→启动→测试→清理全流程。

## 参考资源（Level 3 按需加载）

本技能的 `reference/` 目录包含以下资源，**不要预加载**，在需要时用 Read 工具按需读取：

| 文件 | 用途 | 何时读取 |
|------|------|---------|
| `reference/action-catalog.md` | 全部 106 个 Action 的 ID、参数、分类 | 用户问"有哪些 action"、需要查参数、按分类筛选时 |
| `reference/http-api.md` | HTTP 端点格式（请求/响应 JSON 结构） | 需要确认 API 调用格式时 |
| `reference/register-action.md` | 注册第三方自定义 Action 的完整流程 | 用户要添加新 action、扩展测试能力时 |

示例场景：
- 用户："查询类的 action 有哪些？" → Read `reference/action-catalog.md`，定位"查询行为"章节
- 用户："screenshot 需要什么参数？" → Read `reference/action-catalog.md`，搜索 `screenshot`
- 用户："请求格式是什么？" → Read `reference/http-api.md`

## 通用规则

### 轮询规则

**禁止任何单次等待超过 8 秒。** 所有需要等待的场景必须使用轮询循环：

```
每 N 秒检查一次，最多 M 次，超过则判定失败
```

各场景的轮询参数：

| 场景 | 间隔 | 最大次数 | 总上限 | 判定成功标志 |
|------|------|---------|--------|-------------|
| HTTP 就绪 (:38081) | 3s | 40 | 120s | `curl -sf localhost:38081/status` 返回 0 |
| HTTP 就绪 (:38080) | 3s | 40 | 120s | `curl -sf localhost:38080/status` 返回 0 |
| 服务端启动 | 3s | 40 | 120s | 日志包含 `Done` |
| 世界创建/加入 | 已由 curl 阻塞等待响应，无需轮询（curl 自带 --max-time 90） | | | |
| 客户端连接服务器 | 3s | 20 | 60s | 服务端日志包含 `joined the game` |

轮询实现：使用 Bash `for` 循环 + `sleep`，例如：
```bash
for i in $(seq 1 40); do
  curl -sf http://localhost:38081/status > /dev/null 2>&1 && break
  [ $i -eq 40 ] && echo "FAIL: Mod HTTP not ready after 120s" && exit 1
  sleep 3
done
```

### 进程启动规则

**优先使用 Terminal 技能启动进程，无匹配技能时才降级到 Bash background。**

启动客户端或服务端时，按以下优先级决策：

| 优先级 | 方式 | 条件 |
|--------|------|------|
| ① 高 | 调用对应的 Terminal 技能（`Skill` 工具） | 当前会话有匹配版本的 client/server 技能 |
| ② 低 | `Bash run_in_background: true` | 无匹配技能，或技能不适用 |

**版本→技能映射**：

| 版本 | 角色 | 技能名 |
|------|------|--------|
| 1.21.11 | 客户端 | `client` |
| 1.21.11 | 服务端 | `server` |
| 1.12.2  | 客户端 | `client-1.12.2` |
| 1.12.2  | 服务端 | `server-1.12.2` |

使用 Terminal 技能时，直接调用 `Skill` 工具并传入启动指令，无需手动拼命令：
```
Skill("client")       → 启动 1.21.11 Fabric 客户端
Skill("client-1.12.2") → 启动 1.12.2 Forge 客户端
Skill("server")       → 启动 1.21.11 服务端
Skill("server-1.12.2") → 启动 1.12.2 服务端
```

Terminal 技能启动后，**读取日志也通过同一技能**（查看日志指令），不要用 `TaskOutput` 读取。

降级到 Bash background 时，使用 `TaskOutput` 读取后台输出。

### 工程/配置约定（非常重要）

- **本技能运行目录 = 接入/使用 BBP 的项目仓库根目录**（即你当前执行技能的项目）。
- **配置文件也属于接入项目**：每次执行前必须先读取 `./.claude/config/blackboxpro-env.json`（不是 BBP 仓库里的配置）。

### 定位或拉取 BBP 仓库（bbp.* → BBP_ROOT）

先根据配置计算 `BBP_ROOT`（BlackBoxPro 仓库根目录）：

| 配置项 | 说明 | 示例 |
|--------|------|------|
| `bbp.method` | `local`（本机路径）或 `git`（自动克隆） | `local` |
| `bbp.localPath` | BBP 仓库根目录（仅 local） | `D:/repos/BlackBoxPro` |
| `bbp.git.url` | BBP git 地址（仅 git） | `https://github.com/<org>/BlackBoxPro.git` |
| `bbp.git.ref` | 分支/Tag/Commit（仅 git，可空=默认分支） | `main` |
| `bbp.git.cloneDir` | 克隆目录（仅 git，相对“接入项目”根目录） | `.bbp/BlackBoxPro` |

解析规则：
1. `local`：`BBP_ROOT = bbp.localPath`
2. `git`：若 `cloneDir` 不存在则先 `git clone`，再（可选）checkout `ref`；`BBP_ROOT = <cloneDir>`
3. 校验：`BBP_ROOT` 下必须存在 `gradlew` 或 `gradlew.bat`

示例脚本（git 模式，在“接入项目根目录”执行）：
```bash
CLONE_DIR="<bbp.git.cloneDir>"
[ -d "$CLONE_DIR/.git" ] || git clone "<bbp.git.url>" "$CLONE_DIR"
[ -z "<bbp.git.ref>" ] || (cd "$CLONE_DIR" && git checkout "<bbp.git.ref>")
BBP_ROOT="$CLONE_DIR"
```

### 运行参数配置（versions.*）

若关键字段为空，暂停流程，用 AskUserQuestion 逐项询问后回填配置文件。

**方案 A 必填项**：`client.launchCommand`
**方案 B 额外必填**：`server.directory` + `server.jar`

| 配置项 | 说明 | 示例 |
|--------|------|------|
| `client.launchMethod` | `runClient`（Gradle）或 `launcher`（外部启动器） | `runClient` |
| `client.launchCommand` | 启动客户端的完整命令 | `./gradlew.bat --no-daemon :1.21.11:fabric:runClient` |
| `client.launchCwd` | 工作目录（相对 `BBP_ROOT`；空=`BBP_ROOT`） | `mod` |
| `client.javaHome` | JAVA_HOME（空=系统默认） | `C:/Program Files/Java/jdk-21` |
| `client.playerName` | 游戏内玩家名 | `Player` |
| `server.directory` | 服务端安装目录（绝对路径或相对接入项目根） | `D:/mc-server/paper-1.21.11` |
| `server.jar` | 服务端 JAR 文件名 | `paper-1.21.11-97.jar` |
| `server.javaPath` | 服务端 java 路径（空=`java`） | `java` |
| `server.jvmArgs` | JVM 参数 | `-Xms4G -Xmx4G -XX:+UseG1GC` |
| `build.javaHome` | 构建用 JAVA_HOME（空=系统默认） | 同 client.javaHome |

## 决策树

```
用户请求测试
    │
    ├─ 指定了版本？
    │   ├─ 是 → 使用指定版本
    │   └─ 否 → 询问用户选择 1.21.11 / 1.12.2
    │
    ├─ 指定了模式？
    │   ├─ 是 → 使用指定模式
    │   └─ 否 → 默认方案 A（客户端自测）
    │
    └─ 读取配置 → 缺失则询问 → 进入对应流程
```

## 两种部署模式

| 模式 | 名称 | 描述 | HTTP 端口 |
|------|------|------|-----------|
| A | 客户端自测 | 仅 Mod，单人世界，无需服务端 | 直达 `:38081` |
| B | 服务端联调 | Plugin + Mod，多人服务器 | Plugin `:38080` 转发 Mod `:38081` |

## 产物路径（相对 BBP_ROOT）

| 产物 | 路径 |
|------|------|
| 1.21.11 Fabric Mod | `<BBP_ROOT>/mod/1.21.11/fabric/build/libs/BlackBoxPro-fabric-1.21.11-*.jar` |
| 1.21.11 NeoForge Mod | `<BBP_ROOT>/mod/1.21.11/neoforge/build/libs/BlackBoxPro-neoforge-1.21.11-*.jar` |
| 1.12.2 Forge Mod | `<BBP_ROOT>/mod/1.12.2/forge/build/libs/BlackBoxPro-forge-1.12.2-*.jar` |
| Plugin | `<BBP_ROOT>/plugin/build/libs/BlackBoxPro-Plugin-*.jar` |

---

## 方案 A：客户端自测流程

### A1. 构建 Mod

根据版本执行：
```bash
# 所有 Gradle 构建都应在 BBP_ROOT 下执行
cd <BBP_ROOT> && ./gradlew mod_buildAll --no-daemon
cd <BBP_ROOT> && ./gradlew forge1122_build --no-daemon
```

构建失败则停止流程，向用户报告错误。

### A2. 后台启动客户端

使用 Bash 的 `run_in_background` 启动客户端。命令从配置文件读取：

```
launchCommand = config.versions[version].client.launchCommand
launchCwd = config.versions[version].client.launchCwd  （空则用 BBP_ROOT）
javaHome = config.versions[version].client.javaHome     （非空则设 JAVA_HOME）
```

组装并执行：
```bash
cd <BBP_ROOT>/<launchCwd> && JAVA_HOME="<javaHome>" <launchCommand>
```

> 必须用 `run_in_background: true`，客户端是长驻进程。

### A3. 轮询等待 HTTP 就绪

每 3 秒检查一次 `localhost:38081/status`，最多 40 次（120 秒）：

```bash
for i in $(seq 1 40); do
  curl -sf http://localhost:38081/status > /dev/null 2>&1 && echo "Mod HTTP ready" && break
  if [ $i -eq 40 ]; then echo "FAIL: Mod HTTP :38081 not ready after 120s"; exit 1; fi
  sleep 3
done
```

失败时：读取客户端后台任务输出，查找错误原因并报告。

### A3.5. 界面检测与初始化

HTTP 就绪后，**立即查询当前界面**，根据结果决定下一步：

```bash
curl -s --max-time 8 -X POST http://localhost:38081/execute \
  -H "Content-Type: application/json" \
  -d '{"id":"qs_init","action":"query_screen_state"}'
```

解析 `data.screenClass`（或 `data.type`），按以下逻辑处理：

#### 场景 1：首次启动界面（语言选择 / 初始化向导）

**判断标志**：screenClass 包含 `LanguageSelectScreen`、`InitialScreen`、`AccessibilityOnboardingScreen` 等，或 options.txt 不存在于运行目录。

**处理流程**：

1. **覆盖 options.txt**：将模板文件复制到客户端运行目录，跳过初始化向导：
   ```bash
   # 运行目录（按版本）：
   # 1.21.11 Fabric:  <BBP_ROOT>/mod/1.21.11/fabric/run/
   # 1.21.11 NeoForge:<BBP_ROOT>/mod/1.21.11/neoforge/run/
   # 1.12.2 Forge:    <BBP_ROOT>/mod/1.12.2/forge/run/
   #
   # 模板来源（优先级）：
   # 1. config.versions[version].client.optionsTemplate（若配置了绝对/相对路径）
   # 2. <BBP_ROOT>/mod/options-default.txt（BBP 内置默认模板）
   cp "<optionsTemplate>" "<runDir>/options.txt"
   ```

2. **关闭当前界面**（让客户端回到主菜单）：
   ```bash
   curl -s --max-time 8 -X POST http://localhost:38081/execute \
     -H "Content-Type: application/json" \
     -d '{"id":"cs_init","action":"close_screen"}'
   ```

3. **等待并重新查询**：等待 3s 后再次 `query_screen_state`，确认已进入主菜单（`TitleScreen` / `MainMenuScreen`）。若仍不在主菜单则截图记录并报告。

#### 场景 2：主菜单 / 标题界面

**判断标志**：screenClass 包含 `TitleScreen`、`MainMenuScreen`、`PauseScreen` 等，或 `data.type` 为 `"title"` / `"main_menu"`。

- **方案 A** → 继续 A4（创建/加入世界）
- **方案 B** → 继续 B6（连接服务器）

#### 场景 3：已在世界中（`data.open == false` 或 screenClass 为空/游戏内 HUD）

玩家已在某个世界或服务器中（可能是上次测试未正常退出）：

1. 先执行 `leave_world`（方案 A）或 `disconnect`（方案 B）退出当前世界
2. 等待 2s 后再次 `query_screen_state` 确认回到主菜单
3. 然后继续正常流程

#### 场景 4：其他未知界面

截图确认当前状态：

```bash
curl -s --max-time 8 -X POST http://localhost:38081/execute \
  -H "Content-Type: application/json" \
  -d '{"id":"ss_diag","action":"screenshot","params":{"testId":"init-diag","prefix":"screen"}}'
```

读取截图文件进行视觉分析，向用户报告当前界面类型后再决定是否继续。

### A4. 进入世界

**前提**：已通过 A3.5 确认当前处于主菜单。

自动决策逻辑（不需要用户选择）：

1. 先尝试 `create_world`：
```bash
curl -s --max-time 90 -X POST http://localhost:38081/execute \
  -H "Content-Type: application/json" \
  -d '{"id":"cw1","action":"create_world","params":{"worldName":"blackbox_test","gameMode":"creative","allowCommands":true}}'
```

2. 若响应包含 `"World already exists"`，自动 fallback 到 `join_world`：
```bash
curl -s --max-time 90 -X POST http://localhost:38081/execute \
  -H "Content-Type: application/json" \
  -d '{"id":"jw1","action":"join_world","params":{"worldName":"blackbox_test"}}'
```

3. 确认响应 `status: "success"` + `state: "in_world"` 后继续。

### A5. 执行测试

方案 A 无 Plugin，不能用 `run_test`。逐个执行 action 并汇总结果。

**推荐测试集**（按优先级）：

```bash
# 1. 基础查询（验证链路通畅）
curl -s --max-time 8 -X POST http://localhost:38081/execute \
  -H "Content-Type: application/json" \
  -d '{"id":"t1","action":"query_player_state"}'

# 2. 截图（验证渲染正常）
curl -s --max-time 8 -X POST http://localhost:38081/execute \
  -H "Content-Type: application/json" \
  -d '{"id":"t2","action":"screenshot","params":{"testId":"self-test","prefix":"verify"}}'

# 3. 其他 action（按需）
# 使用 batch 批量执行多个简单 action：
curl -s --max-time 8 -X POST http://localhost:38081/execute \
  -H "Content-Type: application/json" \
  -d '{"id":"t3","action":"batch","params":{"actions":[
    {"action":"sneak_start","params":{}},
    {"action":"wait","params":{"ticks":10}},
    {"action":"sneak_stop","params":{}},
    {"action":"query_player_state","params":{}}
  ]}}'
```

**结果收集**：每个响应记录 `{action, status, message}`。

### A6. 截图视觉分析

从 screenshot 响应中提取 `data.filePath`，用 Read 工具读取 PNG 进行视觉验证。

截图路径：
- 1.21.11：`mod/1.21.11/fabric/run/screenshots/blackboxpro/`
- 1.12.2：`mod/1.12.2/forge/run/screenshots/blackboxpro/`

### A7. 清理

```bash
# 1. 离开世界
curl -s --max-time 8 -X POST http://localhost:38081/execute \
  -H "Content-Type: application/json" \
  -d '{"id":"lw1","action":"leave_world"}'

# 2. 停止客户端后台进程
# 使用 TaskStop 终止 A2 启动的后台任务
```

### A8. 输出报告

```
=== BlackBoxPro 客户端自测报告 ===
版本: 1.12.2
模式: A（客户端自测）
世界: blackbox_test (created/joined)

测试结果:
  query_player_state: success
  screenshot: success (filePath: ...)
  batch(sneak): success
  ...

通过: N / 总计: M
截图: <filePath>（已视觉分析）
```

---

## 方案 B：服务端联调流程

### B1. 构建

```bash
cd <BBP_ROOT> && ./gradlew buildAll --no-daemon
# 或分步：
# cd <BBP_ROOT> && ./gradlew mod_buildAll --no-daemon
# cd <BBP_ROOT> && ./gradlew plugin_build --no-daemon
```

### B2. 部署 Plugin

从配置文件读取 `server.pluginDir`（或 `server.directory + "/plugins"`）：

```bash
cp <BBP_ROOT>/plugin/build/libs/BlackBoxPro-Plugin-*.jar <server.directory>/plugins/
```

> Plugin jar 被服务端占用时无法覆盖，必须先停服再部署再启服。

### B3. 后台启动服务端

```bash
cd <server.directory> && <server.javaPath> <server.jvmArgs> -jar <server.jar> nogui
```

使用 `run_in_background: true`。

轮询服务端就绪：每 3 秒读取后台任务输出，检查是否包含 `Done`，最多 40 次。

### B4. 后台启动客户端

同方案 A2。

### B5. 轮询等待双端 HTTP 就绪

先轮询 `:38080`（Plugin），再轮询 `:38081`（Mod），各 3s × 40 次。

### B5.5. 界面检测与初始化

同方案 A3.5，轮询就绪后立即查询界面状态：

```bash
curl -s --max-time 8 -X POST http://localhost:38081/execute \
  -H "Content-Type: application/json" \
  -d '{"id":"qs_init","action":"query_screen_state"}'
```

按 A3.5 的场景 1-4 处理（首次启动覆盖 options.txt、已在世界中则先退出），确认处于主菜单后继续 B6。

### B6. 连接服务器

**前提**：已通过 B5.5 确认当前处于主菜单。

```bash
curl -s --max-time 8 -X POST http://localhost:38081/execute \
  -H "Content-Type: application/json" \
  -d '{"id":"c1","action":"connect_to_server","params":{"ip":"127.0.0.1","port":25565}}'
```

**连接成功**：轮询服务端日志包含 `joined the game`，每 3s 检查一次，最多 20 次（60s）。

**连接失败处理**：若响应 `status != "success"` 或超时：

1. **立即查询界面状态**：
   ```bash
   curl -s --max-time 8 -X POST http://localhost:38081/execute \
     -H "Content-Type: application/json" \
     -d '{"id":"qs_fail","action":"query_screen_state"}'
   ```

2. **根据返回的 screenClass 判断**：

   | 界面类型 | 说明 | 处理 |
   |----------|------|------|
   | `DisconnectedScreen` / `ConnectScreen` | 连接被拒或认证失败 | 截图记录错误信息，报告给用户，停止流程 |
   | `TitleScreen` / `MainMenuScreen` | 连接未建立，仍在主菜单 | 检查服务端日志（online-mode / 端口），报告原因 |
   | 游戏内 HUD（open=false） | 连接实际已成功但响应异常 | 继续流程，跳过错误 |
   | 其他未知界面 | 状态不明 | 截图 + 视觉分析，向用户报告 |

3. **截图兜底**（query_screen_state 无法确认时）：
   ```bash
   curl -s --max-time 8 -X POST http://localhost:38081/execute \
     -H "Content-Type: application/json" \
     -d '{"id":"ss_fail","action":"screenshot","params":{"testId":"connect-fail","prefix":"diag"}}'
   ```
   读取截图进行视觉分析，向用户说明当前状态后再决定是否重试。

4. **不自动重试连接**：确认原因后报告用户，由用户决定是否继续。

### B7. 执行全量测试

通过 Plugin HTTP 执行（阻塞到完成，约 40-60s）：

```bash
curl -s --max-time 120 -X POST http://localhost:38080/execute \
  -H "Content-Type: application/json" \
  -d '{"id":"run-1","action":"run_test","params":{"player":"<playerName>","scope":"full"}}'
```

### B8. 清理

```bash
# 停止服务端
curl -s --max-time 8 -X POST http://localhost:38080/execute \
  -H "Content-Type: application/json" \
  -d '{"id":"stop","action":"stop_server"}'

# 停止客户端后台进程
# 使用 TaskStop 终止后台任务
```

### B9. 输出报告

```
=== BlackBoxPro 服务端联调报告 ===
版本: 1.21.11
模式: B（服务端联调）
玩家: Player

测试结果:
  passed: 54
  failed: 0
  skipped: 52

通过标准: failed == 0
判定: PASS
```

---

## 结果判定标准

| 指标 | PASS 条件 |
|------|----------|
| 方案 A | 所有执行的 action 响应 `status: "success"` |
| 方案 B | `run_test` 返回 `failed == 0` |

---

## 故障排查

| 症状 | 原因 | 解决方案 |
|------|------|---------|
| `:38081` 轮询超时 | Mod 未加载或崩溃 | 读取客户端后台输出，查找异常 |
| `:38080` 轮询超时 | Plugin 未启用 | 读取服务端后台输出，检查插件加载 |
| `Already in a world` | 未先 leave_world | 先执行 `leave_world` |
| `World already exists` | 同名世界 | 自动 fallback 到 `join_world` |
| `World not found` | 世界不存在 | 使用 `create_world` |
| 截图全黑 | 窗口遮挡或最小化 | 确保客户端窗口可见 |
| `Connection refused` 连接服务器 | 服务端未启动或 online-mode | 检查服务端状态，确认 online-mode=false |

## 端口速查

| 组件 | 端口 | 用途 |
|------|------|------|
| Plugin HTTP | 38080 | 测试脚本指令入口（方案 B） |
| Mod HTTP | 38081 | Mod 直达（方案 A）/ Plugin 转发目标（方案 B） |
| Minecraft Server | 25565 | 游戏连接 |

## 注意事项

- 两个 MC 版本不要同时启动（都占用 25565 和 38081）
- 修改 Plugin 后必须停服 → 部署 → 重启
- 修改 Mod 后需重启客户端
- 所有控制通过 HTTP，禁止使用 RCON
