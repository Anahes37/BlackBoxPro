package com.blackboxpro.forge.action.client

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.getBooleanOrDefault
import com.blackboxpro.forge.util.getIntOrDefault
import com.blackboxpro.forge.util.getStringOrNull
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer.EnumChatVisibility
import net.minecraft.network.play.client.CPacketClientSettings
import net.minecraft.util.EnumHandSide

class ClientInformationAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val lang = params.getStringOrNull("lang") ?: "en_us"
        val viewDistance = params.getIntOrDefault("viewDistance", 12)
        val chatModeStr = params.getStringOrNull("chatMode") ?: "full"
        val chatColors = params.getBooleanOrDefault("chatColors", true)
        val skinParts = params.getIntOrDefault("skinParts", 127)
        val mainHandStr = params.getStringOrNull("mainHand") ?: "right"

        val chatVisibility = when (chatModeStr.lowercase()) {
            "full" -> EnumChatVisibility.FULL
            "commands", "system" -> EnumChatVisibility.SYSTEM
            "hidden" -> EnumChatVisibility.HIDDEN
            else -> return ActionResult.fail("Invalid chatMode: $chatModeStr. Valid: full, commands, hidden")
        }

        val mainHand = when (mainHandStr.lowercase()) {
            "left" -> EnumHandSide.LEFT
            "right" -> EnumHandSide.RIGHT
            else -> return ActionResult.fail("Invalid mainHand: $mainHandStr. Valid: left, right")
        }

        val connection = Minecraft.getMinecraft().connection
            ?: return ActionResult.fail("Not connected to server")

        connection.sendPacket(
            CPacketClientSettings(lang, viewDistance, chatVisibility, chatColors, skinParts, mainHand)
        )
        return ActionResult.ok("Client information sent")
    }
}
