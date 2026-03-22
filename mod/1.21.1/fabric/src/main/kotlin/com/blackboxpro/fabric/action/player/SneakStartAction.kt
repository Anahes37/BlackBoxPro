package com.blackboxpro.fabric.action.player

import com.blackboxpro.fabric.action.ActionExecutor
import com.blackboxpro.fabric.action.ActionResult
import com.google.gson.JsonObject
import net.minecraft.client.MinecraftClient
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket
import net.minecraft.util.PlayerInput

class SneakStartAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val client = MinecraftClient.getInstance()
        val networkHandler = client.networkHandler
            ?: return ActionResult.fail("Not connected to server")

        // 1.21.11: sneak 不再通过 ClientCommandC2SPacket 控制，改用 PlayerInput
        networkHandler.sendPacket(
            PlayerInputC2SPacket(
                PlayerInput(false, false, false, false, false, true, false)
            )
        )
        return ActionResult.ok("Started sneaking")
    }
}
