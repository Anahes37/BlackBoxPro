package com.blackboxpro.forge.action.client

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.config.BlackBoxConfig
import com.blackboxpro.forge.util.ScreenshotHelper
import com.blackboxpro.forge.util.getStringOrNull
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft

/**
 * 截图行为执行器。
 *
 * 捕获当前帧缓冲，按 playerName/testId 目录隔离存储，
 * 自动编号并将元信息回报服务端。
 *
 * Action ID: "screenshot"
 */
class ScreenshotAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val mc = Minecraft.getMinecraft()
        val player = mc.player
            ?: return ActionResult.fail("Player not available")

        // 参数解析
        val playerName = params.getStringOrNull("playerName") ?: player.gameProfile.name
        val testId = params.getStringOrNull("testId") ?: "default"
        val prefix = params.getStringOrNull("prefix")

        // 读取配置
        val screenshotConfig = BlackBoxConfig.current.screenshot

        // 目录构建
        val directory = mc.gameDir.toPath()
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

        // 执行截图
        val result = ScreenshotHelper.capture(directory, fileName)

        // 构建响应
        val data = JsonObject().apply {
            val relativePath = mc.gameDir.toPath().relativize(result.filePath)
            addProperty("filePath", relativePath.toString().replace('\\', '/'))
            addProperty("width", result.width)
            addProperty("height", result.height)
            addProperty("fileSize", result.fileSize)
            addProperty("index", index)
        }

        return ActionResult.ok("Screenshot saved: $fileName.png", data)
    }
}
