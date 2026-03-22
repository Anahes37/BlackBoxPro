# 注册第三方测试 Action

新增一个自定义 Action 需要修改 4 个位置。以下以添加 `my_custom_action` 为例。

## 标准流程

### 1. common — ActionCatalog 注册元数据

文件：`common/src/main/kotlin/com/blackboxpro/common/action/ActionCatalog.kt`

在 `init` 块对应分类位置添加：

```kotlin
// Custom
register("my_custom_action", "param1", "param2", "optionalParam")
```

- 第一个参数是 action ID（全局唯一，snake_case）
- 后续参数是该 action 接受的参数名（顺序即 Tab 补全顺序）
- 无参数的 action 只写 `register("my_custom_action")`

> ActionParamRegistry（plugin 模块）直接委托给 ActionCatalog，无需额外注册。

### 2. mod — 实现 ActionExecutor 并注册

#### 2.1 创建 Action 类

文件：`mod/{version}/{loader}/src/main/kotlin/com/blackboxpro/{loader}/action/{category}/MyCustomAction.kt`

```kotlin
package com.blackboxpro.fabric.action.client  // 按分类选包

import com.blackboxpro.fabric.action.ActionExecutor
import com.blackboxpro.fabric.action.ActionResult
import com.google.gson.JsonObject

class MyCustomAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        // 读取参数
        val param1 = params.get("param1")?.asString
            ?: return ActionResult.fail("Missing required field: param1")

        // 执行逻辑
        // ...

        // 返回结果
        return ActionResult.ok(
            message = "Custom action completed",
            data = JsonObject().apply {
                addProperty("result", "some_value")
            }
        )
    }
}
```

**关键接口**：

```kotlin
interface ActionExecutor {
    fun execute(params: JsonObject): ActionResult
    // 异步 action 重写此方法，返回 ActionResult.async()
    fun execute(params: JsonObject, commandId: String): ActionResult = execute(params)
}

data class ActionResult(
    val success: Boolean,
    val message: String? = null,
    val data: JsonObject? = null,
    val async: Boolean = false
) {
    companion object {
        fun ok(message: String? = null, data: JsonObject? = null): ActionResult
        fun fail(message: String): ActionResult
        fun async(): ActionResult  // 异步 action 自行通过 RuntimeResponseSender 发送响应
    }
}
```

**参数读取工具**（`com.blackboxpro.{loader}.util.JsonUtil`）：

```kotlin
params.requireString("key")           // 必填 String，缺失抛异常
params.getStringOrNull("key")         // 可选 String
params.getIntOrDefault("key", 0)      // 可选 Int，带默认值
params.getBooleanOrDefault("key", false)
params.getDoubleOrDefault("key", 0.0)
```

#### 2.2 注册到 ActionRegistry

文件：`mod/{version}/{loader}/src/.../dispatcher/ActionRegistry.kt`

在 `registerAll()` 中添加：

```kotlin
// === 自定义 Action ===
register("my_custom_action", MyCustomAction())
```

#### 2.3 三端同步

需要在所有目标平台注册：

| 平台 | ActionRegistry 路径 |
|------|---------------------|
| 1.21.11 Fabric | `mod/1.21.11/fabric/.../dispatcher/ActionRegistry.kt` |
| 1.21.11 NeoForge | `mod/1.21.11/neoforge/.../dispatcher/ActionRegistry.kt` 或 `runtime/` |
| 1.12.2 Forge | `mod/1.12.2/forge/.../dispatcher/ActionRegistry.kt` |

> 1.12.2 API 不同时需适配（参见 CLAUDE.md 三端映射差异表）。
> 若 1.12.2 不支持该 action，跳过注册并在步骤 3 加入豁免列表。

### 3. plugin — BlackBoxTestCatalog 配置测试

文件：`plugin/src/.../command/testframework/BlackBoxTestCatalog.kt`

根据 action 特征配置：

**a) 若 1.12.2 不支持**：加入 `unsupportedOn1122`

```kotlin
private val unsupportedOn1122 = setOf(
    // ...existing...
    "my_custom_action",
)
```

**b) 若需要前置夹具**：加入 `pendingFixtureActions` 并在 `prepareFixture()` 添加分支

```kotlin
private val pendingFixtureActions = setOf(
    // ...existing...
    "my_custom_action",
)

// 在 prepareFixture() 的 when 中：
"my_custom_action" ->
    CompletableFuture.completedFuture(BlackBoxPrepareResult("需要特定前置条件"))
```

**c) 若为长耗时 action**：加入 `longRunningActions`

```kotlin
private val longRunningActions = setOf(
    // ...existing...
    "my_custom_action",
)
```

**d) 添加默认测试参数**：在 `defaultParams()` 的 `when` 中

```kotlin
"my_custom_action" -> JsonObject().apply {
    addProperty("param1", "test_value")
    addProperty("param2", 42)
}
```

**e) 添加结果验证**（可选）：在 `verify()` 的 `when` 中

```kotlin
"my_custom_action" -> if (data?.has("result") == true) null else "缺少 result 字段"
```

**f) 分类归属**：在 `categoryOf()` 的 `when` 中

```kotlin
actionId in setOf("my_custom_action", ...) -> "custom"
```

## 快速检查清单

- [ ] `ActionCatalog.kt` — 注册 action ID + 参数名
- [ ] `MyCustomAction.kt` — 实现 `ActionExecutor`
- [ ] `ActionRegistry.kt` — 在 `registerAll()` 中注册（每个目标平台）
- [ ] `BlackBoxTestCatalog.kt` — 配置测试（默认参数 / 夹具 / 豁免 / 验证）
- [ ] 构建通过：`./gradlew mod_buildAll plugin_build --no-daemon`

## 异步 Action 模式

长耗时操作（如世界创建）使用异步模式：

```kotlin
class MyAsyncAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult =
        ActionResult.fail("Requires commandId")

    override fun execute(params: JsonObject, commandId: String): ActionResult {
        // 启动异步操作...
        // 完成后通过 RuntimeResponseSender 回报：
        RuntimeResponseSender.sendResponse(
            id = commandId,
            status = "success",
            message = "Done",
            data = JsonObject()
        )
        return ActionResult.async()  // 告诉 dispatcher 不要自动发响应
    }
}
```

## 调用方式

注册后即可通过 HTTP 调用：

```bash
curl -s --max-time 8 -X POST http://localhost:38081/execute \
  -H "Content-Type: application/json" \
  -d '{"id":"c1","action":"my_custom_action","params":{"param1":"hello","param2":42}}'
```
