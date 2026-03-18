---
description: "控制 Minecraft Paper 1.21.11 服务端。在 Terminal 1 中启动、停止、重启服务端，或执行服务端控制台命令。"
argument-hint: "<start|stop|restart|cmd|status|log> [args...]"
allowed-tools: [Terminal, Bash, Read]
---

# Minecraft Server Controller (Terminal 1)

用户执行了: `/server $ARGUMENTS`

## 环境配置

- Java 路径: `E:\AdoptOpenJDK\zulu21.36.17\bin\java.exe`
- 服务端目录: `E:\paper-1.21.11`
- JAR 文件: `paper-1.21.11-97.jar`
- JVM 参数: `-Xms4G -Xmx4G -XX:+UseG1GC -XX:+OptimizeStringConcat -XX:MaxGCPauseMillis=10 -XX:+UseStringDeduplication`
- 终端: Terminal 1（通过 `Terminal list` 获取，选择列表中的第一个终端）

## 操作流程

### 通用步骤：获取终端 ID

每次操作前，先用 `Terminal list` 获取所有终端，选择名称包含 "Terminal 1" 或列表中的第一个终端作为服务端终端。

### start - 启动服务端

1. 用 `Terminal read` 读取最后 3 行，确认终端空闲（有 `$` 提示符且没有 java 进程输出）
2. 如果终端不空闲（服务端可能已在运行），提示用户先 stop
3. 发送启动命令:
   ```
   cd /e/paper-1.21.11 && "/e/AdoptOpenJDK/zulu21.36.17/bin/java.exe" -Xms4G -Xmx4G -XX:+UseG1GC -XX:+OptimizeStringConcat -XX:MaxGCPauseMillis=10 -XX:+UseStringDeduplication -jar paper-1.21.11-97.jar nogui
   ```
4. 每隔几秒用 `Terminal read` 读取最后 10 行，检查是否出现 `Done` 关键字
5. 出现 `Done` 后报告启动成功，包含耗时信息
6. 如果 60 秒内未出现 `Done`，报告可能启动失败并展示最近日志

### stop - 停止服务端

1. 用 `Terminal write` 发送 `stop\n`
2. 等待几秒后用 `Terminal read` 确认服务端已关闭（出现 `$` 提示符）

### restart - 重启服务端

1. 先执行 stop 流程
2. 确认停止后执行 start 流程

### cmd - 执行服务端命令

参数格式: `/server cmd <command>`

1. 将 `<command>` 部分通过 `Terminal write` 发送到终端（追加 `\n`）
2. 等待 2 秒后用 `Terminal read` 读取最后 20 行输出并展示给用户

### status - 查看状态

1. 用 `Terminal read` 读取最后 10 行
2. 判断服务端是否在运行（有 `>` 提示符表示运行中，有 `$` 表示已停止）
3. 报告状态

### log - 查看日志

参数格式: `/server log [n]`，n 默认 30

1. 用 `Terminal read` 读取最后 n 行并展示

## 构建与部署

Fabric Mod 构建:
```bash
cd E:/Desktop/IDEA/BlackBoxPro && ./gradlew :fabric-1.21.11:build
```

NeoForge Mod 构建:
```bash
cd E:/Desktop/IDEA/BlackBoxPro && ./gradlew :neoforge-1.21.11:build
```

Plugin 构建:
```bash
cd E:/Desktop/IDEA/BlackBoxPro/plugin && ./gradlew jar
```

部署 Fabric Mod:
```bash
cp "E:/Desktop/IDEA/BlackBoxPro/fabric-1.21.11/build/libs/blackboxpro-fabric-*.jar" "I:/PCL/.minecraft/versions/1.21.11-Fabric 0.18.4/mods/"
```

部署 Plugin:
```bash
cp "E:/Desktop/IDEA/BlackBoxPro/plugin/build/libs/BlackBoxPro-Plugin-*.jar" "E:/paper-1.21.11/plugins/"
```

## 测试

通过服务端控制台执行集成测试:
```
blackbox test Player
```

- 22 个测试用例，每个用例 3 阶段截图（before / during / after）
- 截图路径: `I:\PCL\.minecraft\versions\1.21.11-Fabric 0.18.4\screenshots\blackboxpro\Player\integration_<timestamp>\`

## 注意事项

- 启动命令中的路径使用 Git Bash 风格（`/e/` 而非 `E:\`）
- 发送命令时始终在末尾追加 `\n`
- 不要在终端中使用 `cd` 以外的 Windows 命令
- 修改 Plugin 后需要重启服务端（TabooLib 不支持 PlugMan 热重载）
- 修改 Mod 后需要重启客户端
