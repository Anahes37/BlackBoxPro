package com.blackboxpro.forge.action.composite

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.config.BlackBoxConfig
import com.blackboxpro.forge.util.getIntOrDefault
import com.google.gson.JsonObject

class WaitAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val maxDelay = BlackBoxConfig.current.execution.maxDelayTicks
        val ticks = params.getIntOrDefault("ticks", 0).coerceIn(0, maxDelay)
        return ActionResult.ok("Wait $ticks ticks (${ticks * 50}ms). Note: only effective inside batch actions.")
    }
}
