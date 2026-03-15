package com.blackboxpro.fabric.action.debug

import com.blackboxpro.fabric.action.ActionExecutor
import com.blackboxpro.fabric.action.ActionResult
import com.blackboxpro.fabric.util.requireString
import com.google.gson.JsonObject

class DebugSampleAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val type = params.requireString("type")

        // Stub: Debug sample subscription packet varies across 1.21.x versions.
        return ActionResult.fail("Not implemented: debug sample subscription (stub)")
    }
}
