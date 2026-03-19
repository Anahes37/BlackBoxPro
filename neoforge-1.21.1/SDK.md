# BlackBoxPro SDK 接入文档

## 环境要求

- NeoForge 1.21.1（`21.1.x`）
- Kotlin for Forge `5.7.0`+
- JDK 21

## 从源码构建 SDK JAR

### 前置条件

- JDK 21
- Git

### 构建步骤

```bash
# 克隆仓库
git clone https://github.com/YsGqHY/BlackBoxPro.git
cd BlackBoxPro

# 构建 neoforge-1.21.1 模块
# Linux / macOS
JAVA_HOME=/path/to/jdk-21 ./gradlew :neoforge-1.21.1:build

# Windows (PowerShell)
$env:JAVA_HOME='C:\Program Files\Java\jdk-21'
.\gradlew :neoforge-1.21.1:build

# Windows (Git Bash)
JAVA_HOME="C:/Program Files/Java/jdk-21" ./gradlew :neoforge-1.21.1:build
```

### 产物路径

```
neoforge-1.21.1/build/libs/
├── BlackBoxPro-neoforge-1.21.1-1.3.1.jar          # SDK JAR
└── BlackBoxPro-neoforge-1.21.1-1.3.1-sources.jar  # 源码 JAR
```

### 发布到本地 Maven 仓库（可选）

在 `neoforge-1.21.1/build.gradle.kts` 中添加：

```kotlin
plugins {
    `maven-publish`
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = "com.blackboxpro"
            artifactId = "BlackBoxPro-neoforge-1.21.1"
        }
    }
}
```

然后执行：

```bash
./gradlew :neoforge-1.21.1:publishToMavenLocal
```

其他项目即可通过 `mavenLocal()` 引入：

```kotlin
repositories {
    mavenLocal()
}
dependencies {
    compileOnly("com.blackboxpro:BlackBoxPro-neoforge-1.21.1:1.3.1")
}
```

## 依赖引入

### 方式一：本地 JAR

将 `BlackBoxPro-neoforge-1.21.1-1.3.1.jar` 放入项目 `libs/` 目录：

```kotlin
// build.gradle.kts
dependencies {
    compileOnly(files("libs/BlackBoxPro-neoforge-1.21.1-1.3.1.jar"))
}
```

### 方式二：Maven 仓库（如已发布）

```kotlin
// build.gradle.kts
repositories {
    maven("http://repo.aeoliancloud.com/repository/releases")
}

dependencies {
    compileOnly("com.blackboxpro:BlackBoxPro-neoforge-1.21.1:1.3.1")
}
```

> 使用 `compileOnly`，运行时由用户自行安装 BlackBoxPro Mod。

### neoforge.mods.toml 依赖声明

```toml
[[dependencies.your_mod_id]]
    modId = "blackboxpro"
    type = "optional"
    versionRange = "[1.0.0,)"
    ordering = "AFTER"
    side = "CLIENT"
```

## 快速开始

### 1. 启动测试环境

```kotlin
import com.blackboxpro.neoforge.sdk.BlackBoxClient

// 默认监听 127.0.0.1:25580
BlackBoxClient.start()

// 或指定端口
BlackBoxClient.start(port = 25581, bindAddress = "127.0.0.1")
```

### 2. 停止测试环境

```kotlin
BlackBoxClient.stop()
```

### 3. 检查运行状态

```kotlin
if (BlackBoxClient.isRunning) {
    // HTTP Server 正在运行
}
```

## 注册自定义 Action

```kotlin
import com.blackboxpro.neoforge.sdk.BlackBoxClient
import com.blackboxpro.neoforge.action.ActionResult
import com.google.gson.JsonObject

// 注册
BlackBoxClient.registerAction("my_shop_test") { params, commandId ->
    val shopId = params.get("shopId")?.asString ?: return@registerAction ActionResult.fail("Missing shopId")
    
    // 自定义测试逻辑...
    
    ActionResult.ok("Shop test passed", JsonObject().apply {
        addProperty("shopId", shopId)
        addProperty("result", true)
    })
}

// 注销
BlackBoxClient.unregisterAction("my_shop_test")
```

