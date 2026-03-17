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

- `gradle.properties` (根) → `mod_version=1.0.0` → fabric/neoforge 共用
- `forge-1.12.2/gradle.properties` → `mod_version=1.0.0` → forge 独立版本
- `plugin/gradle.properties` → `version=1.0.2` → plugin 独立版本
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
- 功能为 1.21.11 版本的最大兼容子集（约 75 个 Action）

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
| 玩家状态 | `sneak_start`, `drop_item`, `swap_hands` | 16 |
| 聊天命令 | `chat_message`, `chat_command` | 2 |
| 客户端设置 | `client_information`, `player_abilities`, `screenshot` | 4 |
| 进阶交互 | `edit_book`, `update_sign`, `select_trade` | 16 |
| 调试 | `keep_alive`, `pong`, `custom_payload` | 6 |
| 复合行为 | `pathfind_to`, `break_block`, `batch`, `craft_recipe` | 14 |

## v1.1.0 截图功能开发指引

> 完整设计见 `开发文档-1.1.0.md`，本节提供面向实现的精确提示词。

### 总览：需要变更的文件

```
fabric-1.21.11/src/main/kotlin/com/blackboxpro/fabric/
├── action/client/ScreenshotAction.kt     # 新增
├── util/ScreenshotHelper.kt              # 新增
├── config/BlackBoxConfig.kt              # 修改：新增 ScreenshotConfig
└── dispatcher/ActionRegistry.kt          # 修改：注册 screenshot

neoforge-1.21.11/src/main/kotlin/com/blackboxpro/neoforge/
├── action/client/ScreenshotAction.kt     # 新增（对称）
├── util/ScreenshotHelper.kt              # 新增（对称）
├── config/BlackBoxConfig.kt              # 修改：新增 ScreenshotConfig
└── dispatcher/ActionRegistry.kt          # 修改：注册 screenshot

plugin/src/main/kotlin/com/blackboxpro/plugin/
└── api/action/
    ├── ScreenshotActions.kt              # 新增
    └── HighLevelActions.kt               # 修改：新增 screenshot()
```

### 模块 1：fabric — ScreenshotHelper 工具类

文件：`fabric-1.21.11/src/main/kotlin/com/blackboxpro/fabric/util/ScreenshotHelper.kt`

要求：
- `object ScreenshotHelper`，与现有 `DirectionUtil`/`HandUtil`/`JsonUtil`/`MathUtil` 同级
- 包名 `com.blackboxpro.fabric.util`
- 提供 `data class ScreenshotResult(filePath: Path, width: Int, height: Int, fileSize: Long)`
- `fun capture(directory: Path, fileName: String): ScreenshotResult`
  - 调用 `net.minecraft.client.util.ScreenshotRecorder.takeScreenshot(framebuffer)` 获取 `NativeImage`（Fabric Yarn 映射）
  - `framebuffer` 从 `MinecraftClient.getInstance().framebuffer` 获取
  - `Files.createDirectories(directory)` 确保目录存在
  - `image.writeTo(directory.resolve("$fileName.png"))` 写入 PNG
  - `finally { image.close() }` 释放 NativeImage 资源
  - 返回 `ScreenshotResult`，`fileSize` 通过 `Files.size()` 获取
- `fun nextIndex(directory: Path): Int`
  - 目录不存在返回 1
  - 扫描目录下 `^(\d{3}).*\.png$` 文件，取最大编号 + 1
  - 使用 `Files.list(directory).use { stream -> ... }` 确保流关闭
- `fun sanitize(name: String): String` — 替换 `[^a-zA-Z0-9_\-.]` 为 `_`
- 日志：`LoggerFactory.getLogger("BlackBoxPro-Screenshot")`

### 模块 2：neoforge — ScreenshotHelper 工具类

文件：`neoforge-1.21.11/src/main/kotlin/com/blackboxpro/neoforge/util/ScreenshotHelper.kt`

与 fabric 版完全一致，仅以下差异：
- 包名 `com.blackboxpro.neoforge.util`
- 截图 API：`net.minecraft.client.Screenshot.takeScreenshot(framebuffer)`（Mojang 映射，非 Yarn 的 `ScreenshotRecorder`）
- 客户端单例：`Minecraft.getInstance()`（非 `MinecraftClient`）
- framebuffer 获取：`Minecraft.getInstance().mainRenderTarget`（非 `.framebuffer`）

