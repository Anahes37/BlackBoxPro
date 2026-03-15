# 重构项目名：BlackBox → BlackBoxPro

## 改动范围

需要区分两类 "blackbox"：

### 1. 网络协议层的 channel namespace（`blackbox:command` / `blackbox:response`）
这是 Minecraft 插件消息的 namespace，属于**协议标识符**。改名会导致与现有服务端插件不兼容。
→ **保持不变**，不改 `"blackbox"` namespace。

### 2. 项目/Mod 显示名和内部标识
以下全部从 `BlackBox` → `BlackBoxPro`：

| 类别 | 改动 |
|------|------|
| `settings.gradle.kts` | `rootProject.name = "BlackBox"` → `"BlackBoxPro"` |
| `build.gradle.kts` (root) | `group = "com.blackbox"` → `"com.blackboxpro"` |
| `fabric/build.gradle.kts` | `archivesName = "blackbox-fabric"` → `"blackboxpro-fabric"` |
| `neoforge/build.gradle.kts` | `archivesName = "blackbox-neoforge"` → `"blackboxpro-neoforge"` |
| `gradle.properties` | 无需改（不含项目名） |
| `fabric.mod.json` | `id`, `name`, `description` 中的 BlackBox → BlackBoxPro |
| `neoforge.mods.toml` | `modId`, `displayName`, `description` 中的 blackbox → blackboxpro |
| Fabric 入口类 | `BlackBoxFabric.kt` → `BlackBoxProFabric.kt`，类名同步改 |
| NeoForge 入口类 | `BlackBoxNeoForge.kt` → `BlackBoxProNeoForge.kt`，类名同步改 |
| 所有 Kotlin 包名 | `com.blackbox.fabric` → `com.blackboxpro.fabric`，`com.blackbox.neoforge` → `com.blackboxpro.neoforge` |
| 目录结构 | `com/blackbox/` → `com/blackboxpro/` |
| Logger 名称 | `"BlackBox-*"` / `"BlackBoxFabric"` / `"BlackBoxNeoForge"` → `"BlackBoxPro-*"` 等 |
| Config 文件名 | `blackbox-fabric.json` → `blackboxpro-fabric.json`，`blackbox-neoforge.json` → `blackboxpro-neoforge.json` |
| 文档 | `开发文档-1.0.0.md` 和 `需求文档-1.0.0.md` 中的 BlackBox/blackbox 引用 |

### 3. 不改的内容
- 网络 channel namespace `"blackbox:command"` / `"blackbox:response"` — 协议兼容性
- `NetworkConfig` 中的默认值 `"blackbox:command"` / `"blackbox:response"` — 同上

## 执行步骤

1. 移动目录：`com/blackbox/` → `com/blackboxpro/`（fabric 和 neoforge 两个模块）
2. 批量 sed 替换包名：`com.blackbox.fabric` → `com.blackboxpro.fabric`，`com.blackbox.neoforge` → `com.blackboxpro.neoforge`
3. 重命名入口类文件和类名
4. 修改构建脚本和元数据文件
5. 修改 Logger 名称和配置文件名
6. 更新文档
