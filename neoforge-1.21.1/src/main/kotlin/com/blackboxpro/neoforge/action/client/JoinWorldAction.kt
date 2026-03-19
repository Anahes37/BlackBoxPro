package com.blackboxpro.neoforge.action.client

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.dispatcher.CommandDispatcher
import com.blackboxpro.neoforge.util.getIntOrDefault
import com.blackboxpro.neoforge.util.requireString
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent
import net.neoforged.neoforge.client.event.RenderFrameEvent
import net.neoforged.neoforge.common.NeoForge
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

/**
 * 加入已有的单人世界。
 *
 * Action ID: "join_world"
 * 参数:
 *   - levelName: String（必填）— saves/ 下的世界文件夹名
 *   - timeout: Int（可选，默认 600 ticks = 30s）— 等待加载完成的超时
 */
class JoinWorldAction : ActionExecutor {

    private val logger = LoggerFactory.getLogger("BlackBoxPro-JoinWorld")

    /** 等待世界加载完成的回调 */
    internal data class PendingJoin(
        val commandId: String,
        val levelName: String,
        val deadline: Long,
        var started: Boolean = false
    )

    companion object {
        internal val pendingJoins = ConcurrentHashMap<String, PendingJoin>()
        private val logger = LoggerFactory.getLogger("BlackBoxPro-JoinWorld")
        private const val ACTION_ID = "join_world"

        fun init() {
            NeoForge.EVENT_BUS.register(JoinWorldEventListener)
        }
    }

    private object JoinWorldEventListener {
        @SubscribeEvent
        fun onLoggedIn(event: ClientPlayerNetworkEvent.LoggingIn) {
            // 玩家登入事件，检查是否有等待中的 join_world
            val iterator = pendingJoins.entries.iterator()
            while (iterator.hasNext()) {
                val (_, pending) = iterator.next()
                if (!pendingJoins.remove(pending.commandId, pending)) {
                    continue
                }
                WorldActionCoordinator.finish(ACTION_ID, pending.commandId)
                logger.info("World '{}' loaded (LoggingIn event)", pending.levelName)
                val data = JsonObject().apply {
                    addProperty("levelName", pending.levelName)
                }
                CommandDispatcher.sendResponse(pending.commandId, "success", "Joined world: ${pending.levelName}", data)
            }
        }

        @SubscribeEvent
        fun onRenderFrame(event: RenderFrameEvent.Pre) {
            val client = Minecraft.getInstance()
            val now = System.currentTimeMillis()
            val iterator = pendingJoins.entries.iterator()
            while (iterator.hasNext()) {
                val (_, pending) = iterator.next()
                if (now >= pending.deadline) {
                    if (!pendingJoins.remove(pending.commandId, pending)) {
                        continue
                    }
                    WorldActionCoordinator.finish(ACTION_ID, pending.commandId)
                    logger.warn("Join world '{}' timed out", pending.levelName)
                    CommandDispatcher.sendResponse(
                        pending.commandId,
                        "failure",
                        "Timed out waiting to join world: ${pending.levelName}"
                    )
                    continue
                }
                if (pending.started) {
                    continue
                }
                if (LeaveWorldAction.disconnecting || CommandDispatcher.pendingDisconnect != null || client.level != null || client.screen == null) {
                    continue
                }

                pending.started = true
                tryOpenWorld(client, pending)
            }
        }

        private fun tryOpenWorld(client: Minecraft, pending: PendingJoin) {
            try {
                client.createWorldOpenFlows().openWorld(pending.levelName) {
                    if (!pendingJoins.remove(pending.commandId, pending)) {
                        return@openWorld
                    }
                    WorldActionCoordinator.finish(ACTION_ID, pending.commandId)
                    logger.warn("World '{}' access failed or was cancelled", pending.levelName)
                    CommandDispatcher.sendResponse(
                        pending.commandId,
                        "failure",
                        "Failed to open world: ${pending.levelName}"
                    )
                }
            } catch (e: IllegalStateException) {
                if (e.message?.contains("during disconnection") == true) {
                    pending.started = false
                    logger.info("World '{}' open delayed until disconnect completes", pending.levelName)
                    return
                }
                if (!pendingJoins.remove(pending.commandId, pending)) {
                    return
                }
                WorldActionCoordinator.finish(ACTION_ID, pending.commandId)
                logger.error("Failed to open world: {}", pending.levelName, e)
                CommandDispatcher.sendResponse(pending.commandId, "failure", "Failed to open world: ${e.message}")
            } catch (e: Exception) {
                if (!pendingJoins.remove(pending.commandId, pending)) {
                    return
                }
                WorldActionCoordinator.finish(ACTION_ID, pending.commandId)
                logger.error("Failed to open world: {}", pending.levelName, e)
                CommandDispatcher.sendResponse(pending.commandId, "failure", "Failed to open world: ${e.message}")
            }
        }
    }

    override fun execute(params: JsonObject): ActionResult =
        ActionResult.fail("JoinWorldAction requires commandId")

    override fun execute(params: JsonObject, commandId: String): ActionResult {
        val client = Minecraft.getInstance()
        val levelName = params.requireString("levelName")
        val timeout = params.getIntOrDefault("timeout", 600)

        if (client.level != null) {
            return ActionResult.fail("Already in a world. Use leave_world first.")
        }
        if (levelName.isBlank()) {
            return ActionResult.fail("levelName cannot be blank")
        }

        val validationError = validateLevelName(client, levelName)
        if (validationError != null) {
            return ActionResult.fail(validationError)
        }

        val beginError = WorldActionCoordinator.tryBegin(ACTION_ID, commandId)
        if (beginError != null) {
            return ActionResult.fail(beginError)
        }

        logger.info("Joining world: {}", levelName)

        val deadline = System.currentTimeMillis() + timeout * 50L
        pendingJoins[commandId] = PendingJoin(commandId, levelName, deadline)

        return ActionResult.async()
    }

    private fun validateLevelName(client: Minecraft, levelName: String): String? {
        val savesDir = client.gameDirectory.toPath().resolve("saves").normalize()
        val levelDir = savesDir.resolve(levelName).normalize()

        if (!levelDir.startsWith(savesDir) || levelDir.parent != savesDir) {
            return "Invalid levelName: $levelName"
        }

        if (!Files.isDirectory(levelDir)) {
            return "World not found: $levelName"
        }

        if (!hasLevelData(levelDir)) {
            return "World metadata not found: $levelName"
        }

        return null
    }

    private fun hasLevelData(levelDir: Path): Boolean =
        Files.isRegularFile(levelDir.resolve("level.dat")) ||
            Files.isRegularFile(levelDir.resolve("level.dat_old"))
}
