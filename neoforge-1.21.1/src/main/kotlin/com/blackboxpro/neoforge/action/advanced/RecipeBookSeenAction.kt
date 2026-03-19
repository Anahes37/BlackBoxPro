package com.blackboxpro.neoforge.action.advanced

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.google.gson.JsonObject

class RecipeBookSeenAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        return ActionResult.fail("recipe_book_seen is not supported on 1.21.1 (requires RecipeHolder)")
    }
}
