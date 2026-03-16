package com.blackboxpro.neoforge.network

import com.blackboxpro.neoforge.dispatcher.CommandDispatcher
import com.blackboxpro.neoforge.dispatcher.CommandMessage
import com.google.gson.Gson
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
import net.neoforged.neoforge.network.handling.IPayloadContext
import net.neoforged.neoforge.client.network.ClientPacketDistributor
import org.slf4j.LoggerFactory

object NetworkHandler {

    private val logger = LoggerFactory.getLogger("BlackBoxPro-Network")
    private val gson = Gson()

    fun register(modBus: IEventBus) {
        modBus.addListener(::onRegisterPayloadHandlers)
        logger.info("BlackBoxPro network channels registered.")
    }

    private fun onRegisterPayloadHandlers(event: RegisterPayloadHandlersEvent) {
        val registrar = event.registrar("blackbox")

        registrar.playToClient(
            CommandPayload.TYPE,
            CommandPayload.STREAM_CODEC
        ) { payload, context -> handleCommand(payload, context) }

        registrar.playToServer(
            ResponsePayload.TYPE,
            ResponsePayload.STREAM_CODEC
        ) { _, _ -> /* 服务端处理，客户端无需 handler */ }
    }

    private fun handleCommand(payload: CommandPayload, context: IPayloadContext) {
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
                return
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

    fun sendResponse(json: String) {
        val client = Minecraft.getInstance()
        if (client.connection != null) {
            ClientPacketDistributor.sendToServer(ResponsePayload(json))
            logger.debug("Sent response: {}", json)
        } else {
            logger.warn("Cannot send response, network handler is null.")
        }
    }
}