注册后通过 HTTP 调用，与内置 action 完全一致：

```bash
curl -X POST http://127.0.0.1:25580/api/command \
  -H "Content-Type: application/json" \
  -d '{"action":"my_shop_test","params":{"shopId":"weapon_shop"}}'
```

## ActionResult 返回值

```kotlin
// 成功
ActionResult.ok("描述信息")
ActionResult.ok("描述信息", jsonData)

// 失败
ActionResult.fail("错误原因")

// 异步（action 自行通过 CommandDispatcher.sendResponse 发送响应）
ActionResult.async()
```

## HTTP API

启动后所有 action（内置 + 自定义）通过统一端点调用：

```
POST http://127.0.0.1:25580/api/command
Content-Type: application/json

{
  "action": "action_id",
  "params": { ... },
  "delay": 0
}
```

响应格式：

```json
{
  "id": "uuid",
  "status": "success",
  "message": "...",
  "data": { ... }
}
```

可选查询参数 `?timeout=60` 控制等待超时（秒）。

## 内置 Action 速查

| 分类 | Action ID | 说明 |
|------|-----------|------|
| 世界管理 | `create_world` | 创建单机世界，参数：`name` |
| | `join_world` | 加入已有世界，参数：`levelName` |
| | `leave_world` | 退出到主菜单 |
| 查询 | `query_player_state` | 玩家状态（坐标/血量/游戏模式等） |
| | `query_world_state` | 世界状态（时间/天气/维度等） |
| | `query_block_state` | 方块状态，参数：`x`, `y`, `z` |
| | `query_inventory_slot` | 背包槽位，参数：`slot` |
| | `query_container_state` | 当前容器状态 |
| | `query_nearby_entities` | 附近实体，参数：`radius`, `type`, `limit` |
| | `query_screen_state` | 当前屏幕/GUI |
| 移动 | `player_move` | 移动到坐标，参数：`x`, `y`, `z`, `speed`, `timeout` |
| | `player_look` | 转向，参数：`yaw`, `pitch` |
| | `navigate_to` | 寻路导航，参数：`x`, `y`, `z`, `speed` |
| 交互 | `chat_command` | 执行命令，参数：`command` |
| | `chat_message` | 发送聊天，参数：`message` |
| | `open_inventory` | 打开背包 |
| | `close_container` | 关闭容器 |
| | `click_slot` | 点击容器槽位 |
| | `attack_entity` | 攻击实体，参数：`entityId` |
| 截图 | `screenshot` | 截图，参数：`name` |

完整 action 列表共 86+，详见项目 `ActionRegistry.kt`。

## 完整示例

```kotlin
@Mod("my_test_mod")
object MyTestMod {

    init {
        // Mod 加载时启动 BlackBoxPro 测试环境
        BlackBoxClient.start()

        // 注册自定义测试 action
        BlackBoxClient.registerAction("verify_shop_ui") { params, _ ->
            val shopName = params.get("name")?.asString
                ?: return@registerAction ActionResult.fail("Missing name")
            
            // 验证逻辑...
            ActionResult.ok("Shop UI verified: $shopName")
        }
    }
}
```

外部测试脚本：

```python
import requests

BASE = "http://127.0.0.1:25580/api/command"

def cmd(action, params=None):
    r = requests.post(BASE, json={"action": action, "params": params or {}})
    return r.json()

# 创建世界
cmd("create_world", {"name": "test"})

# 查询玩家状态
state = cmd("query_player_state")
print(state["data"]["x"], state["data"]["y"], state["data"]["z"])

# 调用自定义 action
result = cmd("verify_shop_ui", {"name": "weapon_shop"})
print(result)

# 截图
cmd("screenshot", {"name": "test_result"})

# 退出
cmd("leave_world")
```
