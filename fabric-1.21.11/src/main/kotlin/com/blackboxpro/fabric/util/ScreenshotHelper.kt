package com.blackboxpro.fabric.util

import net.minecraft.client.MinecraftClient
import net.minecraft.client.util.ScreenshotRecorder
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

/**
 * 截图工具类。
 * 帧缓冲读取必须在渲染线程（主线程）调用。
 */
object ScreenshotHelper {

    private val logger = LoggerFactory.getLogger("BlackBoxPro-Screenshot")
    private val INDEX_PATTERN = Regex("^(\\d{3}).*\\.png$")
    private val SANITIZE_PATTERN = Regex("[^a-zA-Z0-9_\\-.]")

    data class ScreenshotResult(
        val filePath: Path,
        val width: Int,
        val height: Int,
        val fileSize: Long
    )

    /**
     * 捕获当前帧缓冲并保存为 PNG。
     * 必须在主线程调用。takeScreenshot 回调在同一帧内同步执行。
     */
    fun capture(directory: Path, fileName: String): ScreenshotResult {
        val framebuffer = MinecraftClient.getInstance().framebuffer
        val future = CompletableFuture<ScreenshotResult>()

        ScreenshotRecorder.takeScreenshot(framebuffer) { image ->
            try {
                Files.createDirectories(directory)
                val filePath = directory.resolve("$fileName.png")
                image.writeTo(filePath)
                val fileSize = Files.size(filePath)
                val w = image.getWidth()
                val h = image.getHeight()
                logger.info("Screenshot saved: {} ({}x{}, {} bytes)", filePath, w, h, fileSize)
                future.complete(ScreenshotResult(filePath, w, h, fileSize))
            } catch (e: Exception) {
                future.completeExceptionally(e)
            } finally {
                image.close()
            }
        }

        return future.get()
    }

    /**
     * 计算目录下的下一个截图编号。
     */
    fun nextIndex(directory: Path): Int {
        if (!Files.exists(directory)) return 1
        val files = directory.toFile().listFiles() ?: return 1
        return (files.mapNotNull { INDEX_PATTERN.matchEntire(it.name)?.groupValues?.get(1)?.toIntOrNull() }
            .maxOrNull() ?: 0) + 1
    }

    /**
     * 清洗文件名，移除文件系统不允许的字符。
     */
    fun sanitize(name: String): String =
        name.replace(SANITIZE_PATTERN, "_")
}
