---
description: "控制 Minecraft Fabric 1.21.11 客户端。在 Terminal 2 中启动、停止客户端，或查看客户端日志。"
argument-hint: "<start|stop|status|log> [args...]"
allowed-tools: [Terminal, Bash, Read]
---

# Minecraft Client Controller (Terminal 2)

用户执行了: `/client $ARGUMENTS`

## 环境配置

- 客户端目录: `I:\PCL\.minecraft\versions\1.21.11-Fabric 0.18.4`
- 启动脚本: `I:\PCL\.minecraft\versions\1.21.11-Fabric 0.18.4\launch.bat`
- 启动命令: `cmd //c "I:\\PCL\\.minecraft\\versions\\1.21.11-Fabric 0.18.4\\launch.bat"`
- 客户端日志: `I:\PCL\.minecraft\versions\1.21.11-Fabric 0.18.4\logs\latest.log`
- Mod 目录: `I:\PCL\.minecraft\versions\1.21.11-Fabric 0.18.4\mods`
- 截图目录: `I:\PCL\.minecraft\versions\1.21.11-Fabric 0.18.4\screenshots\blackboxpro`
- 终端: Terminal 2（通过 `Terminal list` 获取，选择列表中的第二个终端）
- 客户端启动后会通过 `--quickPlayMultiplayer localhost:25565` 自动连接本地服务端

## 操作流程

### 通用步骤：获取终端 ID

每次操作前，先用 `Terminal list` 获取所有终端，选择名称包含 "Terminal 2" 或列表中的第二个终端作为客户端终端。

### start - 启动客户端

1. 用 `Terminal read` 读取最后 3 行，确认终端空闲（有 `$` 提示符）
2. 如果终端不空闲（客户端可能已在运行），提示用户先 stop
3. 发送启动命令:
   ```
   cmd //c "I:\\PCL\\.minecraft\\versions\\1.21.11-Fabric 0.18.4\\launch.bat"
   ```
4. 每隔几秒用 `Terminal read` 读取最后 10 行，检查是否出现以下关键字之一:
   - `BlackBoxPro network channels registered` - Mod 加载完成
   - `Created:` 多次出现 - 资源加载完成
   - `libpng warning` - 正在加载资源（仍在启动中）
5. 同时检查服务端终端（Terminal 1）是否出现 `joined the game` 表示客户端已连接
6. 报告启动状态

### stop - 停止客户端

1. 客户端没有优雅关闭的控制台命令，需要通过 Ctrl+C 终止:
   - 用 `Terminal write` 发送 `\x03`（Ctrl+C）
2. 等待几秒后用 `Terminal read` 确认进程已退出（出现 `$` 提示符）
3. 如果 Ctrl+C 无效，提示用户手动关闭客户端窗口

### status - 查看状态

1. 用 `Terminal read` 读取最后 10 行
2. 判断客户端是否在运行:
   - 有 `$` 提示符 → 已停止
   - 有 `libpng warning` 或游戏日志输出 → 运行中
3. 可选: 读取客户端日志文件获取更详细的状态

### log - 查看日志

参数格式: `/client log [n]`，n 默认 30

两种日志来源:
1. 终端输出: 用 `Terminal read` 读取最后 n 行
2. 日志文件: 用 `Read` 工具读取 `I:\PCL\.minecraft\versions\1.21.11-Fabric 0.18.4\logs\latest.log` 的最后 n 行

优先使用终端输出，如果终端输出不够详细则补充日志文件内容。

## 构建与部署

Fabric Mod 构建:
```bash
cd E:/Desktop/IDEA/BlackBoxPro && ./gradlew :fabric-1.21.11:build
```

部署 Mod:
```bash
cp "E:/Desktop/IDEA/BlackBoxPro/fabric-1.21.11/build/libs/blackboxpro-fabric-*.jar" "I:/PCL/.minecraft/versions/1.21.11-Fabric 0.18.4/mods/"
```

## 测试

通过服务端控制台执行集成测试:
```
blackbox test Player
```

- 22 个测试用例，每个用例 3 阶段截图（before / during / after）
- 共生成 68 张截图（1 开始 + 22×3 + 1 结束）
- 截图路径: `I:\PCL\.minecraft\versions\1.21.11-Fabric 0.18.4\screenshots\blackboxpro\Player\integration_<timestamp>\`
- 截图命名: `XXX_<id>_1_before.png` / `XXX_<id>_2_during.png` / `XXX_<id>_3_after.png`

## 注意事项

- 客户端是 GUI 程序，终端输出主要是 log4j 日志和 libpng 警告
- 客户端启动较慢（需要加载资源），耐心等待
- launch.bat 中已配置 `--quickPlayMultiplayer localhost:25565`，启动后会自动连接本地服务端
- 离线模式，玩家名为 `Player`
- Ctrl+C 可能无法完全终止客户端（Java GUI 进程），必要时需要 `taskkill`
- 修改 Mod 后需要重启客户端
- 修改 Plugin 后需要重启服务端（TabooLib 不支持 PlugMan 热重载）
