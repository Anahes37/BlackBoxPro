# BlackBoxPro 服务端/客户端控制技能

## 目标

创建两个 slash command，分别控制 Minecraft 服务端和客户端的启动/停止/命令执行，绑定到 Terminal 1 和 Terminal 2。

## 文件结构

```
.claude/
└── commands/
    ├── server.md      # /server - 控制 Terminal 1 中的 Paper 服务端
    └── client.md      # /client - 控制 Terminal 2 中的 Fabric 客户端
```

## /server 命令

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

## /client 命令

- `/client start` - 在 Terminal 2 启动 Fabric 1.21.11 客户端
- `/client stop` - 关闭客户端进程
- `/client status` - 读取终端输出判断客户端状态
- `/client log [n]` - 读取最近 n 行日志

硬编码参数：
- 客户端目录: `I:\PCL\.minecraft\versions\1.21.11-Fabric 0.18.4`
- 启动方式: `cmd //c "I:\\PCL\\.minecraft\\versions\\1.21.11-Fabric 0.18.4\\launch.bat"`
- Terminal ID: 通过 `Terminal list` 找到 Terminal 2

## 实现要点

1. 每个命令通过 `$ARGUMENTS` 获取子命令参数
2. 使用 `Terminal` 工具的 `list` action 动态获取终端 ID（不硬编码）
3. 使用 `Terminal write` 发送命令，`Terminal read` 读取输出
4. `allowed-tools` 包含 `Terminal` 和 `Bash`
5. 启动前检查终端是否空闲（通过读取最后几行判断是否有 `$` 提示符）
6. 服务端启动后轮询等待 `Done` 关键字确认启动完成
