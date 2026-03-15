package com.blackboxpro.fabric.action.debug

import com.blackboxpro.fabric.action.ActionExecutor
import com.blackboxpro.fabric.action.ActionResult
import com.blackboxpro.fabric.util.requireString
import com.google.gson.JsonObject
import net.minecraft.client.MinecraftClient
import java.util.Base64

class CustomPayloadAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val channel = params.requireString("channel")
        val dataBase64 = params.requireString("data")

        val client = MinecraftClient.getInstance()
        client.networkHandler
            ?: return ActionResult.fail("Not connected to server")

        try {
            Base64.getDecoder().decode(dataBase64)
        } catch (e: IllegalArgumentException) {
            return ActionResult.fail("Invalid base64 data: ${e.message}")
        }

        // 1.21.x 的 CustomPayload 需要预注册 payload type，无法动态发送任意 channel
        return ActionResult.fail("Not implemented: custom payload sending requires pre-registered payload types in 1.21.x")
    }
}
