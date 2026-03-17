package com.blackboxpro.neoforge.dispatcher

import com.blackboxpro.neoforge.network.NetworkHandler
import com.blackboxpro.neoforge.config.BlackBoxConfig
import com.google.gson.Gson
import net.minecraft.client.Minecraft
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.client.event.ClientTickEvent
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

    fun init() {
        NeoForge.EVENT_BUS.register(this)
    }

    @SubscribeEvent
    fun onClientTick(event: ClientTickEvent.Post) {
        val iterator = delayedQueue.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            entry.remainingTicks--
            if (entry.remainingTicks <= 0) {
                iterator.remove()
                executeOnMainThread(entry.message)
            }
        }
    }

    @SubscribeEvent
    fun onDisconnect(event: ClientPlayerNetworkEvent.LoggingOut) {
        val count = delayedQueue.size
        delayedQueue.clear()
        if (count > 0) {
            logger.info("Cleared {} delayed commands on disconnect", count)
        }
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
            executeOnMainThread(message)
        }
    }

    private fun executeOnMainThread(message: CommandMessage) {
        Minecraft.getInstance().execute {
            val executor = ActionRegistry.find(message.action)
            if (executor == null) {
                logger.warn("Unknown action: {}", message.action)
                sendResponse(message.id, "failure", "Unknown action: ${message.action}")
                return@execute
            }

            try {
                val result = executor.execute(message.params)
                sendResponse(
                    id = message.id,
                    status = if (result.success) "success" else "failure",
                    message = result.message,
                    data = result.data
                )
            } catch (e: IllegalArgumentException) {
                logger.warn("Invalid params for action {}: {}", message.action, e.message)
                sendResponse(message.id, "failure", "Invalid params: ${e.message}")
            } catch (e: Exception) {
                logger.error("Action {} threw exception", message.action, e)
                sendResponse(message.id, "failure", "Exception: ${e.message}")
            }
        }
    }

    private fun isActionAllowed(actionId: String): Boolean {
        val config = BlackBoxConfig.current.safety
        if (!config.enabled) return true
        if (actionId in config.blockedActions) return false
        if (config.allowedActions.isEmpty()) return true
        return actionId in config.allowedActions
    }

    private fun sendResponse(
        id: String,
        status: String,
        message: String? = null,
        data: com.google.gson.JsonObject? = null
    ) {
        val response = ResponseMessage(id, status, message, data)
        val json = gson.toJson(response)
        NetworkHandler.sendResponse(json)
    }
}
