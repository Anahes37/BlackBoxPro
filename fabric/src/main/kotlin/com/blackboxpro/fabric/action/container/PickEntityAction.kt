package com.blackboxpro.fabric.action.container

import com.blackboxpro.fabric.action.ActionExecutor
import com.blackboxpro.fabric.action.ActionResult
import com.blackboxpro.fabric.util.getBooleanOrDefault
import com.blackboxpro.fabric.util.requireInt
import com.google.gson.JsonObject
import net.minecraft.client.MinecraftClient

class PickEntityAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val entityId = params.requireInt("entityId")
        val includeData = params.getBooleanOrDefault("includeData", false)

        MinecraftClient.getInstance().networkHandler
            ?: return ActionResult.fail("Not connected to server")

        // 1.21.x 没有独立的实体 pick 包，需要通过 interactionManager 实现
        return ActionResult.fail("Not implemented: entity pick requires interactionManager integration")
    }
}
