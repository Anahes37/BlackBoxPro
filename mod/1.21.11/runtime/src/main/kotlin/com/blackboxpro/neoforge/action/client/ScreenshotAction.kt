package com.blackboxpro.neoforge.action.client

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.runtime.config.RuntimeBlackBoxConfig
import com.blackboxpro.runtime.dispatcher.RuntimeResponseSender
import com.blackboxpro.neoforge.util.ScreenshotHelper
import com.blackboxpro.neoforge.util.getStringOrNull
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft

/**
 * 截图行为执行器。
 *
 * 捕获当前帧缓冲，按 playerName/testId 目录隔离存储，
 * 自动编号并将元信息回报服务端。
 *
 * 使用异步模式：截图回调在渲染线程执行完成后自行发送响应，
 * 避免 future.get() 阻塞主线程导致死锁。
 *
 * Action ID: "screenshot"
 */
class ScreenshotAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult =
        ActionResult.fail("ScreenshotAction requires commandId, use execute(params, commandId)")

    override fun execute(params: JsonObject, commandId: String): ActionResult {
        val client = Minecraft.getInstance()
        val player = client.player
            ?: return ActionResult.fail("Player not available")

        // 参数解析
        val playerName = params.getStringOrNull("playerName") ?: player.gameProfile.name
        val testId = params.getStringOrNull("testId") ?: "default"
        val prefix = params.getStringOrNull("prefix")

        // 读取配置
        val screenshotConfig = RuntimeBlackBoxConfig.current.screenshot

        // 目录构建
        val directory = client.gameDirectory.toPath()
            .resolve(screenshotConfig.rootDirectory)
            .resolve(ScreenshotHelper.sanitize(playerName))
            .resolve(ScreenshotHelper.sanitize(testId))

        // 编号计算
        val index = ScreenshotHelper.nextIndex(directory)
        if (index > screenshotConfig.maxPerTest) {
            return ActionResult.fail("Screenshot index overflow (max ${screenshotConfig.maxPerTest}) for testId=$testId")
        }
        val indexStr = index.toString().padStart(3, '0')

        // 文件名
        val fileName = if (prefix != null) {
            "${indexStr}_${ScreenshotHelper.sanitize(prefix)}"
        } else {
            indexStr
        }

        // 异步执行截图，回调中自行发送响应
        ScreenshotHelper.captureAsync(directory, fileName) { result ->
            result.fold(
                onSuccess = { screenshot ->
                    val data = JsonObject().apply {
                        val relativePath = client.gameDirectory.toPath().relativize(screenshot.filePath)
                        addProperty("filePath", relativePath.toString().replace('\\', '/'))
                        addProperty("width", screenshot.width)
                        addProperty("height", screenshot.height)
                        addProperty("fileSize", screenshot.fileSize)
                        addProperty("index", index)
                    }
                    RuntimeResponseSender.sendResponse(commandId, "success", "Screenshot saved: $fileName.png", data)
                },
                onFailure = { e ->
                    RuntimeResponseSender.sendResponse(commandId, "failure", "Screenshot failed: ${e.message}")
                }
            )
        }

        // 返回 async 标记，CommandDispatcher 不再发送响应
        return ActionResult.async()
    }
}
