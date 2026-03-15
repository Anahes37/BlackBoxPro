package com.blackboxpro.neoforge.action.advanced

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.requireBoolean
import com.blackboxpro.neoforge.util.requireString
import com.google.gson.JsonObject

class RecipeBookToggleAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val category = params.requireString("category")
        val open = params.requireBoolean("open")
        val filtering = params.requireBoolean("filtering")

        // Stub: RecipeCategoryOptionsC2SPacket 构造在 1.21.x 中需要 enum 映射
        return ActionResult.fail("Not implemented: recipe book toggle (stub)")
    }
}
