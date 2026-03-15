package com.blackboxpro.fabric.action.advanced

import com.blackboxpro.fabric.action.ActionExecutor
import com.blackboxpro.fabric.action.ActionResult
import com.blackboxpro.fabric.util.*
import com.google.gson.JsonObject

class UpdateStructureBlockAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val x = params.requireInt("x")
        val y = params.requireInt("y")
        val z = params.requireInt("z")
        val action = params.requireInt("action")
        val mode = params.requireString("mode")
        val name = params.requireString("name")
        val offsetX = params.getIntOrDefault("offsetX", 0)
        val offsetY = params.getIntOrDefault("offsetY", 0)
        val offsetZ = params.getIntOrDefault("offsetZ", 0)
        val sizeX = params.getIntOrDefault("sizeX", 0)
        val sizeY = params.getIntOrDefault("sizeY", 0)
        val sizeZ = params.getIntOrDefault("sizeZ", 0)
        val mirror = params.getStringOrNull("mirror") ?: "NONE"
        val rotation = params.getStringOrNull("rotation") ?: "NONE"
        val metadata = params.getStringOrNull("metadata") ?: ""
        val integrity = params.getFloatOrDefault("integrity", 1.0f)
        val seed = params.getLongOrDefault("seed", 0L)
        val flags = params.getIntOrDefault("flags", 0)

        // Stub: UpdateStructureBlockC2SPacket 构造在 1.21.x 中参数复杂
        return ActionResult.fail(
            "Not implemented: structure block update at ($x, $y, $z) (stub)"
        )
    }
}
