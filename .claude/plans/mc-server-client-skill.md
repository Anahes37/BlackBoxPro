# BlackBoxPro 服务端/客户端控制技能

## 目标

创建 slash command，分别控制 Minecraft 服务端和客户端的启动/停止/命令执行，绑定到 Terminal 1 和 Terminal 2。支持 1.21.11 和 1.12.2 两个版本。

## 文件结构

```
.claude/
└── commands/
    ├── server.md          # /server - 控制 Paper 1.21.11 服务端
    ├── client.md          # /client - 控制 Fabric 1.21.11 客户端
    ├── server-1.12.2.md   # /server-1.12.2 - 控制 Paper 1.12.2 服务端
    └── client-1.12.2.md   # /client-1.12.2 - 控制 Forge 1.12.2 客户端
```

## 1.21.11 版本

### /server 命令

- `/server start` - 在 Terminal 1 启动 Paper 1.21.11 服务端
- `/server stop` - 发送 stop 命令关闭服务端
- `/server restart` - stop 后重新启动
- `/server cmd <command>` - 在服务端控制台执行任意命令
- `/server status` - 读取最近日志判断服务端状态
- `/server log [n]` - 读取最近 n 行日志

硬编码参数：
- Java: `E:\AdoptOpenJDK\zulu21.36.17\bin\java.exe`
- 服务端目录: `E:\paper-1.21.11`
- JAR: `paper-1.21.11-97.jar`
- JVM 参数: `-Xms4G -Xmx4G -XX:+UseG1GC -XX:+OptimizeStringConcat -XX:MaxGCPauseMillis=10 -XX:+UseStringDeduplication`
- Terminal ID: 通过 `Terminal list` 找到 Terminal 1

### /client 命令

- `/client start` - 在 Terminal 2 启动 Fabric 1.21.11 客户端
- `/client stop` - 关闭客户端进程
- `/client status` - 读取终端输出判断客户端状态
- `/client log [n]` - 读取最近 n 行日志

硬编码参数：
- 客户端目录: `I:\PCL\.minecraft\versions\1.21.11-Fabric 0.18.4`
- 启动方式: `cmd //c "I:\\PCL\\.minecraft\\versions\\1.21.11-Fabric 0.18.4\\launch.bat"`
- Terminal ID: 通过 `Terminal list` 找到 Terminal 2

## 1.12.2 版本

### /server-1.12.2 命令

- `/server-1.12.2 start` - 在 Terminal 1 启动 Paper 1.12.2 服务端
- `/server-1.12.2 stop` - 通过 RCON 发送 stop 命令
- `/server-1.12.2 restart` - stop 后重新启动
- `/server-1.12.2 cmd <command>` - 通过 RCON 执行命令
- `/server-1.12.2 status` - 检查端口 + 读取日志
- `/server-1.12.2 log [n]` - 读取最近 n 行日志

硬编码参数：
- Java: `E:\AdoptOpenJDK\zulu8.0.422\bin\java.exe`
- 服务端目录: `E:\paper-1.12.2`
- JAR: `paper.jar`
- JVM 参数: `-Xms2G -Xmx4G -XX:+UseG1GC`
- RCON: 端口 25575，密码 `123456`
- Terminal ID: 通过 `Terminal list` 找到 Terminal 1

### /client-1.12.2 命令

- `/client-1.12.2 start` - 在 Terminal 2 启动 Forge 1.12.2 客户端
- `/client-1.12.2 stop` - Ctrl+C + Y 关闭客户端
- `/client-1.12.2 status` - 读取终端输出判断状态
- `/client-1.12.2 log [n]` - 读取最近 n 行日志

硬编码参数：
- 客户端目录: `I:\PCL\.minecraft\versions\1.12.2-Forge_14.23.5.2860`
- 启动方式: `cmd //c "I:\\PCL\\.minecraft\\versions\\1.12.2-Forge_14.23.5.2860\\start-client.bat"`
- 玩家名: `BlackBoxTester`
- Terminal ID: 通过 `Terminal list` 找到 Terminal 2

## 1.12.2 特殊注意事项

- Paper 1.12.2 终端有 readline 模式，直接 `Terminal write` 发送命令可能不被执行，优先使用 RCON
- TabooLib 插件不支持 PlugMan 热重载，修改插件后必须重启服务端
- Forgelin 捆绑的 Kotlin stdlib 版本较旧，避免使用 `maxOrNull`/`max` 等 Kotlin 1.4+ API
- stop 客户端时需要处理 bat 的 `终止批处理操作吗(Y/N)?` 提示

## 测试相关

### 1.12.2 集成测试

通过 RCON 执行: `blackbox test BlackBoxTester`

- 22 个测试用例，每个用例 3 阶段截图（before / during / after）
- 共生成 68 张截图（1 开始 + 22×3 + 1 结束）
- 截图路径: `I:\PCL\.minecraft\versions\1.12.2-Forge_14.23.5.2860\screenshots\blackboxpro\BlackBoxTester\integration_<timestamp>\`

构建部署流程:
```bash
# 构建 Forge Mod
cd E:/Desktop/IDEA/BlackBoxPro/forge-1.12.2 && ./gradlew build
# 构建 Plugin
cd E:/Desktop/IDEA/BlackBoxPro/plugin && ./gradlew jar
# 部署 Mod
cp E:/Desktop/IDEA/BlackBoxPro/forge-1.12.2/build/libs/BlackBoxPro-forge-1.12.2-*.jar "I:/PCL/.minecraft/versions/1.12.2-Forge_14.23.5.2860/mods/"
# 部署 Plugin
cp E:/Desktop/IDEA/BlackBoxPro/plugin/build/libs/BlackBoxPro-Plugin-*.jar "E:/paper-1.12.2/plugins/"
```

## 实现要点

1. 每个命令通过 `$ARGUMENTS` 获取子命令参数
2. 使用 `Terminal` 工具的 `list` action 动态获取终端 ID（不硬编码）
3. 使用 `Terminal write` 发送命令，`Terminal read` 读取输出
4. `allowed-tools` 包含 `Terminal` 和 `Bash`
5. 启动前检查终端是否空闲（通过读取最后几行判断是否有 `$` 提示符）
6. 服务端启动后轮询等待 `Done` 关键字确认启动完成
7. 1.12.2 服务端命令优先通过 RCON 执行
