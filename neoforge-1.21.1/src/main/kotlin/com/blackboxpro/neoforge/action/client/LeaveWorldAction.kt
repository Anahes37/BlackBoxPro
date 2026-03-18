package com.blackboxpro.neoforge.action.client

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.dispatcher.CommandDispatcher
import com.blackboxpro.neoforge.util.getIntOrDefault
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.GenericMessageScreen
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen
import net.minecraft.network.chat.Component
import com.mojang.realmsclient.RealmsMainScreen
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.client.event.RenderFrameEvent
import net.neoforged.neoforge.common.NeoForge
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * 离开当前世界，回到主菜单。
 *
 * Action ID: "leave_world"
 * 参数:
 *   - timeout: Int（可选，默认 600 ticks = 30s）— 等待返回主菜单的超时
 *
 * disconnect 通过 CommandDispatcher.pendingDisconnect 在下一帧的
 * RenderFrameEvent.Pre 开头执行（在 menuQueue 消费之前），
 * 执行后 return 跳过当前帧的命令处理，避免重入问题。
 *
 * 这里对齐原版 PauseScreen.onDisconnect()：
 * 1. 先让 ClientLevel 自己断开
 * 2. 再走 Minecraft.disconnect(...)
 * 3. 最后切回 Title / Multiplayer / Realms
 *
 * 如果跳过 level.disconnect()，单人世界第一次退出很容易卡在
 * disconnect() 的 waitForServer 循环里，表现为黑屏或仍停留在世界内。
 */
class LeaveWorldAction : ActionExecutor {

    internal data class PendingLeave(
        val commandId: String,
        val deadline: Long,
        var started: Boolean = false
    )

    companion object {
        private const val ACTION_ID = "leave_world"
        internal val pendingLeaves = ConcurrentHashMap<String, PendingLeave>()
        private val logger = LoggerFactory.getLogger("BlackBoxPro-LeaveWorld")

        /** disconnect 是否正在进行中（供 JoinWorldAction 检查） */
        @Volatile
        var disconnecting = false

        fun init() {
            NeoForge.EVENT_BUS.register(LeaveWorldEventListener)
        }
    }

    private object LeaveWorldEventListener {
        @SubscribeEvent
        fun onRenderFrame(event: RenderFrameEvent.Pre) {
            val client = Minecraft.getInstance()
            val now = System.currentTimeMillis()
            val iterator = pendingLeaves.entries.iterator()
            while (iterator.hasNext()) {
                val (_, pending) = iterator.next()

                if (now >= pending.deadline) {
                    if (!pendingLeaves.remove(pending.commandId, pending)) {
                        continue
                    }
                    WorldActionCoordinator.finish(ACTION_ID, pending.commandId)
                    logger.warn("Leave world timed out")
                    CommandDispatcher.sendResponse(
                        pending.commandId,
                        "failure",
                        "Timed out waiting to leave world"
                    )
                    continue
                }

                if (!pending.started) {
                    continue
                }

                if (!hasReachedMenu(client)) {
                    continue
                }

                if (!pendingLeaves.remove(pending.commandId, pending)) {
                    continue
                }
                WorldActionCoordinator.finish(ACTION_ID, pending.commandId)
                logger.info("Leave world completed")
                CommandDispatcher.sendResponse(
                    pending.commandId,
                    "success",
                    "Left world and returned to menu"
                )
            }
        }

        private fun hasReachedMenu(client: Minecraft): Boolean {
            if (client.level != null) {
                return false
            }
            return when (client.screen) {
                is TitleScreen,
                is JoinMultiplayerScreen,
                is RealmsMainScreen -> true
                else -> false
            }
        }
    }

    override fun execute(params: JsonObject): ActionResult =
        ActionResult.fail("LeaveWorldAction requires commandId")

    override fun execute(params: JsonObject, commandId: String): ActionResult {
        val client = Minecraft.getInstance()
        val timeout = params.getIntOrDefault("timeout", 600)

        if (client.level == null) {
            return ActionResult.fail("Not in a world")
        }
        val beginError = WorldActionCoordinator.tryBegin(ACTION_ID, commandId)
        if (beginError != null) {
            return ActionResult.fail(beginError)
        }

        logger.info("Leaving world")

        val pending = PendingLeave(
            commandId = commandId,
            deadline = System.currentTimeMillis() + timeout * 50L
        )
        pendingLeaves[commandId] = pending
        disconnecting = true

        // 注册 disconnect 到下一帧执行，使用官方的 disconnect(screen) 链路
        CommandDispatcher.pendingDisconnect = {
            pending.started = true
            logger.info("Executing disconnect")
            try {
                val isLocalServer = client.isLocalServer
                val currentServer = client.currentServer

                client.level?.disconnect()

                if (isLocalServer) {
                    client.disconnect(GenericMessageScreen(Component.translatable("menu.savingLevel")))
                } else {
                    client.disconnect()
                }

                val titleScreen = TitleScreen()
                when {
                    isLocalServer -> client.setScreen(titleScreen)
                    currentServer?.isRealm == true -> client.setScreen(RealmsMainScreen(titleScreen))
                    else -> client.setScreen(JoinMultiplayerScreen(titleScreen))
                }
                logger.info("Disconnect completed")
            } catch (e: Exception) {
                pendingLeaves.remove(commandId, pending)
                WorldActionCoordinator.finish(ACTION_ID, commandId)
                logger.error("Disconnect failed", e)
                CommandDispatcher.sendResponse(commandId, "failure", "Failed to leave world: ${e.message}")
            } finally {
                disconnecting = false
            }
        }

        return ActionResult.async()
    }
}
