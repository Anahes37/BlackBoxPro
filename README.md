# BlackBoxPro

Minecraft 自动化黑盒测试框架。通过 Plugin Message Channel 实现服务端插件向客户端 Mod 下发 JSON 指令，Mod 在客户端模拟真实玩家行为（移动、交互、GUI 操作、战斗等），用于对服务端插件逻辑进行自动化功能测试。

## 架构

```
┌─────────────────────┐     blackbox:command      ┌──────────────────────┐
│   Bukkit Server     │ ────────────────────────▶  │  Fabric / NeoForge   │
│   (plugin 模块)     │                            │  / Forge 客户端 Mod  │
│                     │ ◀────────────────────────  │                     │
│                     │     blackbox:response      │                     │
└─────────────────────┘                            └──────────────────────┘
```

服务端插件通过 `blackbox:command` 通道发送 JSON 指令，客户端 Mod 执行后通过 `blackbox:response` 通道回报结果。

## 模块

| 模块 | 角色 | 框架 | JVM |
|------|------|------|-----|
| `mod/common` | 无 MC 依赖的共享协议层 | Kotlin + Gson | 8 |
| `mod:1.21.11:fabric` | 客户端 Mod | Fabric 1.21.11 + fabric-language-kotlin | 21 |
| `mod:1.21.11:neoforge` | 客户端 Mod | NeoForge 21.11.x + KotlinForForge | 21 |
| `forge-1.12.2` | 客户端 Mod（遗留独立构建） | Forge 1.12.2 | 8 |
| `plugin` | 服务端插件实现，依赖 `common` | Paper/Spigot + TabooLib 6.2 | 21 |

仓库根现在是聚合构建入口：
- `mod` 负责客户端相关模块，结构参考 Zeus。
- `plugin` 和 `forge-1.12.2` 保持独立 Gradle 项目。
- `plugin` 通过组合构建依赖 `mod/common`。

## 支持的行为 (86+)

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

指令 (Server → Client)：
```json
{
  "id": "uuid",
  "action": "screenshot",
  "params": { "testId": "shop_gui_test", "prefix": "after_warp" },
  "delay": 0
}
```

响应 (Client → Server)：
```json
{
  "id": "uuid",
  "status": "success",
  "message": "Screenshot saved: 001_after_warp.png",
  "data": { "filePath": "screenshots/blackboxpro/Steve/shop_gui_test/001_after_warp.png" }
}
```

## 服务端 API

`BlackBoxApi` 提供三种调用模式：

```kotlin
// Fire-and-forget
BlackBoxApi.send(player, "chat_message", params)

// 回调
BlackBoxApi.send(player, "click_slot", params) { response ->
    // 处理响应
}

// CompletableFuture（推荐）
BlackBoxApi.sendAsync(player, "pathfind_to", params).thenAccept { response ->
    // 处理响应
}
```

同时提供高级封装 `HighLevelActions`，简化常见操作的参数构建。

## 构建

```bash
# 客户端模块（common + fabric + neoforge）
./gradlew -p mod buildAll

# 服务端插件（独立项目，组合构建依赖 mod/common）
cd plugin && ./gradlew jar

# Forge 1.12.2（独立项目，运行 Gradle 需 JDK 17+/21，JDK 8 toolchain 自动下载）
cd forge-1.12.2 && ./gradlew build

# 仓库根聚合构建
./gradlew mod_buildAll plugin_build

# 全量构建 + 收集产物到根 build/libs
./gradlew buildAll collectJars
```

产物路径：
- `mod/common/build/libs/blackboxpro-common-*.jar`
- `fabric-1.21.11/build/libs/blackboxpro-fabric-*.jar`
- `neoforge-1.21.11/build/libs/blackboxpro-neoforge-*.jar`
- `forge-1.12.2/build/libs/blackboxpro-forge-*.jar`
- `plugin/build/libs/BlackBoxPro-Plugin-*.jar`

## 技术栈

- Kotlin, JVM 21 / JVM 8 (forge-1.12.2)
- Gradle (Kotlin DSL)
- Fabric API / NeoForge / Forge
- TabooLib 6.2 (plugin)
- Gson

## 许可证

All Rights Reserved
