package com.blackboxpro.fabric.action.block

import com.blackboxpro.fabric.action.ActionExecutor
import com.blackboxpro.fabric.action.ActionResult
import com.blackboxpro.fabric.util.HandUtil
import com.blackboxpro.fabric.util.getIntOrDefault
import com.blackboxpro.fabric.util.getStringOrNull
import com.google.gson.JsonObject
import net.minecraft.client.MinecraftClient
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket

class UseItemAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val hand = HandUtil.fromString(params.getStringOrNull("hand") ?: "main_hand")
        val sequence = params.getIntOrDefault("sequence", 0)

        val player = MinecraftClient.getInstance().networkHandler
            ?: return ActionResult.fail("Not connected to server")

        player.sendPacket(PlayerInteractItemC2SPacket(hand, sequence))
        return ActionResult.ok("Used item")
    }
}
