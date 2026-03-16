# BlackBoxPro — AI 上下文文件

## 项目概述

BlackBoxPro 是一个 Minecraft 自动化黑盒测试框架，通过 Plugin Message Channel 实现服务端→客户端的指令下发与结果回报。服务端插件向客户端 Mod 发送 JSON 指令，Mod 在客户端模拟真实玩家行为（移动、交互、GUI 操作、战斗等），用于对服务端插件逻辑进行自动化功能测试。

- 语言：Kotlin，JVM 21，`-Xjvm-default=all`
- 构建工具：Gradle (Kotlin DSL)，多模块项目
- 包根路径：`com.blackboxpro`
- Minecraft 版本：1.21.11

## 项目结构

```
BlackBoxPro/                    # Gradle 根项目
├── fabric/                     # Fabric 客户端 Mod（ClientModInitializer）
├── neoforge/                   # NeoForge 客户端 Mod（@Mod）
├── plugin/                     # Bukkit 服务端插件（独立 Gradle 项目，不在 settings.gradle.kts 中）
├── gradle.properties           # mod_version（fabric/neoforge 共用）
├── build.gradle.kts            # 根构建脚本，subprojects 统一 group/version
└── settings.gradle.kts         # 仅 include fabric、neoforge
```

### 三个子模块的职责

| 模块 | 角色 | 框架 | 入口类 |
|------|------|------|--------|
| `fabric` | 客户端 Mod | Fabric 1.21.11 + fabric-language-kotlin | `BlackBoxProFabric : ClientModInitializer` |
| `neoforge` | 客户端 Mod | NeoForge 21.11.x + KotlinForForge | `BlackBoxProNeoForge` (`@Mod`) |
| `plugin` | 服务端插件 | Paper/Spigot + TabooLib 6.2.4 | `BlackBoxPro : Plugin()` (object) |

### 版本号管理

- `gradle.properties` (根) → `mod_version=1.0.0` → fabric/neoforge 共用
- `plugin/gradle.properties` → `version=1.0.2` → plugin 独立版本
- 两者独立演进，CI 通过版本号变化检测触发对应构建

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

### 客户端 Mod (fabric / neoforge)

- Fabric API / NeoForge
- fabric-language-kotlin / KotlinForForge
- SLF4J 日志
- 无外部依赖，纯 Minecraft 协议操作

### 服务端插件 (plugin)

- TabooLib 6.2.4（`io.izzel.taboolib` Gradle 插件 2.0.30）
- TabooLib 模块：Basic, Bukkit, BukkitUtil, CommandHelper, MinecraftChat
- NMS：`ink.ptms.core:v12105:12105` (mapped + universal)
- Gson 2.11.0

## 客户端 Mod 架构 (fabric / neoforge 对称)

两个 Mod 模块结构完全对称，代码几乎一致：

```
com.blackboxpro.{fabric|neoforge}
├── action/              # 行为执行器（每个 Action 一个类）
│   ├── movement/        # 移动类：PlayerMoveAction, PlayerLookAction...
│   ├── block/           # 方块交互：DigStartAction, PlaceBlockAction...
│   ├── entity/          # 实体交互：AttackEntityAction, InteractEntityAction...
│   ├── container/       # 容器/GUI：ClickSlotAction, CloseContainerAction...
│   ├── player/          # 玩家状态：SneakStartAction, DropItemAction...
│   ├── chat/            # 聊天命令：ChatMessageAction, ChatCommandAction
│   ├── client/          # 客户端设置：ClientInformationAction...
│   ├── advanced/        # 进阶交互：EditBookAction, UpdateSignAction...
│   ├── debug/           # 调试：KeepAliveAction, PongAction...
│   └── composite/       # 复合行为：PathfindToAction, BreakBlockAction, BatchAction...
├── dispatcher/          # 调度层：ActionRegistry, CommandDispatcher, 消息模型
├── network/             # 网络层：Channel 注册、Payload 编解码
├── config/              # 配置：BlackBoxConfig
└── util/                # 工具：DirectionUtil, HandUtil, JsonUtil, MathUtil
```

核心流程：
1. `NetworkHandler` 注册 `blackbox:command` / `blackbox:response` 通道
2. 收到指令 → `CommandDispatcher` 解析 JSON → 查找 `ActionRegistry` → 调度到主线程执行
3. `ActionExecutor.execute()` 执行具体行为 → 通过 response 通道回报结果
4. 复合行为通过 `TickScheduler` 跨 tick 调度

### ActionExecutor 接口

所有行为实现 `ActionExecutor` 接口，通过 `ActionRegistry.registerAll()` 在启动时注册。当前已注册 ~70 个行为，覆盖 Minecraft 全部 Serverbound 协议包。

## 服务端插件架构 (plugin)

```
com.blackboxpro.plugin
├── api/                 # 公开 API
│   ├── BlackBoxApi      # 三种调用模式：fire-and-forget / callback / CompletableFuture
│   └── action/          # 预封装的高级 Action 辅助方法
├── channel/             # Plugin Message 通讯层
│   ├── ChannelHandler   # 发送/接收/回调管理（@Awake 自动注册）
│   ├── BlackBoxChannels # Channel ID 常量
│   ├── CommandMessage    # 指令消息模型
│   └── ResponseMessage   # 响应消息模型
├── command/             # 命令系统
│   ├── BlackBoxCommand  # /blackbox send|test|status|reload（@CommandHeader）
│   └── BlackBoxTestRunner # 集成测试执行器
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
3. fabric 和 neoforge 两个模块需同步添加

### 错误处理

- 每个 Action 执行失败时必须通过 response 通道回报错误信息
- 不得因单个指令失败导致 Mod 崩溃或断开连接

## 构建与发布

### 本地构建

```bash
# 构建 Fabric + NeoForge Mod
./gradlew :fabric:build :neoforge:build

# 构建服务端插件（独立 Gradle 项目）
cd plugin && ./gradlew jar
```

### 产物路径

- `fabric/build/libs/blackboxpro-fabric-{mod_version}.jar`
- `neoforge/build/libs/blackboxpro-neoforge-{mod_version}.jar`
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
| 玩家状态 | `sneak_start`, `drop_item`, `swap_hands` | 16 |
| 聊天命令 | `chat_message`, `chat_command` | 2 |
| 客户端设置 | `client_information`, `player_abilities` | 3 |
| 进阶交互 | `edit_book`, `update_sign`, `select_trade` | 16 |
| 调试 | `keep_alive`, `pong`, `custom_payload` | 6 |
| 复合行为 | `pathfind_to`, `break_block`, `batch`, `craft_recipe` | 14 |

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
