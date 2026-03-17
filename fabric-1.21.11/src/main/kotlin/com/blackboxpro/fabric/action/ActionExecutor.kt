package com.blackboxpro.fabric.action

import com.google.gson.JsonObject

interface ActionExecutor {
    fun execute(params: JsonObject): ActionResult
}

data class ActionResult(
    val success: Boolean,
    val message: String? = null,
    val data: JsonObject? = null
) {
    companion object {
        fun ok(message: String? = null, data: JsonObject? = null) =
            ActionResult(true, message, data)

        fun fail(message: String) =
            ActionResult(false, message)
    }
}
