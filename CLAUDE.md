# BlackBoxPro — AI 上下文文件

## 项目概述

BlackBoxPro 是一个 Minecraft 自动化黑盒测试框架，通过 Plugin Message Channel 实现服务端→客户端的指令下发与结果回报。服务端插件向客户端 Mod 发送 JSON 指令，Mod 在客户端模拟真实玩家行为（移动、交互、GUI 操作、战斗等），用于对服务端插件逻辑进行自动化功能测试。

- 语言：Kotlin，JVM 21（1.21.11 模块）/ JVM 8（1.12.2 模块），`-Xjvm-default=all`
- 构建工具：Gradle (Kotlin DSL)，多模块项目
- 包根路径：`com.blackboxpro`
- Minecraft 版本：1.21.11、1.12.2（多版本架构，模块名含 MC 版本号）

## 项目结构

```
BlackBoxPro/                    # Gradle 根项目
├── fabric-1.21.11/             # Fabric 客户端 Mod（ClientModInitializer）
├── neoforge-1.21.11/           # NeoForge 客户端 Mod（@Mod）
├── forge-1.12.2/               # Forge 1.12.2 客户端 Mod（@Mod，独立 Gradle 项目）
├── plugin/                     # Bukkit 服务端插件（独立 Gradle 项目，不在 settings.gradle.kts 中）
├── gradle.properties           # mod_version（fabric/neoforge 共用）
├── build.gradle.kts            # 根构建脚本，subprojects 统一 group/version
└── settings.gradle.kts         # include fabric-1.21.11、neoforge-1.21.11
```

### 多版本模块命名规则

模块目录以 `{loader}-{mc_version}` 命名（如 `fabric-1.21.11`、`neoforge-1.21.11`、`forge-1.12.2`），便于后续新增其他 MC 版本的并行开发。Kotlin 包名保持 `com.blackboxpro.{fabric|neoforge|forge}` 不含版本号。

### 子模块职责

| 模块 | 角色 | 框架 | 入口类 |
|------|------|------|--------|
| `fabric-1.21.11` | 客户端 Mod | Fabric 1.21.11 + fabric-language-kotlin | `BlackBoxProFabric : ClientModInitializer` |
| `neoforge-1.21.11` | 客户端 Mod | NeoForge 21.11.x + KotlinForForge | `BlackBoxProNeoForge` (`@Mod`) |
| `forge-1.12.2` | 客户端 Mod | Forge 1.12.2 + Kotlin 1.9.25（独立 Gradle 项目，JDK 8） | `BlackBoxProForge` (`@Mod` object) |
| `plugin` | 服务端插件 | Paper/Spigot + TabooLib 6.2.4 | `BlackBoxPro : Plugin()` (object) |

### 版本号管理

- `gradle.properties` (根) → `version=1.3.0` → fabric/neoforge 共用
- `forge-1.12.2/gradle.properties` → `mod_version=x.x.x` → forge 独立版本
- `plugin/gradle.properties` → `version=x.x.x` → plugin 独立版本
- 各模块独立演进，CI 通过版本号变化检测触发对应构建

## 通讯架构

```
┌─────────────────────┐     blackbox:command      ┌─────────────────────┐
│   Bukkit Server     │ ────────────────────────▶  │  Fabric/NeoForge Mod │
│   (plugin 模块)     │                            │  (客户端执行端)      │
│                     │ ◀────────────────────────  │                     │
│                     │     blackbox:response      │                     │
└─────────────────────┘                            └─────────────────────┘
```

消息格式：JSON over Plugin Message Channel，VarInt(length) + UTF-8 bytes 编码。

指令消息 (Server → Client)：`{ id, action, params, delay }`
响应消息 (Client → Server)：`{ id, status, message, data }`

## 技术栈

### 客户端 Mod (fabric-1.21.11 / neoforge-1.21.11)

- Fabric API / NeoForge
- fabric-language-kotlin / KotlinForForge
- SLF4J 日志
- 无外部依赖，纯 Minecraft 协议操作

### 客户端 Mod (forge-1.12.2)

