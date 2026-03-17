package com.blackboxpro.fabric.network

import com.blackboxpro.fabric.dispatcher.CommandDispatcher
import com.blackboxpro.fabric.dispatcher.CommandMessage
import com.google.gson.Gson
import com.google.gson.JsonObject
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.minecraft.client.MinecraftClient
import org.slf4j.LoggerFactory

object NetworkHandler {

    private val logger = LoggerFactory.getLogger("BlackBoxPro-Network")
    private val gson = Gson()

    fun register() {
        PayloadTypeRegistry.playS2C().register(CommandPayload.ID, CommandPayload.CODEC)
        PayloadTypeRegistry.playC2S().register(ResponsePayload.ID, ResponsePayload.CODEC)

        ClientPlayNetworking.registerGlobalReceiver(CommandPayload.ID) { payload, context ->
            val json = payload.json
            logger.debug("Received command: {}", json)

            var extractedId: String? = null
            try {
                val obj = gson.fromJson(json, JsonObject::class.java)
                extractedId = obj.get("id")?.asString
                val action = obj.get("action")?.asString

                if (extractedId == null || action == null) {
                    logger.error("Malformed command (missing id or action): {}", json)
                    if (extractedId != null) {
                        sendResponse(gson.toJson(mapOf("id" to extractedId, "status" to "failure", "message" to "Malformed command: missing action")))
                    }
                    return@registerGlobalReceiver
                }

                val message = CommandMessage(
                    id = extractedId,
                    action = action,
                    params = obj.getAsJsonObject("params") ?: JsonObject(),
                    delay = obj.get("delay")?.asLong ?: 0L
                )
                CommandDispatcher.dispatch(message)
            } catch (e: Exception) {
                logger.error("Failed to parse command: {}", json, e)
                if (extractedId != null) {
                    sendResponse(gson.toJson(mapOf("id" to extractedId, "status" to "failure", "message" to "Parse error: ${e.message}")))
                }
            }
        }

        logger.info("BlackBoxPro network channels registered.")
    }

    fun sendResponse(json: String) {
        val client = MinecraftClient.getInstance()
        if (client.networkHandler != null) {
            ClientPlayNetworking.send(ResponsePayload(json))
            logger.debug("Sent response: {}", json)
        } else {
            logger.warn("Cannot send response, network handler is null.")
        }
    }
}
