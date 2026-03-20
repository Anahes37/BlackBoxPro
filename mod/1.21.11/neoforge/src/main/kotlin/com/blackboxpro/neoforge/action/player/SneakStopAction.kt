package com.blackboxpro.neoforge.action.player

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket
import net.minecraft.world.entity.player.Input

class SneakStopAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val client = Minecraft.getInstance()
        val networkHandler = client.connection
            ?: return ActionResult.fail("Not connected to server")

        // 1.21.11: sneak 不再通过 PlayerCommand 控制，改用 PlayerInput
        networkHandler.send(
            ServerboundPlayerInputPacket(Input.EMPTY)
        )
        return ActionResult.ok("Stopped sneaking")
    }
}
