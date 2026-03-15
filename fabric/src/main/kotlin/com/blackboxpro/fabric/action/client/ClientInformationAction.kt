package com.blackboxpro.fabric.action.client

import com.blackboxpro.fabric.action.ActionExecutor
import com.blackboxpro.fabric.action.ActionResult
import com.blackboxpro.fabric.util.getBooleanOrDefault
import com.blackboxpro.fabric.util.getIntOrDefault
import com.blackboxpro.fabric.util.getStringOrNull
import com.google.gson.JsonObject
import net.minecraft.client.MinecraftClient
import net.minecraft.client.option.GameOptions
import net.minecraft.network.packet.c2s.common.SyncedClientOptions

class ClientInformationAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val locale = params.getStringOrNull("locale") ?: "en_us"
        val viewDistance = params.getIntOrDefault("viewDistance", 12)
        val chatMode = params.getIntOrDefault("chatMode", 0)
        val chatColors = params.getBooleanOrDefault("chatColors", true)
        val skinParts = params.getIntOrDefault("skinParts", 127)
        val mainHand = params.getIntOrDefault("mainHand", 1)
        val textFiltering = params.getBooleanOrDefault("textFiltering", false)
        val allowServerListings = params.getBooleanOrDefault("allowServerListings", true)

        val client = MinecraftClient.getInstance()
        val networkHandler = client.networkHandler
            ?: return ActionResult.fail("Not connected to server")

        // 显式映射 mainHand，不依赖 enum ordinal
        val arm = when (mainHand) {
            0 -> net.minecraft.util.Arm.LEFT
            1 -> net.minecraft.util.Arm.RIGHT
            else -> return ActionResult.fail("Invalid mainHand: $mainHand (expected 0=left, 1=right)")
        }

        val syncedOptions = SyncedClientOptions(
            locale,
            viewDistance,
            GameOptions.ChatVisibility.byId(chatMode),
            chatColors,
            skinParts,
            arm,
            textFiltering,
            allowServerListings,
            net.minecraft.client.option.ParticlesMode.ALL
        )
        networkHandler.sendPacket(syncedOptions.toPacket())

        return ActionResult.ok("Client information sent")
    }
}
