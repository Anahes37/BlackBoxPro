package com.blackboxpro.neoforge.action.entity

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.getStringOrNull
import com.blackboxpro.neoforge.util.HandUtil
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundSwingPacket

class SwingArmAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val hand = HandUtil.fromString(params.getStringOrNull("hand") ?: "main_hand")

        val packet = ServerboundSwingPacket(hand)
        Minecraft.getInstance().connection?.send(packet)
            ?: return ActionResult.fail("Network handler is not available")

        return ActionResult.ok("Swung arm with ${hand.name.lowercase()}")
    }
}
