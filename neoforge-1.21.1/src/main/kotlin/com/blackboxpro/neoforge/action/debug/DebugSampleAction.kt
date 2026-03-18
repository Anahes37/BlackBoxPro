package com.blackboxpro.neoforge.action.debug

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.google.gson.JsonObject

class DebugSampleAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        return ActionResult.fail("Debug sample subscription is not supported on 1.21.1")
    }
}
