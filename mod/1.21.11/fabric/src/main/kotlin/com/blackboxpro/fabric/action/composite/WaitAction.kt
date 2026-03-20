package com.blackboxpro.fabric.action.composite

import com.blackboxpro.fabric.action.ActionExecutor
import com.blackboxpro.fabric.action.ActionResult
import com.blackboxpro.fabric.config.BlackBoxConfig
import com.blackboxpro.fabric.util.getIntOrDefault
import com.google.gson.JsonObject

class WaitAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val maxDelay = BlackBoxConfig.current.execution.maxDelayTicks
        val ticks = params.getIntOrDefault("ticks", 0).coerceIn(0, maxDelay)
        // WaitAction 作为独立指令时立即返回，真正的延迟语义仅在 batch 上下文中生效
        return ActionResult.ok("Wait $ticks ticks (${ticks * 50}ms). Note: only effective inside batch actions.")
    }
}
