---
name: blackbox-runner
description: BlackBoxPro 测试环境管理。按测试类型启动/停止服务端和客户端、构建部署、执行 plugin+mod 联合测试或纯mod 测试。用于 1.21.11 / 1.12.2 的 plugin+mod 联测，以及 NeoForge 1.21.1 的纯mod runClient + HTTP 测试。
---

BlackBoxPro 测试环境统一管理技能。先判断测试类型，再决定是否需要服务端、plugin、客户端部署和测试入口。

用户可能的调用方式：
- "启动 1.12.2 测试环境" → `plugin+mod` 联合测试，启动服务端 + 客户端
- "跑一下 1.21.11 的 blackbox test" → `plugin+mod` 联合测试，构建部署 + 启动 + 执行 `blackbox test`
- "用 runClient 起 neoforge 1.21.1 纯mod 测试" → `纯mod` 测试，只启动本地客户端
- "跑一下 create_world / join_world / leave_world" → `纯mod` 测试，启动 `:neoforge-1.21.1:runClient` + 调用本地 HTTP API

## 测试类型

### 1. plugin+mod 联合测试

用于验证：

- Bukkit/Paper `plugin`
- Plugin Message 通道
- `blackbox test <player>`
- 服务端命令或 `plugin` API 到客户端执行链路

默认版本：

- `1.21.11`
- `1.12.2`

### 2. 纯mod 测试

用于验证：

- 本地 `runClient`
- 纯 mod / 客户端本地 action
- 单机世界管理：`create_world` / `join_world` / `leave_world`
- 本地 HTTP API：`127.0.0.1:25580`

当前版本：

- `neoforge-1.21.1`

### 类型判断规则

- 用户提到 `plugin`、`联调`、`服务端`、`blackbox test`、`Plugin Message` 时，优先按 `plugin+mod` 联合测试处理
- 用户提到 `纯mod`、`纯客户端`、`runClient`、`HTTP`、`create_world`、`join_world`、`leave_world` 时，优先按 `纯mod` 测试处理
- 如果技能文档中的固定路径不存在，必须先探测本机实际环境，再执行后续动作

## 版本 / 类型环境速查

### 1.21.11（plugin+mod 联合测试）

| 项目 | 值 |
|------|-----|
| Java | `E:\AdoptOpenJDK\zulu21.36.17\bin\java.exe` |
| 服务端目录 | `E:\paper-1.21.11` |
| 服务端 JAR | `paper-1.21.11-97.jar` |
| 服务端 JVM | `-Xms4G -Xmx4G -XX:+UseG1GC -XX:+OptimizeStringConcat -XX:MaxGCPauseMillis=10 -XX:+UseStringDeduplication` |
| 客户端目录 | `I:\PCL\.minecraft\versions\1.21.11-Fabric 0.18.4` |
| 客户端启动 | `cmd //c "I:\\PCL\\.minecraft\\versions\\1.21.11-Fabric 0.18.4\\launch.bat"` |
| 玩家名 | `Player` |
| Mod 构建 | `cd E:/Desktop/IDEA/BlackBoxPro && ./gradlew :fabric-1.21.11:build` 或 `:neoforge-1.21.11:build` |
| Mod 产物 | `E:\Desktop\IDEA\BlackBoxPro\fabric-1.21.11\build\libs\blackboxpro-fabric-*.jar` |
| Plugin 产物 | `E:\Desktop\IDEA\BlackBoxPro\plugin\build\libs\BlackBoxPro-Plugin-*.jar` |
| 服务端 stop | Terminal write `stop\n` |
| 客户端 stop | Terminal write `\x03` (Ctrl+C) |

### 1.12.2（plugin+mod 联合测试）

| 项目 | 值 |
|------|-----|
| Java | `E:\AdoptOpenJDK\zulu8.0.422\bin\java.exe` |
| 服务端目录 | `E:\paper-1.12.2` |
| 服务端 JAR | `paper.jar` |
| 服务端 JVM | `-Xms2G -Xmx4G -XX:+UseG1GC` |
| RCON | 端口 `25575`，密码 `123456` |
| 客户端目录 | `I:\PCL\.minecraft\versions\1.12.2-Forge_14.23.5.2860` |
| 客户端启动 | `cmd //c "I:\\PCL\\.minecraft\\versions\\1.12.2-Forge_14.23.5.2860\\start-client.bat"` |
| 玩家名 | `BlackBoxTester` |
| Mod 构建 | `cd E:/Desktop/IDEA/BlackBoxPro/forge-1.12.2 && ./gradlew build` |
| Mod 产物 | `E:\Desktop\IDEA\BlackBoxPro\forge-1.12.2\build\libs\BlackBoxPro-forge-1.12.2-*.jar` |
| Plugin 产物 | `E:\Desktop\IDEA\BlackBoxPro\plugin\build\libs\BlackBoxPro-Plugin-*.jar` |
| 服务端 stop | 通过 RCON 发送 `stop`（终端 readline 模式不可靠） |
| 客户端 stop | Terminal write `\x03` → 等待 `终止批处理操作吗(Y/N)?` → `Y\n` |