- Forge 1.12.2-14.23.5.2860 + ForgeGradle 2.3
- Kotlin 1.9.25（JDK 8）
- Log4j 日志（Forge 内置）
- 独立 Gradle 项目（Groovy DSL），不在根 settings.gradle.kts 中
- 网络层使用 FMLEventChannel + CPacketCustomPayload
- 功能为 1.21.11 版本的最大兼容子集（约 80+ 个 Action）

### 服务端插件 (plugin)

- TabooLib 6.2.4（`io.izzel.taboolib` Gradle 插件 2.0.30）
- TabooLib 模块：Basic, Bukkit, BukkitUtil, CommandHelper, MinecraftChat
- NMS：`ink.ptms.core:v12105:12105` (mapped + universal)
- Gson 2.11.0

## 客户端 Mod 架构 (fabric / neoforge / forge 三端对称)

三个 Mod 模块结构对称，逻辑一致（forge-1.12.2 为最大兼容子集）：

```
com.blackboxpro.{fabric|neoforge|forge}
├── action/              # 行为执行器（每个 Action 一个类）
│   ├── movement/        # 移动类：PlayerMoveAction, PlayerLookAction...
│   ├── block/           # 方块交互：DigStartAction, PlaceBlockAction...
│   ├── entity/          # 实体交互：AttackEntityAction, InteractEntityAction...
│   ├── container/       # 容器/GUI：ClickSlotAction, CloseContainerAction...
│   ├── player/          # 玩家状态：SneakStartAction, DropItemAction...
│   ├── chat/            # 聊天命令：ChatMessageAction, ChatCommandAction
│   ├── client/          # 客户端设置：ClientInformationAction, ScreenshotAction...
│   ├── advanced/        # 进阶交互：EditBookAction, UpdateSignAction...
│   ├── debug/           # 调试：KeepAliveAction, PongAction...
│   ├── query/           # 查询行为：QueryPlayerStateAction, QueryBlockStateAction...
│   └── composite/       # 复合行为：PathfindToAction, BreakBlockAction, BatchAction...
├── dispatcher/          # 调度层：ActionRegistry, CommandDispatcher, 消息模型
├── network/             # 网络层：Channel 注册、Payload 编解码
├── config/              # 配置：BlackBoxConfig
└── util/                # 工具：DirectionUtil, HandUtil, JsonUtil, MathUtil, InjectedInput
```

核心流程：
1. `NetworkHandler` 注册 `blackbox:command` / `blackbox:response` 通道
2. 收到指令 → `CommandDispatcher` 解析 JSON → 查找 `ActionRegistry` → 调度到主线程执行
3. `ActionExecutor.execute()` 执行具体行为 → 通过 response 通道回报结果
4. 复合行为通过 `TickScheduler` 跨 tick 调度

### ActionExecutor 接口

所有行为实现 `ActionExecutor` 接口，通过 `ActionRegistry.registerAll()` 在启动时注册。当前已注册 ~86 个行为，覆盖 Minecraft 全部 Serverbound 协议包 + 查询行为。

## 服务端插件架构 (plugin)

```
com.blackboxpro.plugin
├── api/                 # 公开 API
│   ├── BlackBoxApi      # 三种调用模式：fire-and-forget / callback / CompletableFuture
│   └── action/          # 预封装的高级 Action 辅助方法
│       ├── MovementActions, PlayerActions, EntityActions, ContainerActions
│       ├── ChatActions, ClientActions, CompositeActions, NavigationActions
│       ├── QueryActions, ScreenshotActions
│       └── HighLevelActions  # 面向场景的语义化高级 API
├── channel/             # Plugin Message 通讯层
│   ├── ChannelHandler   # 发送/接收/回调管理（@Awake 自动注册）
│   ├── BlackBoxChannels # Channel ID 常量
│   ├── CommandMessage    # 指令消息模型
│   └── ResponseMessage   # 响应消息模型
├── command/             # 命令系统
│   ├── BlackBoxCommand  # /blackbox send|exec|test|status|reload（@CommandHeader）
│   ├── BlackBoxTestRunner # 集成测试执行器（截图 + 三阶段验证）
│   ├── ActionParamRegistry # Action 参数元数据（Tab 补全）
│   └── FlatParamParser  # key:value 扁平化参数解析器
└── config/              # 配置
    └── BlackBoxSettings # @Config("config.yml")，debug/timeout/maxPayload
```

