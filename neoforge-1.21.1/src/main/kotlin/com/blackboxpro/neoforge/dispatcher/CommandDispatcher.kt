package com.blackboxpro.neoforge.dispatcher

import com.blackboxpro.neoforge.network.NetworkHandler
import com.blackboxpro.neoforge.config.BlackBoxConfig
import com.blackboxpro.neoforge.http.ResponseRouter
import com.google.gson.Gson
import net.minecraft.client.Minecraft
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.client.event.RenderFrameEvent
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent
import net.neoforged.neoforge.common.NeoForge
import org.slf4j.LoggerFactory

object CommandDispatcher {

    private val logger = LoggerFactory.getLogger("BlackBoxPro-Dispatcher")
    private val gson = Gson()

    private data class DelayedCommand(
        val message: CommandMessage,
        var remainingTicks: Int
    )

    private val delayedQueue = ArrayDeque<DelayedCommand>()

    /** 主菜单待执行队列，由 RenderFrameEvent 消费 */
    private val menuQueue = ArrayDeque<CommandMessage>()

    fun init() {
        NeoForge.EVENT_BUS.register(this)
    }

    /** 待执行的 disconnect 操作 */
    @Volatile
    internal var pendingDisconnect: (() -> Unit)? = null

    /** 每帧触发，消费命令队列（主菜单没有 ClientTickEvent） */
    @SubscribeEvent
    fun onRenderFrame(event: RenderFrameEvent.Pre) {
        // 先执行 disconnect（如果有）
        pendingDisconnect?.let { task ->
            pendingDisconnect = null
            task()
            return // disconnect 后当前帧不再处理命令
        }
        // 消费命令队列（只处理当前帧的消息，requeue 的在下一帧处理）
        val count = menuQueue.size
        repeat(count) {
            if (menuQueue.isNotEmpty()) {
                val msg = menuQueue.removeFirst()
                doExecute(msg)
            }
        }
    }

    @SubscribeEvent
    fun onClientTick(event: ClientTickEvent.Post) {
        val iterator = delayedQueue.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            entry.remainingTicks--
            if (entry.remainingTicks <= 0) {
                iterator.remove()
                doExecute(entry.message)
            }
        }
    }

    @SubscribeEvent
    fun onDisconnect(event: ClientPlayerNetworkEvent.LoggingOut) {
        val count = delayedQueue.size
        delayedQueue.clear()
        // 重置 disconnect 标志（disconnect 完成后触发此事件）
        com.blackboxpro.neoforge.action.client.LeaveWorldAction.disconnecting = false
        if (count > 0) {
            logger.info("Cleared {} delayed commands on disconnect", count)
        }
    }

    /** 重新放回队列，下一帧再执行 */
    internal fun requeue(message: CommandMessage) {
        menuQueue.addLast(message)
    }

    fun dispatch(message: CommandMessage) {
        logger.info("Dispatching action: {} (id={})", message.action, message.id)

        if (!isActionAllowed(message.action)) {
            sendResponse(message.id, "failure", "Action blocked by safety config: ${message.action}")
            return
        }

        if (message.delay > 0) {
            val maxDelay = BlackBoxConfig.current.execution.maxDelayTicks
            val ticks = (message.delay / 50).toInt().coerceAtLeast(1).coerceAtMost(maxDelay)
            logger.debug("Delaying action {} for {} ticks", message.action, ticks)
            Minecraft.getInstance().execute {
                delayedQueue.addLast(DelayedCommand(message, ticks))
            }
        } else {
            // 统一提交到 menuQueue，由 RenderFrameEvent 在主线程消费
            // 不使用 Minecraft.execute{}，因为 disconnect 等操作会清空其队列
            menuQueue.addLast(message)
        }
    }

    private fun doExecute(message: CommandMessage) {
        logger.info("Executing action: {} (id={}) on thread: {}", message.action, message.id, Thread.currentThread().name)
        val executor = ActionRegistry.find(message.action)
        if (executor == null) {
            logger.warn("Unknown action: {}", message.action)
            sendResponse(message.id, "failure", "Unknown action: ${message.action}")
            return
        }

        try {
            val result = executor.execute(message.params, message.id)
            if (!result.async) {
                sendResponse(
                    id = message.id,
                    status = if (result.success) "success" else "failure",
                    message = result.message,
                    data = result.data
                )
            }
        } catch (e: IllegalArgumentException) {
            logger.warn("Invalid params for action {}: {}", message.action, e.message)
            sendResponse(message.id, "failure", "Invalid params: ${e.message}")
        } catch (e: Exception) {
            logger.error("Action {} threw exception", message.action, e)
            sendResponse(message.id, "failure", "Exception: ${e.message}")
        }
    }

    private fun isActionAllowed(actionId: String): Boolean {
        val config = BlackBoxConfig.current.safety
        if (!config.enabled) return true
        if (actionId in config.blockedActions) return false
        if (config.allowedActions.isEmpty()) return true
        return actionId in config.allowedActions
    }

    /** 供异步 Action 自行发送响应，不要在普通 Action 中调用 */
    internal fun sendResponse(
        id: String,
        status: String,
        message: String? = null,
        data: com.google.gson.JsonObject? = null
    ) {
        val response = ResponseMessage(id, status, message, data)
        val json = gson.toJson(response)
        val routed = ResponseRouter.tryRoute(id, json)
        logger.info("sendResponse: id={}, status={}, routed={}", id, status, routed)
        if (!routed) {
            NetworkHandler.sendResponse(json)
        }
    }
}
