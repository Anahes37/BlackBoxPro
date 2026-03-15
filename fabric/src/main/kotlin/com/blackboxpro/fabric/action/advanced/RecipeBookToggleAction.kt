package com.blackboxpro.fabric.action.advanced

import com.blackboxpro.fabric.action.ActionExecutor
import com.blackboxpro.fabric.action.ActionResult
import com.blackboxpro.fabric.util.requireBoolean
import com.blackboxpro.fabric.util.requireString
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
