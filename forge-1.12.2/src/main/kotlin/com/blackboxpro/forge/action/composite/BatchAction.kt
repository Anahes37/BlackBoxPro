package com.blackboxpro.forge.action.composite

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.config.BlackBoxConfig
import com.blackboxpro.forge.dispatcher.ActionRegistry
import com.google.gson.JsonObject
import org.apache.logging.log4j.LogManager

class BatchAction : ActionExecutor {

    private val logger = LogManager.getLogger("BlackBoxPro-Batch")

    override fun execute(params: JsonObject): ActionResult {
        val actionsArray = params.getAsJsonArray("actions")
            ?: return ActionResult.fail("Missing required field: actions")

        val maxBatchSize = BlackBoxConfig.current.execution.maxBatchSize
        if (actionsArray.size() > maxBatchSize) {
            return ActionResult.fail("Batch too large: ${actionsArray.size()} > $maxBatchSize")
        }

        if (actionsArray.size() == 0) {
            return ActionResult.ok("Empty batch, nothing to do")
        }

        executeBatchStep(actionsArray.map { it.asJsonObject }, 0)

        return ActionResult.ok("Batch started (${actionsArray.size()} actions)")
    }

    private fun executeBatchStep(actions: List<JsonObject>, startIndex: Int) {
        var index = startIndex
        while (index < actions.size) {
            val item = actions[index]
            val action = item.get("action")?.asString
            if (action == null) {
                logger.warn("Batch step {} missing 'action' field, skipping", index)
                index++
                continue
            }
            val actionParams = item.getAsJsonObject("params") ?: JsonObject()

            if (action == "batch") {
                logger.warn("Nested batch is not allowed, skipping step {}", index)
                index++
                continue
            }

            if (action == "wait") {
                val maxDelay = BlackBoxConfig.current.execution.maxDelayTicks
                val ticks = (actionParams.get("ticks")?.asInt ?: 0).coerceAtMost(maxDelay)
                TickScheduler.schedule(ticks) {
                    executeBatchStep(actions, index + 1)
                }
                return
            }

            val executor = ActionRegistry.find(action)
            if (executor != null) {
                val result = executor.execute(actionParams)
                if (!result.success) {
                    logger.warn("Batch step {} ({}) failed: {}", index, action, result.message)
                }
            } else {
                logger.warn("Batch step {} unknown action: {}", index, action)
            }

            index++
        }
    }
}
