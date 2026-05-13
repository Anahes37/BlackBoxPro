package com.blackboxpro.fabric.action.player

import com.blackboxpro.fabric.action.ActionExecutor
import com.blackboxpro.fabric.action.ActionResult
import com.google.gson.JsonObject
import net.minecraft.client.MinecraftClient
import net.minecraft.util.PlayerInput

class SneakStartAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val client = MinecraftClient.getInstance()
        val player = client.player
            ?: return ActionResult.fail("Player not available")

        val pi = player.input.playerInput
        player.input.playerInput = PlayerInput(
            pi.forward(), pi.backward(), pi.left(), pi.right(),
            pi.jump(), true, pi.sprint()
        )
        player.isSneaking = true
        return ActionResult.ok("Started sneaking")
    }
}
