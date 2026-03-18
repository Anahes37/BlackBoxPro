package com.blackboxpro.neoforge.action.container

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.google.gson.JsonObject

class PickItemFromBlockAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        return ActionResult.fail("This action is not supported on 1.21.1")
    }
}
