package com.blackboxpro.runtime.dispatcher

import com.blackboxpro.common.protocol.CommandMessage
import com.blackboxpro.runtime.action.ActionExecutor
import com.blackboxpro.runtime.config.RuntimeBlackBoxConfig
import com.google.gson.JsonObject
import org.slf4j.Logger

object RuntimeCommandDispatcher {

    interface MainThreadExecutor {
        fun execute(task: () -> Unit)
    }

    interface ActionResolver {
        fun find(actionId: String): ActionExecutor?
    }

    private data class DelayedCommand(
        val message: CommandMessage,
        var remainingTicks: Int
    )

    private lateinit var logger: Logger
    private lateinit var mainThreadExecutor: MainThreadExecutor
    private lateinit var actionResolver: ActionResolver
    private val delayedQueue = ArrayDeque<DelayedCommand>()

    fun bind(
        logger: Logger,
        mainThreadExecutor: MainThreadExecutor,
        actionResolver: ActionResolver
    ) {
        this.logger = logger
        this.mainThreadExecutor = mainThreadExecutor
        this.actionResolver = actionResolver
    }

    fun tick() {
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

    fun clear(): Int {
        val count = delayedQueue.size
        delayedQueue.clear()
        return count
    }

    fun dispatch(message: CommandMessage) {
        logger.info("Dispatching action: {} (id={})", message.action, message.id)

        if (!isActionAllowed(message.action)) {
            RuntimeResponseSender.sendResponse(message.id, "failure", "Action blocked by safety config: ${message.action}")
            return
        }

        if (message.delay > 0) {
            val maxDelay = RuntimeBlackBoxConfig.current.execution.maxDelayTicks
            val ticks = (message.delay / 50).toInt().coerceAtLeast(1).coerceAtMost(maxDelay)
            logger.debug("Delaying action {} for {} ticks", message.action, ticks)
            mainThreadExecutor.execute {
                delayedQueue.addLast(DelayedCommand(message, ticks))
            }
        } else {
            executeOnMainThread(message)
        }
    }

    fun sendResponse(
        id: String,
        status: String,
        message: String? = null,
        data: JsonObject? = null
    ) {
        RuntimeResponseSender.sendResponse(id, status, message, data)
    }

    fun resolveAction(actionId: String): ActionExecutor? =
        if (::actionResolver.isInitialized) actionResolver.find(actionId) else null

    private fun executeOnMainThread(message: CommandMessage) {
        mainThreadExecutor.execute task@ {
            val executor = actionResolver.find(message.action)
            if (executor == null) {
                logger.warn("Unknown action: {}", message.action)
                RuntimeResponseSender.sendResponse(message.id, "failure", "Unknown action: ${message.action}")
                return@task
            }

            try {
                val result = executor.execute(message.params, message.id)
                if (!result.async) {
                    RuntimeResponseSender.sendResponse(
                        id = message.id,
                        status = if (result.success) "success" else "failure",
                        message = result.message,
                        data = result.data
                    )
                }
            } catch (e: IllegalArgumentException) {
                logger.warn("Invalid params for action {}: {}", message.action, e.message)
                RuntimeResponseSender.sendResponse(message.id, "failure", "Invalid params: ${e.message}")
            } catch (e: Exception) {
                logger.error("Action {} threw exception", message.action, e)
                RuntimeResponseSender.sendResponse(message.id, "failure", "Exception: ${e.message}")
            }
        }
    }

    private fun isActionAllowed(actionId: String): Boolean {
        val config = RuntimeBlackBoxConfig.current.safety
        if (!config.enabled) return true
        if (actionId in config.blockedActions) return false
        if (config.allowedActions.isEmpty()) return true
        return actionId in config.allowedActions
    }
}