## 开发规范

### TabooLib 优先原则（仅 plugin 模块）

- 调度器：`submit(async = true) { ... }` / `submitAsync { ... }`，禁止 `BukkitRunnable`
- 命令：`@CommandHeader` + DSL，禁止 `plugin.yml` 注册
- 配置：`@Config` + `Configuration`，禁止手动 `getConfig()`
- 日志：`info()`, `warning()`, `severe()`
- 事件：`@SubscribeEvent`
- 生命周期：`@Awake(LifeCycle.ENABLE)` / `@Awake(LifeCycle.DISABLE)`

### Kotlin 惯用语

- 默认 `val`，按需 `var`
- 严禁 `!!`，使用 `?.let`, `?:` 处理空安全
- 简单逻辑用表达式体 (`=`)，复杂逻辑用代码块 (`{}`)
- 扩展函数必须有明确语境，避免污染全局

### 线程安全

- 客户端 Mod：所有 Minecraft 状态操作必须在客户端主线程执行，通过 `MinecraftClient.getInstance().execute {}` 调度
- 服务端插件：Bukkit API 调用必须在主线程，异步操作使用 TabooLib `submitAsync`

### 新增 Action 的标准流程

1. 在 `action/` 对应子包下创建 `XxxAction` 类，实现 `ActionExecutor`
2. 在 `ActionRegistry.registerAll()` 中注册 `register("action_id", XxxAction())`
3. fabric、neoforge、forge-1.12.2 三个模块需同步添加（forge 为兼容子集，API 不同时需适配）
4. plugin 模块：在对应的 `XxxActions` object 中新增方法，`ActionParamRegistry` 注册参数

### 错误处理

- 每个 Action 执行失败时必须通过 response 通道回报错误信息
- 不得因单个指令失败导致 Mod 崩溃或断开连接

## 构建与发布

### 本地构建

```bash
# 构建 Fabric + NeoForge Mod
./gradlew :fabric-1.21.11:build :neoforge-1.21.11:build

# 构建 Forge 1.12.2 Mod（独立 Gradle 项目，需要 JDK 8）
cd forge-1.12.2 && ./gradlew build

# 构建服务端插件（独立 Gradle 项目）
cd plugin && ./gradlew jar
```

### 产物路径

- `fabric-1.21.11/build/libs/blackboxpro-fabric-{mod_version}.jar`
- `neoforge-1.21.11/build/libs/blackboxpro-neoforge-{mod_version}.jar`
- `forge-1.12.2/build/libs/blackboxpro-forge-{mod_version}.jar`
- `plugin/build/libs/BlackBoxPro-Plugin-{plugin_version}.jar`

### CI/CD

`.github/workflows/release.yml`：push 到 main 时检测版本号变化，自动构建并创建 GitHub Release。

## 行为分类速查

| 分类 | Action ID 示例 | 数量 |
|------|---------------|------|
| 移动与位置 | `player_move`, `player_look`, `confirm_teleportation` | 8 |
| 方块交互 | `dig_start`, `place_block`, `use_item` | 5 |
| 实体交互 | `attack_entity`, `interact_entity`, `swing_arm` | 4 |
| 容器/GUI | `click_slot`, `close_container`, `set_carried_item` | 11 |
| 玩家状态 | `sneak_start`, `drop_item`, `swap_hands`, `jump` | 17 |
| 聊天命令 | `chat_message`, `chat_command` | 2 |
| 客户端设置 | `client_information`, `player_abilities`, `screenshot` | 4 |
| 进阶交互 | `edit_book`, `update_sign`, `select_trade` | 16 |
| 调试 | `keep_alive`, `pong`, `custom_payload` | 6 |
| 复合行为 | `pathfind_to`, `break_block`, `batch`, `craft_recipe` | 14 |
| 查询行为 | `query_player_state`, `query_block_state`, `query_scoreboard` | 14 |

