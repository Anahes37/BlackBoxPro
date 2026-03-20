package com.blackboxpro.runtime.bindings

import com.blackboxpro.common.runtime.LogHandler
import com.blackboxpro.common.runtime.LoggerSupplier
import com.blackboxpro.common.runtime.screenshot.RuntimeScreenshotBridge
import net.minecraft.client.Minecraft
import org.apache.logging.log4j.LogManager
import java.nio.file.Path

/**
 * Forge 1.12.2 platform bindings for common runtime.
 */
object ForgeBindings : LoggerSupplier {

    private val logger = LogManager.getLogger("BlackBoxPro-ForgeBindings")

    val screenshotProvider = ForgeScreenshotProvider()

    init {
        RuntimeScreenshotBridge.bind(screenshotProvider)
        logger.info("ForgeBindings initialized with screenshot provider")
    }

    override fun getLogger(name: String): LogHandler = Log4jLogHandler(LogManager.getLogger(name))
}

private class Log4jLogHandler(private val logger: org.apache.logging.log4j.Logger) : LogHandler {
    override fun info(message: String, vararg args: Any?) = logger.info(message, *args)
    override fun debug(message: String, vararg args: Any?) = logger.debug(message, *args)
    override fun warn(message: String, vararg args: Any?) = logger.warn(message, *args)
    override fun error(message: String, vararg args: Any?) = logger.error(message, *args)
}

/**
 * Forge 1.12.2 screenshot provider for RuntimeScreenshotBridge.
 */
class ForgeScreenshotProvider : RuntimeScreenshotBridge.Provider {

    override fun currentPlayerName(): String? =
        Minecraft.getMinecraft().session.username

    override fun gameDirectory(): Path? =
        Minecraft.getMinecraft().gameDir.toPath()

    override fun captureAsync(directory: Path, fileName: String, callback: (Result<RuntimeScreenshotBridge.CaptureResult>) -> Unit) {
        try {
            val mc = Minecraft.getMinecraft()
            val screenshot = mc.getScreenshot() ?: run {
                callback(Result.failure(IllegalStateException("Failed to capture screenshot")))
                return
            }

            directory.toFile().mkdirs()
            val file = java.io.File(directory.toFile(), "$fileName.png")

            Thread {
                try {
                    javax.imageio.ImageIO.write(screenshot, "PNG", file)
                    callback(Result.success(
                        RuntimeScreenshotBridge.CaptureResult(
                            filePath = file.toPath(),
                            width = screenshot.width,
                            height = screenshot.height,
                            fileSize = file.length()
                        )
                    ))
                } catch (e: Exception) {
                    callback(Result.failure(e))
                }
            }.start()
        } catch (e: Exception) {
            callback(Result.failure(e))
        }
    }
}