### NeoForge 1.21.1（纯mod 测试）

| 项目 | 值 |
|------|-----|
| Java | `C:\Program Files\Java\jdk-21\bin\java.exe` |
| 仓库目录 | `F:\minecraft\mod\BlackBoxPro` |
| 客户端模块 | `neoforge-1.21.1` |
| 启动方式 | `.\gradlew :neoforge-1.21.1:runClient --console=plain` |
| 推荐方式 | 新开可见 PowerShell 终端运行 `runClient` |
| 运行目录 | `F:\minecraft\mod\BlackBoxPro\neoforge-1.21.1\run` |
| HTTP 接口 | `http://127.0.0.1:25580/api/command` |
| 测试入口 | 本地 HTTP POST |
| 主要 Action | `create_world`、`join_world`、`leave_world`、`query_player_state`、`query_world_state` |
| 停止方式 | 结束 `runClient` 对应终端 / `Ctrl+C` |

## 操作流程

### plugin+mod 联合测试：启动服务端

Terminal 1 执行：

1.21.11:
```bash
cd /e/paper-1.21.11 && "/e/AdoptOpenJDK/zulu21.36.17/bin/java.exe" -Xms4G -Xmx4G -XX:+UseG1GC -XX:+OptimizeStringConcat -XX:MaxGCPauseMillis=10 -XX:+UseStringDeduplication -jar paper-1.21.11-97.jar nogui
```

1.12.2:
```bash
cd E:/paper-1.12.2 && "E:/AdoptOpenJDK/zulu8.0.422/bin/java.exe" -Xms2G -Xmx4G -XX:+UseG1GC -jar paper.jar nogui
```

等待 `Done` 关键字确认启动完成。

### plugin+mod 联合测试：启动客户端

Terminal 2 执行对应版本的启动命令。等待服务端出现 `joined the game`。

### plugin+mod 联合测试：停止服务端

- `1.21.11`：Terminal write `stop\n`
- `1.12.2`：通过 RCON（见下方 RCON 工具函数）

### plugin+mod 联合测试：停止客户端

- `1.21.11`：Terminal write `\x03`
- `1.12.2`：Terminal write `\x03` → 等待提示 → `Y\n`

### plugin+mod 联合测试：构建部署

```bash
# Plugin（两个版本共用）
cd E:/Desktop/IDEA/BlackBoxPro/plugin && ./gradlew jar

# 1.21.11 Fabric Mod
cd E:/Desktop/IDEA/BlackBoxPro && ./gradlew :fabric-1.21.11:build

# 1.12.2 Forge Mod
cd E:/Desktop/IDEA/BlackBoxPro/forge-1.12.2 && ./gradlew build
```

部署：
```bash
# 1.21.11
cp "E:/Desktop/IDEA/BlackBoxPro/fabric-1.21.11/build/libs/blackboxpro-fabric-*.jar" "I:/PCL/.minecraft/versions/1.21.11-Fabric 0.18.4/mods/"
cp "E:/Desktop/IDEA/BlackBoxPro/plugin/build/libs/BlackBoxPro-Plugin-*.jar" "E:/paper-1.21.11/plugins/"

# 1.12.2
cp "E:/Desktop/IDEA/BlackBoxPro/forge-1.12.2/build/libs/BlackBoxPro-forge-1.12.2-*.jar" "I:/PCL/.minecraft/versions/1.12.2-Forge_14.23.5.2860/mods/"
cp "E:/Desktop/IDEA/BlackBoxPro/plugin/build/libs/BlackBoxPro-Plugin-*.jar" "E:/paper-1.12.2/plugins/"
```

### plugin+mod 联合测试：执行测试

- `1.21.11`：服务端控制台 `blackbox test Player`
- `1.12.2`：通过 RCON `blackbox test BlackBoxTester`