## v1.1.0 截图功能开发指引

> 完整设计见 `开发文档-1.1.0.md`。已实现，三端（fabric/neoforge/forge）均已支持。

## v1.2.x 移动系统重构

### InjectedInput 输入注入机制

`PlayerMoveAction`、`PlayerMoveLookAction`、`NavigationController` 从直接 `setPosition + sendPacket` 改为通过 `InjectedInput` 注入键盘输入，让 MC 物理引擎处理碰撞/重力/速度。

三端实现：
- Fabric: `InjectedInput : Input()` — 替换 `KeyboardInput`
- NeoForge: `InjectedInput : ClientInput()` — 替换 `KeyboardInput`
- Forge 1.12.2: `InjectedMovementInput : MovementInput()` — 替换 `MovementInputFromOptions`

关键设计：
- `installedPlayer` 字段保存安装时的 player 引用，防止断线重连后 `uninstall` 恢复到错误实例
- `uninstall()` 支持无参调用，校验 `target.input === this` 后才恢复
- `player_move` 参数从 `x,y,z,onGround` 改为 `x,y,z,speed,timeout`
- `player_move_look` 参数从 `x,y,z,yaw,pitch,onGround` 改为 `x,y,z,pitch,speed,timeout`（yaw 自动朝向目标）

### exec 扁平化命令

`/blackbox exec <player> <action> [key:value ...]` — 不使用 JSON 格式，通过 `FlatParamParser` 解析 `key:value` 参数，`ActionParamRegistry` 提供 Tab 补全。

## v1.3.0 查询系统

### 新增 6 个 Query Action（三端同步实现）

| Action ID | 功能 | 参数 |
|-----------|------|------|
| `query_block_state` | 查询指定坐标方块状态 | `x`, `y`, `z` |
| `query_world_state` | 查询世界全局状态 | 无 |
| `query_tab_list` | 查询 Tab 列表玩家 | `limit` |
| `query_scoreboard` | 查询记分板状态 | `objective` |
| `query_screen_state` | 查询当前屏幕/GUI | 无 |
| `query_boss_bar` | 查询 Boss Bar | 无 |

### QueryPlayerState 扩展（v1.3.0 新增 12 字段）

`absorption`, `armorValue`, `airSupply`, `maxAirSupply`, `isSwimming`, `isUsingItem`, `isFallFlying`, `fallDistance`, `vehicleId`, `dimension`, `biome`, `mainHandItem`

## 三端映射差异速查表

### Fabric (Yarn) ↔ NeoForge (Mojang) ↔ Forge 1.12.2

