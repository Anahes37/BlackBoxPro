package com.blackboxpro.fabric.action.advanced

import com.blackboxpro.fabric.action.ActionExecutor
import com.blackboxpro.fabric.action.ActionResult
import com.blackboxpro.fabric.util.*
import com.google.gson.JsonObject

class UpdateJigsawBlockAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val x = params.requireInt("x")
        val y = params.requireInt("y")
        val z = params.requireInt("z")
        val name = params.requireString("name")
        val target = params.requireString("target")
        val pool = params.requireString("pool")
        val finalState = params.getStringOrNull("finalState") ?: ""
        val jointType = params.getStringOrNull("jointType") ?: "rollable"
        val placementPriority = params.getIntOrDefault("placementPriority", 0)
        val selectionPriority = params.getIntOrDefault("selectionPriority", 0)

        // Stub: UpdateJigsawC2SPacket 构造在 1.21.x 中参数复杂
        return ActionResult.fail(
            "Not implemented: jigsaw block update at ($x, $y, $z) (stub)"
        )
    }
}