测试结果：22 个用例，每个 3 阶段截图（before / during / after），共 68 张。

截图路径：
- `1.21.11`：`I:\PCL\.minecraft\versions\1.21.11-Fabric 0.18.4\screenshots\blackboxpro\Player\integration_<ts>\`
- `1.12.2`：`I:\PCL\.minecraft\versions\1.12.2-Forge_14.23.5.2860\screenshots\blackboxpro\BlackBoxTester\integration_<ts>\`

### 纯mod 测试：启动客户端

优先使用可见终端启动，便于观察是否黑屏、卡在菜单或停在世界内。

```powershell
Set-Location 'F:\minecraft\mod\BlackBoxPro'
$env:JAVA_HOME='C:\Program Files\Java\jdk-21'
$env:Path='C:\Program Files\Java\jdk-21\bin;' + $env:Path
.\gradlew :neoforge-1.21.1:runClient --console=plain
```

### 纯mod 测试：执行测试

- 不启动服务端
- 不部署 `plugin`
- 不执行 `blackbox test`
- 通过 HTTP 直接调用 `127.0.0.1:25580/api/command`

常用动作：

- `query_player_state`：判断当前是在主菜单还是世界内
- `create_world`：创建并进入单机世界
- `leave_world`：退出到主菜单
- `join_world`：重新进入已有单机世界
- `query_world_state`：读取当前单机世界状态

世界管理 Action 的完成语义：

- `create_world` 只有在真正进入新世界后才能算成功
- `join_world` 只有在真正进入已有世界后才能算成功；不存在或缺少 `level.dat` / `level.dat_old` 的世界应直接失败
- `leave_world` 只有在真正回到主菜单后才能算成功
- 纯mod 回归不要依赖固定 `sleep` 等待世界切换；若必须等待，说明 Action 尚未原子化

典型顺序：

1. 等待 `25580` 监听
2. `query_player_state` 预期 `failure`，确认在主菜单
3. `create_world`
4. 运行世界内 action / query
5. `leave_world`
6. 再次 `query_player_state`，预期 `failure`，确认已经回主菜单
7. `join_world`
8. 再次 `query_player_state`，预期 `success`，确认已经重新进入世界
9. 验证不存在世界、缺失参数、未知 action 等边界项

当前纯mod 脚本归属：

- `test_world.sh`
- `test_socket.py`

### 纯mod 测试：停止客户端

- 结束 `runClient` 终端
- 或对终端发送 `Ctrl+C`
- 停止后确认 `25580` 不再监听

## RCON 工具函数（1.12.2 专用）

Paper 1.12.2 终端有 readline 模式，`Terminal write` 发送命令不可靠。所有服务端命令通过 RCON 执行：

```bash
python -c "
import socket, struct
def rcon(cmd):
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.settimeout(10)
    s.connect(('127.0.0.1', 25575))
    pw = b'123456\x00\x00'
    p = struct.pack('<ii', 0, 3) + pw
    s.send(struct.pack('<i', len(p)) + p)
    s.recv(4096)
    p = struct.pack('<ii', 1, 2) + cmd.encode() + b'\x00\x00'
    s.send(struct.pack('<i', len(p)) + p)
    r = s.recv(4096)
    l = struct.unpack('<i', r[:4])[0]
    body = r[12:4+l-2].decode('utf-8', errors='replace')
    s.close()
    return body
print(rcon('这里替换为实际命令'))
"
```

## 重要注意事项

- 先判断测试类型，再执行环境启动
- `plugin+mod` 联合测试不要误用本地 HTTP 结果替代服务端联调结果
- `纯mod` 测试不要启动服务端，也不要执行 `blackbox test`
- `1.21.11`、`1.12.2` 当前归类为 `plugin+mod` 联测
- `neoforge-1.21.1` 当前归类为 `纯mod` 测试线
- 修改 Plugin 后必须重启服务端（TabooLib 不支持 PlugMan 热重载）
- 修改 Mod 后必须重启客户端
- `1.12.2` 使用 JDK 8，Forge 不兼容更高版本
- `1.12.2` Forgelin 的 Kotlin stdlib 版本旧，避免 `maxOrNull`/`max` 等 Kotlin 1.4+ API，用纯循环替代
- 如果技能文档中的固定路径不可用，先探测本机实际路径，不要硬编码依赖 `I:` 盘
- 检查服务端是否运行：`netstat -ano | findstr 25565`
- 两个联合测试版本不能同时运行（共用 `25565` 端口）
