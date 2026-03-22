---
name: deploying-blackbox
description: 部署并测试 BlackBoxPro 黑盒测试环境。两种模式：客户端自测（单人世界 HTTP 直达 mod:38081）和服务端联调（plugin:38080 转发 mod:38081）。覆盖构建、部署、启动、测试、截图视觉分析。
---

BlackBoxPro 自动化测试部署技能。根据用户指定的版本和模式，执行构建→启动→测试→清理全流程。

## 参考资源（Level 3 按需加载）

本技能的 `reference/` 目录包含以下资源，**不要预加载**，在需要时用 Read 工具按需读取：

| 文件 | 用途 | 何时读取 |
|------|------|---------|
| `reference/action-catalog.md` | 全部 106 个 Action 的 ID、参数、分类 | 用户问"有哪些 action"、需要查参数、按分类筛选时 |
| `reference/http-api.md` | HTTP 端点格式（请求/响应 JSON 结构） | 需要确认 API 调用格式时 |

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

### 配置文件

**每次执行前必须先读取** `.claude/config/blackboxpro-env.json`。

若关键字段为空，暂停流程，用 AskUserQuestion 逐项询问后回填配置文件。

**方案 A 必填项**：`client.launchCommand`
**方案 B 额外必填**：`server.directory` + `server.jar`

| 配置项 | 说明 | 示例 |
|--------|------|------|
| `client.launchMethod` | `runClient`（Gradle）或 `launcher`（外部启动器） | `runClient` |
| `client.launchCommand` | 启动客户端的完整命令 | `./gradlew :mod:1.21.11:fabric:runClient --no-daemon` |
| `client.launchCwd` | 工作目录（相对仓库根，空=仓库根） | `mod/1.12.2` |
| `client.javaHome` | JAVA_HOME（空=系统默认） | `/Library/Java/.../jdk-21` |
| `client.playerName` | 游戏内玩家名 | `Player` |
| `server.directory` | 服务端安装目录 | `/Users/xxx/server/paper-1.21.11` |
| `server.jar` | 服务端 JAR | `paper-1.21.11-97.jar` |
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

## 固定产物路径

| 产物 | 路径 |
|------|------|
| 1.21.11 Fabric Mod | `mod/1.21.11/fabric/build/libs/BlackBoxPro-fabric-1.21.11-*.jar` |
| 1.21.11 NeoForge Mod | `mod/1.21.11/neoforge/build/libs/BlackBoxPro-neoforge-1.21.11-*.jar` |
| 1.12.2 Forge Mod | `mod/1.12.2/forge/build/libs/BlackBoxPro-forge-1.12.2-*.jar` |
| Plugin | `plugin/build/libs/BlackBoxPro-Plugin-*.jar` |

---

## 方案 A：客户端自测流程

### A1. 构建 Mod

根据版本执行：
```bash
# 1.21.11
./gradlew mod_buildAll --no-daemon

# 1.12.2
./gradlew forge1122_build --no-daemon
```

构建失败则停止流程，向用户报告错误。

### A2. 后台启动客户端

使用 Bash 的 `run_in_background` 启动客户端。命令从配置文件读取：

```
launchCommand = config.versions[version].client.launchCommand
launchCwd = config.versions[version].client.launchCwd  （空则用仓库根）
javaHome = config.versions[version].client.javaHome     （非空则设 JAVA_HOME）
```

组装并执行：
```bash
cd <launchCwd> && JAVA_HOME="<javaHome>" <launchCommand>
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

### A4. 进入世界

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
./gradlew buildAll --no-daemon
# 或分步：
# ./gradlew mod_buildAll --no-daemon
# ./gradlew plugin_build --no-daemon
```

### B2. 部署 Plugin

从配置文件读取 `server.pluginDir`（或 `server.directory + "/plugins"`）：

```bash
cp plugin/build/libs/BlackBoxPro-Plugin-*.jar <server.directory>/plugins/
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

### B6. 连接服务器

```bash
curl -s --max-time 8 -X POST http://localhost:38081/execute \
  -H "Content-Type: application/json" \
  -d '{"id":"c1","action":"connect_to_server","params":{"ip":"127.0.0.1","port":25565}}'
```

轮询确认连接：每 3 秒查服务端日志是否包含 `joined the game`，最多 20 次（60 秒）。

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
