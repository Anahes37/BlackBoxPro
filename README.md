# BlackBoxPro

Minecraft 自动化黑盒测试框架。服务端插件通过 Plugin Message Channel 向客户端 Mod 下发 JSON 指令，客户端模拟真实玩家行为并回传结果，用于服务端插件功能测试。

## 架构

```text
┌─────────────────────┐     blackbox:command      ┌──────────────────────┐
│   Bukkit Server     │ ────────────────────────▶ │  Fabric / NeoForge   │
│   (plugin 模块)     │                           │  / Forge 客户端 Mod  │
│                     │ ◀──────────────────────── │                      │
│                     │     blackbox:response     │                      │
└─────────────────────┘                           └──────────────────────┘
```

## 模块

| 模块 | 角色 | 框架 | JVM |
|------|------|------|-----|
| `mod/common` | 无 MC 依赖的共享协议层 | Kotlin + Gson | 8 |
| `mod:1.21.11:runtime` | 1.21.11 公共运行时核心（当前含共享 bridge/core 与部分共享 Action） | NeoForm + Kotlin | 21 |
| `mod:1.21.11:fabric` | Fabric wrapper + 平台实现 | Fabric 1.21.11 + fabric-language-kotlin | 21 |
| `mod:1.21.11:neoforge` | NeoForge wrapper + 平台实现 | NeoForge 21.11.x + KotlinForForge | 21 |
| `plugin` | 服务端插件 | Paper/Spigot + TabooLib 6.2 | 21 |
| `mod/1.12.2` | Forge 1.12.2 客户端 Mod（独立构建根，含 `runtime` / `forge`） | Forge 1.12.2 | 8 |

## 仓库结构

```text
BlackBoxPro/
├── mod/
│   ├── common/
│   ├── 1.21.11/
│   │   ├── runtime/
│   │   ├── fabric/
│   │   └── neoforge/
│   └── 1.12.2/
│       ├── runtime/
│       └── forge/
├── plugin/
├── build.gradle.kts        # 根聚合入口
└── settings.gradle.kts
```

说明：
- 根项目负责聚合构建。
- `mod` 是客户端聚合入口，负责 1.21.11 模块并通过包装任务调度 `mod/1.12.2` 独立构建。
- `mod/1.12.2` 是独立 Gradle 构建根，内部包含 `runtime` / `forge`，用于隔离 Kotlin 1.9.25 + RFG 工具链。
- `runtime` 现在开始承载 1.21.11 公共桥接核心（dispatcher/scheduler/config/response）以及首批共享 Action。
- `screenshot` 已提升为 runtime 共享编排能力，Fabric / NeoForge 仅保留平台截图实现。
- NeoForge 1.21.11 产物按 `common + runtime + neoforge-wrapper` 分层构建。
- Fabric 1.21.11 已接入 `runtime` 公共核心源码，但平台实现仍保留在 Fabric 模块内。
- `plugin` 与 `mod/1.12.2` 都通过 `mod/common` 共享协议层产物。


## 支持的行为

| 分类 | 示例 | 数量 |
|------|------|------|
| 移动与位置 | `player_move`, `player_look`, `confirm_teleportation` | 8 |
| 方块交互 | `dig_start`, `place_block`, `use_item` | 5 |
| 实体交互 | `attack_entity`, `interact_entity`, `swing_arm` | 4 |
| 容器 / GUI | `click_slot`, `close_container`, `set_carried_item` | 11 |
| 玩家状态 | `sneak_start`, `drop_item`, `swap_hands`, `jump` | 16 |
| 聊天命令 | `chat_message`, `chat_command` | 2 |
| 客户端设置 | `client_information`, `player_abilities`, `screenshot` | 4 |
| 进阶交互 | `edit_book`, `update_sign`, `select_trade` | 17 |
| 调试 | `keep_alive`, `pong`, `custom_payload` | 6 |
| 复合行为 | `pathfind_to`, `break_block`, `batch`, `craft_recipe` | 14 |
| 查询 | `query_held_item`, `query_inventory_slot`, `query_player_state` | 8 |

## 通讯协议

消息格式：JSON over Plugin Message Channel（VarInt length + UTF-8 bytes）。

指令：
```json
{
  "id": "uuid",
  "action": "screenshot",
  "params": { "testId": "shop_gui_test", "prefix": "after_warp" },
  "delay": 0
}
```

响应：
```json
{
  "id": "uuid",
  "status": "success",
  "message": "Screenshot saved: 001_after_warp.png",
  "data": { "filePath": "screenshots/blackboxpro/Steve/shop_gui_test/001_after_warp.png" }
}
```

## 构建

在仓库根目录执行：

```powershell
# 构建 1.21.11 mod（common + fabric + neoforge）
.\gradlew mod_buildAll

# 构建 plugin
.\gradlew plugin_build

# 构建 forge 1.12.2
.\gradlew forge1122_build

# 全量构建并收集产物到根 build\libs
.\gradlew buildAll
```

## 产物路径

- `mod/common/build/libs/blackboxpro-common-*.jar`
- `mod/1.21.11/fabric/build/libs/BlackBoxPro-fabric-1.21.11-*.jar`
- `mod/1.21.11/neoforge/build/libs/BlackBoxPro-neoforge-1.21.11-*.jar`
- `plugin/build/libs/BlackBoxPro-Plugin-*.jar`
- `mod/1.12.2/build/libs/BlackBoxPro-forge-1.12.2-*.jar`
- `build/libs/` 为根聚合收集目录

## 技术栈

- Kotlin
- Gradle Kotlin DSL
- Fabric API / NeoForge / Forge
- TabooLib 6.2（plugin）
- Gson

## 许可证

All Rights Reserved
