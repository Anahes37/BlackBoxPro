package com.blackboxpro.neoforge.action.debug

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.requireString
import com.google.gson.JsonObject

class DebugSampleAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val type = params.requireString("type")

        // Stub: Debug sample subscription packet varies across 1.21.x versions.
        return ActionResult.fail("Not implemented: debug sample subscription (stub)")
    }
}
