package com.blackboxpro.neoforge.action.movement

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.getBooleanOrDefault
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket

class PlayerInputAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val forward = params.getBooleanOrDefault("forward", false)
        val backward = params.getBooleanOrDefault("backward", false)
        val left = params.getBooleanOrDefault("left", false)
        val right = params.getBooleanOrDefault("right", false)
        val jump = params.getBooleanOrDefault("jump", false)
        val sneak = params.getBooleanOrDefault("sneak", false)

        val handler = Minecraft.getInstance().connection
            ?: return ActionResult.fail("Not connected to server")

        val sideways = (if (left) 1.0f else 0.0f) - (if (right) 1.0f else 0.0f)
        val forwardImpulse = (if (forward) 1.0f else 0.0f) - (if (backward) 1.0f else 0.0f)

        handler.send(ServerboundPlayerInputPacket(sideways, forwardImpulse, jump, sneak))
        return ActionResult.ok("Player input sent")
    }
}
