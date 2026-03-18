package com.blackboxpro.neoforge.action.container

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.requireBoolean
import com.blackboxpro.neoforge.util.requireInt
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundContainerSlotStateChangedPacket

class SlotStateChangeAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val slotId = params.requireInt("slotId")
        val windowId = params.requireInt("windowId")
        val newState = params.requireBoolean("newState")

        val networkHandler = Minecraft.getInstance().connection
            ?: return ActionResult.fail("Not connected to server")

        networkHandler.send(ServerboundContainerSlotStateChangedPacket(slotId, windowId, newState))
        return ActionResult.ok("Changed slot $slotId state to $newState in window $windowId")
    }
}