| 概念 | Fabric (Yarn) | NeoForge (Mojang) | Forge 1.12.2 |
|------|--------------|-------------------|--------------|
| 客户端单例 | `MinecraftClient.getInstance()` | `Minecraft.getInstance()` | `Minecraft.getMinecraft()` |
| 世界 | `client.world` | `client.level` | `mc.world` |
| 当前屏幕 | `client.currentScreen` | `client.screen` | `mc.currentScreen` |
| 网络连接 | `client.networkHandler` | `client.connection` | `mc.connection` |
| 发包 | `networkHandler.sendPacket(...)` | `connection.send(...)` | `connection.sendPacket(...)` |
| yaw/pitch | `player.yaw` / `player.pitch` | `player.yRot` / `player.xRot` | `player.rotationYaw` / `player.rotationPitch` |
| 位置 | `player.x/y/z` | `player.x/y/z` | `player.posX/posY/posZ` |
| 是否在地面 | `player.isOnGround` | `player.onGround()` | `player.onGround` |
| 是否潜行 | `player.isSneaking` | `player.isShiftKeyDown` | `player.isSneaking` |
| 是否死亡 | `player.isDead` | `player.isDeadOrDying` | `player.isDead` |
| 饥饿管理 | `player.hungerManager` | `player.foodData` | `player.foodStats` |
| 容器处理器 | `player.currentScreenHandler` | `player.containerMenu` | `player.openContainer` |
| 玩家背包容器 | `player.playerScreenHandler` | `player.inventoryMenu` | `player.inventoryContainer` |
| 窗口 ID | `handler.syncId` | `handler.containerId` | `container.windowId` |
| 注册表 | `Registries.XXX.getId()` | `BuiltInRegistries.XXX.getKey()` | `xxx.registryName` |
| ResourceKey 标识 | `.value.toString()` | `.identifier().toString()` | N/A (dimension int) |
| AABB 扩展 | `boundingBox.expand(r)` | `boundingBox.inflate(r)` | `entityBoundingBox.grow(r)` |
| 获取实体 | `world.getOtherEntities(...)` | `world.getEntities(...)` | `world.getEntitiesInAABBexcluding(...)` |
| 距离平方 | `entity.squaredDistanceTo(...)` | `entity.distanceToSqr(...)` | `entity.getDistanceSq(...)` |
| UUID 字符串 | `entity.uuidAsString` | `entity.stringUUID` | `entity.uniqueID.toString()` |
| 帧缓冲 | `client.framebuffer` | `client.mainRenderTarget` | N/A |
| 运行目录 | `client.runDirectory` | `client.gameDirectory` | N/A |
| 滑翔判定 | `player.isGliding` | `player.isFallFlying` | `player.isElytraFlying` |
| 护甲值 | `player.armor` | `player.armorValue` | `player.totalArmorValue` |
| 主手物品 | `player.mainHandStack` | `player.mainHandItem` | `player.heldItemMainhand` |
| Tab 列表 | `networkHandler.playerList` | `connection.onlinePlayers` | `connection.playerInfoMap` |
| 延迟 | `entry.latency` | `entry.latency` | `entry.responseTime` |
| 显示名 | `entry.displayName` | `entry.tabListDisplayName` | `entry.displayName` |
| 队伍 | `entry.scoreboardTeam` | `entry.team` | `entry.playerTeam` |
| 记分板显示槽 | `ScoreboardDisplaySlot.SIDEBAR` | `DisplaySlot.SIDEBAR` | `getObjectiveInDisplaySlot(1)` |
| 记分板分数 | `scoreboard.getScoreboardEntries(obj)` | `scoreboard.listPlayerScores(obj)` | `scoreboard.getSortedScores(obj)` |
| 玩家队伍 | `scoreboard.getScoreHolderTeam(player.nameForScoreboard)` | `scoreboard.getPlayersTeam(player.scoreboardName)` | `scoreboard.getPlayersTeam(player.name)` |
| BossBar HUD | `client.inGameHud.bossBarHud` | `client.gui.bossOverlay` | `mc.ingameGUI.bossOverlay` |
| BossBar 字段名 | `bossBars` | `events` | `mapBossInfos` |
| BossBar 类型 | `BossBar` | `BossEvent` | `BossInfo` |

## TabooLib 文档查询指引

涉及 TabooLib 相关开发时（仅 plugin 模块），应先查询文档获取准确信息。

### 1. Wiki 文档查询（优先）
```bash
node .codex/skills/taboolib/extract.js <主题|目录> [关键词]
```

常用主题别名：
| 别名 | 主题 |
|------|------|
| 配置/yaml | config |
| 调度/submit/异步 | scheduler |
| 事件/listener | event-manager |
| 命令/cmd | command |
| 物品/item | item-builder |
| nms/跨版本 | nms-proxy |

### 2. 源码查询
```bash
node .codex/skills/taboolib/extract.js --src <模块> [文件名]
```

### 3. 直接读取源码
TabooLib 6.2.4 源码位于 `E:\Desktop\IDEA\taboolib\`

### 关键 API 速查

**生命周期:** `NONE → CONST → INIT → LOAD → ENABLE → ACTIVE → DISABLE`

**调度器:** `submit(async, delay, period) { }` / `submitAsync { }`

**命令:** `@CommandHeader` + `@CommandBody` + `mainCommand { }` / `subCommand { }`

**配置:** `@Config("config.yml") lateinit var conf: Configuration`

**平台函数:** `info()`, `warning()`, `severe()`, `adaptPlayer()`, `console()`
