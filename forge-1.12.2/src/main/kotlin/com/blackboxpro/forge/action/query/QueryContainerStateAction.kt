package com.blackboxpro.forge.action.query

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft

class QueryContainerStateAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val mc = Minecraft.getMinecraft()
        val player = mc.player
            ?: return ActionResult.fail("Player not available")

        val container = player.openContainer
        val isInventoryOnly = container === player.inventoryContainer

        val data = JsonObject().apply {
            addProperty("open", !isInventoryOnly)
            addProperty("windowId", container.windowId)
            addProperty("slotCount", container.inventorySlots.size)

            // Container class name as type identifier
            addProperty("type", container.javaClass.simpleName)
        }

        return ActionResult.ok("Container state queried", data)
    }
}