### 模块 3：fabric — ScreenshotAction 执行器

文件：`fabric-1.21.11/src/main/kotlin/com/blackboxpro/fabric/action/client/ScreenshotAction.kt`

要求：
- 包名 `com.blackboxpro.fabric.action.client`，与 `ClientInformationAction` 同包
- `class ScreenshotAction : ActionExecutor`
- Action ID：`"screenshot"`
- 参数解析（均使用 `util/JsonUtil.kt` 中的扩展函数）：
  - `playerName`: `params.getStringOrNull("playerName") ?: player.gameProfile.name`
  - `testId`: `params.getStringOrNull("testId") ?: "default"`
  - `prefix`: `params.getStringOrNull("prefix")`（可选，可为 null）
- 前置检查：`client.player ?: return ActionResult.fail("Player not available")`
- 目录构建：`client.runDirectory.toPath().resolve("screenshots/blackboxpro").resolve(sanitize(playerName)).resolve(sanitize(testId))`
- 编号：`ScreenshotHelper.nextIndex(directory)`，超过 999 返回 `ActionResult.fail`
- 文件名：有 prefix → `"{indexStr}_{sanitize(prefix)}"`，无 prefix → `indexStr`，其中 `indexStr = index.toString().padStart(3, '0')`
- 调用 `ScreenshotHelper.capture(directory, fileName)` 执行截图
- 响应 data（JsonObject）：
  - `filePath`：相对于 `client.runDirectory` 的路径，`replace('\\', '/')`
  - `width`、`height`、`fileSize`、`index`
- 返回 `ActionResult.ok("Screenshot saved: $fileName.png", data)`

### 模块 4：neoforge — ScreenshotAction 执行器

文件：`neoforge-1.21.11/src/main/kotlin/com/blackboxpro/neoforge/action/client/ScreenshotAction.kt`

与 fabric 版完全一致，仅以下差异：
- 包名 `com.blackboxpro.neoforge.action.client`
- import 路径：`com.blackboxpro.neoforge.action.*`、`com.blackboxpro.neoforge.util.*`
- 客户端单例：`Minecraft.getInstance()`
- 玩家获取：`client.player ?: ...`（NeoForge 的 `Minecraft.player` 与 Fabric 的 `MinecraftClient.player` 属性名相同）
- 玩家名：`player.gameProfile.name`（两端一致）
- 运行目录：`client.gameDirectory.toPath()`（NeoForge Mojang 映射，非 Fabric 的 `runDirectory`）

### 模块 5：fabric/neoforge — ActionRegistry 注册

两个模块的 `dispatcher/ActionRegistry.kt` 均需修改：

在 `// === 客户端设置与信息 ===` 分类末尾，`resource_pack_response` 之后新增一行：

```kotlin
// === 客户端设置与信息 ===
register("client_information", ClientInformationAction())
register("player_abilities", PlayerAbilitiesAction())
register("resource_pack_response", ResourcePackResponseAction())
register("screenshot", ScreenshotAction())  // ← 新增
```

import 已有 `import com.blackboxpro.{fabric|neoforge}.action.client.*`，无需新增 import。

### 模块 6：fabric/neoforge — BlackBoxConfig 配置扩展

两个模块的 `config/BlackBoxConfig.kt` 均需修改：

在现有配置数据类之后（`SafetyConfig` 之后）新增：

```kotlin
data class ScreenshotConfig(
    val rootDirectory: String = "screenshots/blackboxpro",
    val maxPerTest: Int = 999
)
```

在 `BlackBoxConfig` 主数据类中新增字段：

```kotlin
data class BlackBoxConfig(
    val logging: LoggingConfig = LoggingConfig(),
    val network: NetworkConfig = NetworkConfig(),
    val execution: ExecutionConfig = ExecutionConfig(),
    val pathfinding: PathfindingConfig = PathfindingConfig(),
    val safety: SafetyConfig = SafetyConfig(),
    val screenshot: ScreenshotConfig = ScreenshotConfig()  // ← 新增
)
```

