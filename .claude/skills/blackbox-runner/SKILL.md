---
name: blackbox-runner
description: BlackBoxPro 测试环境管理。启动/停止服务端和客户端，构建部署，执行集成测试。支持 1.21.11 和 1.12.2 两个版本。
---

BlackBoxPro 测试环境统一管理技能。根据用户指定的版本，控制服务端/客户端的启动停止、构建部署、执行测试。

用户可能的调用方式：
- "启动 1.12.2 测试环境" → 启动服务端 + 客户端
- "跑一下 1.12.2 的测试" → 构建部署 + 启动 + 执行 blackbox test
- "重启客户端" → 根据上下文判断版本，stop + start

## 版本环境速查

### 1.21.11

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

### 1.12.2

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

## 操作流程

### 启动服务端

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

### 启动客户端

Terminal 2 执行对应版本的启动命令。等待服务端出现 `joined the game`。

### 停止服务端

- 1.21.11: Terminal write `stop\n`
- 1.12.2: 通过 RCON（见下方 RCON 工具函数）

### 停止客户端

- 1.21.11: Terminal write `\x03`
- 1.12.2: Terminal write `\x03` → 等待提示 → `Y\n`

### 构建部署

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

### 执行测试

- 1.21.11: 服务端控制台 `blackbox test Player`
- 1.12.2: 通过 RCON `blackbox test BlackBoxTester`

测试结果：22 个用例，每个 3 阶段截图（before / during / after），共 68 张。

截图路径：
- 1.21.11: `I:\PCL\.minecraft\versions\1.21.11-Fabric 0.18.4\screenshots\blackboxpro\Player\integration_<ts>\`
- 1.12.2: `I:\PCL\.minecraft\versions\1.12.2-Forge_14.23.5.2860\screenshots\blackboxpro\BlackBoxTester\integration_<ts>\`

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

- 修改 Plugin 后必须重启服务端（TabooLib 不支持 PlugMan 热重载）
- 修改 Mod 后必须重启客户端
- 1.12.2 使用 JDK 8，Forge 不兼容更高版本
- 1.12.2 Forgelin 的 Kotlin stdlib 版本旧，避免 `maxOrNull`/`max` 等 Kotlin 1.4+ API，用纯循环替代
- 检查服务端是否运行: `netstat -ano | grep "25565.*LISTEN"`
- 两个版本不能同时运行（共用 25565 端口）
