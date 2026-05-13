package com.blackboxpro.neoforge.action.player

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.player.Input

class SneakStartAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val client = Minecraft.getInstance()
        val player = client.player
            ?: return ActionResult.fail("Player not available")

        val kp = player.input.keyPresses
        player.input.keyPresses = Input(kp.forward(), kp.backward(), kp.left(), kp.right(), kp.jump(), true, kp.sprint())
        player.setShiftKeyDown(true)
        return ActionResult.ok("Started sneaking")
    }
}