注意：两个模块的 `BlackBoxConfig.companion` 差异仅在配置文件路径：
- Fabric：`FabricLoader.getInstance().configDir.resolve("blackboxpro-fabric.json")`
- NeoForge：`FMLPaths.CONFIGDIR.get().resolve("blackboxpro-neoforge.json")`

companion object 内部无需修改，Gson 会自动序列化/反序列化新增字段。

### 模块 7：plugin — ScreenshotActions 服务端 API

文件：`plugin/src/main/kotlin/com/blackboxpro/plugin/api/action/ScreenshotActions.kt`

要求：
- `object ScreenshotActions`，与现有 `ClientActions`/`CompositeActions` 等同级
- 遵循现有 API 风格：返回 `CompletableFuture<ResponseMessage>`，内部调用 `BlackBoxApi.sendAsync()`
- 方法签名：

```kotlin
fun screenshot(
    player: Player,
    testId: String = "default",
    prefix: String? = null,
    playerName: String? = null
): CompletableFuture<ResponseMessage>
```

- 实现：`BlackBoxApi.sendAsync(player, "screenshot", JsonObject().apply { ... })`
- `playerName` 和 `prefix` 仅在非 null 时 `addProperty`（与现有 API 中可选参数的处理方式一致）

### 模块 8：plugin — HighLevelActions 扩展

文件：`plugin/src/main/kotlin/com/blackboxpro/plugin/api/action/HighLevelActions.kt`

在文件末尾 `}` 之前，新增截图分类：

```kotlin
// ======================== 截图 ========================

/**
 * 触发客户端截图。
 */
fun screenshot(
    player: Player,
    testId: String = "default",
    prefix: String? = null
): CompletableFuture<ResponseMessage> =
    ScreenshotActions.screenshot(player, testId, prefix, player.name)
```

### Fabric / NeoForge 映射差异速查（截图相关）

| 概念 | Fabric (Yarn) | NeoForge (Mojang) |
|------|--------------|-------------------|
| 客户端单例 | `MinecraftClient.getInstance()` | `Minecraft.getInstance()` |
| 帧缓冲 | `client.framebuffer` | `client.mainRenderTarget` |
| 截图 API | `ScreenshotRecorder.takeScreenshot(fb)` | `Screenshot.takeScreenshot(fb)` |
| 截图 API 包 | `net.minecraft.client.util.ScreenshotRecorder` | `net.minecraft.client.Screenshot` |
| 运行目录 | `client.runDirectory` | `client.gameDirectory` |
| NativeImage | `net.minecraft.client.texture.NativeImage` | `com.mojang.blaze3d.platform.NativeImage` |
| 网络连接 | `client.networkHandler` | `client.connection` |
| 发包 | `networkHandler.sendPacket(...)` | `connection.send(...)` |

### 通讯协议（截图 Action）

指令 (Server → Client)：
```json
{
    "id": "uuid",
    "action": "screenshot",
    "params": {
        "playerName": "Steve",
        "testId": "shop_gui_test",
        "prefix": "after_warp"
    }
}
```

所有 params 字段均可选：
- `playerName` 缺省取客户端当前玩家名
- `testId` 缺省 `"default"`
- `prefix` 缺省无前缀

响应 (Client → Server)：
```json
{
    "id": "uuid",
    "status": "success",
    "message": "Screenshot saved: 001_after_warp.png",
    "data": {
        "filePath": "screenshots/blackboxpro/Steve/shop_gui_test/001_after_warp.png",
        "width": 1920,
        "height": 1080,
        "fileSize": 2048576,
        "index": 1
    }
}
```

### 截图文件存储规则

```
<minecraft_run_dir>/screenshots/blackboxpro/<playerName>/<testId>/<index>_<prefix>.png

编号规则：
- 三位数字，001 起始，同目录下自动递增
- 扫描已有文件取最大编号 + 1
- 上限 999，溢出返回 failure

文件名示例：
- 001_before_open.png   (有 prefix)
- 002.png               (无 prefix)

玩家名/testId 中的非法字符替换为 _
```

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
