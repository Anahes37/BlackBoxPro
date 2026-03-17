package com.blackboxpro.fabric.action.entity

import com.blackboxpro.fabric.action.ActionExecutor
import com.blackboxpro.fabric.action.ActionResult
import com.blackboxpro.fabric.util.getStringOrNull
import com.blackboxpro.fabric.util.HandUtil
import com.google.gson.JsonObject
import net.minecraft.client.MinecraftClient
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket

class SwingArmAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val hand = HandUtil.fromString(params.getStringOrNull("hand") ?: "main_hand")

        val packet = HandSwingC2SPacket(hand)
        MinecraftClient.getInstance().networkHandler?.sendPacket(packet)
            ?: return ActionResult.fail("Network handler is not available")

        return ActionResult.ok("Swung arm with ${hand.name.lowercase()}")
    }
}
