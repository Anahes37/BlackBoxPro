package com.blackboxpro.neoforge.action.player

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket

class SneakStopAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val handler = Minecraft.getInstance().connection
            ?: return ActionResult.fail("Not connected to server")
        handler.send(ServerboundPlayerInputPacket(0f, 0f, false, false))
        return ActionResult.ok("Sneak stopped")
    }
}
