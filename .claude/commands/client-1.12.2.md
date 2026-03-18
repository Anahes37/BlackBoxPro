---
description: "控制 Minecraft Forge 1.12.2 客户端。在 Terminal 2 中启动、停止客户端，或查看客户端日志。"
argument-hint: "<start|stop|status|log> [args...]"
allowed-tools: [Terminal, Bash, Read]
---

# Minecraft 1.12.2 Forge Client Controller (Terminal 2)

用户执行了: `/client-1.12.2 $ARGUMENTS`

## 环境配置

- Java 路径: `E:\AdoptOpenJDK\zulu8.0.422\bin\java.exe`
- 客户端目录: `I:\PCL\.minecraft\versions\1.12.2-Forge_14.23.5.2860`
- 启动脚本: `I:\PCL\.minecraft\versions\1.12.2-Forge_14.23.5.2860\start-client.bat`
- 启动命令: `cmd //c "I:\\PCL\\.minecraft\\versions\\1.12.2-Forge_14.23.5.2860\\start-client.bat"`
- 客户端日志: `I:\PCL\.minecraft\versions\1.12.2-Forge_14.23.5.2860\logs\latest.log`
- Mod 目录: `I:\PCL\.minecraft\versions\1.12.2-Forge_14.23.5.2860\mods`
- 截图目录: `I:\PCL\.minecraft\versions\1.12.2-Forge_14.23.5.2860\screenshots\blackboxpro`
- 终端: Terminal 2（通过 `Terminal list` 获取，选择列表中的第二个终端）
- 客户端启动后通过 `--server localhost --port 25565` 自动连接本地服务端
- 离线模式，玩家名为 `BlackBoxTester`

## 操作流程

### 通用步骤：获取终端 ID

每次操作前，先用 `Terminal list` 获取所有终端，选择名称包含 "Terminal 2" 或列表中的第二个终端作为客户端终端。

### start - 启动客户端

1. 用 `Terminal read` 读取最后 3 行，确认终端空闲（有 `$` 提示符）
2. 如果终端不空闲（客户端可能已在运行），提示用户先 stop
3. 发送启动命令:
   ```
   cmd //c "I:\\PCL\\.minecraft\\versions\\1.12.2-Forge_14.23.5.2860\\start-client.bat"
   ```
4. 每隔几秒用 `Terminal read` 读取最后 10 行，检查是否出现:
   - `BlackBoxPro network channels registered` - Mod 加载完成
   - `BlackBoxProForge` - Mod 初始化完成
5. 同时检查服务端终端是否出现 `joined the game` 表示客户端已连接
6. 报告启动状态

### stop - 停止客户端

1. 用 `Terminal write` 发送 `\x03`（Ctrl+C）
2. 等待出现 `终止批处理操作吗(Y/N)?` 提示
3. 发送 `Y\n` 确认
4. 等待几秒后用 `Terminal read` 确认进程已退出（出现 `$` 提示符）

### status - 查看状态

1. 用 `Terminal read` 读取最后 10 行
2. 判断客户端是否在运行:
   - 有 `$` 提示符 → 已停止
   - 有游戏日志输出 → 运行中

### log - 查看日志

参数格式: `/client-1.12.2 log [n]`，n 默认 30

两种日志来源:
1. 终端输出: 用 `Terminal read` 读取最后 n 行
2. 日志文件: 用 `Read` 工具读取 latest.log 的最后 n 行

## 构建与部署

Forge 1.12.2 Mod 构建:
```bash
cd E:/Desktop/IDEA/BlackBoxPro/forge-1.12.2 && ./gradlew build
```

部署 Mod:
```bash
cp "E:/Desktop/IDEA/BlackBoxPro/forge-1.12.2/build/libs/BlackBoxPro-forge-1.12.2-1.2.4.jar" "I:/PCL/.minecraft/versions/1.12.2-Forge_14.23.5.2860/mods/BlackBoxPro-forge-1.12.2-1.2.4.jar"
```

部署 Plugin:
```bash
cp "E:/Desktop/IDEA/BlackBoxPro/plugin/build/libs/BlackBoxPro-Plugin-1.2.4.jar" "E:/paper-1.12.2/plugins/BlackBoxPro-Plugin-1.2.4.jar"
```

## 测试

通过 RCON 执行集成测试:
```bash
# blackbox test 命令（22 个用例，每个用例 3 阶段截图）
rcon('blackbox test BlackBoxTester')
```

测试截图存储在: `screenshots/blackboxpro/BlackBoxTester/integration_<timestamp>/`
- `001_00_test_start.png` - 测试开始
- `XXX_<id>_1_before.png` - 动作执行前
- `XXX_<id>_2_during.png` - 动作执行中
- `XXX_<id>_3_after.png` - 动作完成后（或 `3_FAILED.png`）
- `068_99_test_end.png` - 测试结束

## 注意事项

- 客户端使用 JDK 8（zulu8.0.422），Forge 1.12.2 不兼容更高版本
- Forgelin 捆绑的 Kotlin stdlib 版本较旧，代码中避免使用 Kotlin 1.4+ 的集合 API（如 `maxOrNull`、`max`），用纯循环替代
- 客户端启动较慢（约 30-40 秒），耐心等待
- stop 时需要处理 bat 的 `终止批处理操作吗(Y/N)?` 提示
- 修改 Mod 后需要重启客户端，修改 Plugin 后需要重启服务端（不能用 PlugMan 热重载）
