package com.blackboxpro.forge.network

import com.blackboxpro.forge.config.BlackBoxConfig
import com.blackboxpro.forge.dispatcher.CommandDispatcher
import com.blackboxpro.forge.dispatcher.CommandMessage
import com.google.gson.Gson
import com.google.gson.JsonObject
import io.netty.buffer.Unpooled
import net.minecraft.client.Minecraft
import net.minecraft.network.PacketBuffer
import net.minecraft.network.play.client.CPacketCustomPayload
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.network.FMLEventChannel
import net.minecraftforge.fml.common.network.FMLNetworkEvent
import net.minecraftforge.fml.common.network.NetworkRegistry

import org.apache.logging.log4j.LogManager

object NetworkHandler {

    private val logger = LogManager.getLogger("BlackBoxPro-Network")
    private val gson = Gson()

    private lateinit var commandChannel: FMLEventChannel
    private lateinit var responseChannel: FMLEventChannel

    fun register() {
        commandChannel = NetworkRegistry.INSTANCE.newEventDrivenChannel(BlackBoxChannels.COMMAND)
        commandChannel.register(this)

        responseChannel = NetworkRegistry.INSTANCE.newEventDrivenChannel(BlackBoxChannels.RESPONSE)

        logger.info("BlackBoxPro network channels registered.")
    }

    @SubscribeEvent
    fun onClientCustomPacket(event: FMLNetworkEvent.ClientCustomPacketEvent) {
        val packet = event.packet
        if (packet.channel() != BlackBoxChannels.COMMAND) return

        val buf = PacketBuffer(packet.payload())
        val maxSize = BlackBoxConfig.current.network.maxPayloadSize
        // readString 内部已包含 VarInt 长度前缀解码，与服务端 encodeString 格式一致
        val json: String = buf.readString(maxSize)
        logger.debug("Received command: {}", json as Any)

        var extractedId: String? = null
        try {
            val obj = gson.fromJson(json, JsonObject::class.java) as JsonObject
            extractedId = obj.get("id")?.asString
            val action = obj.get("action")?.asString

            if (extractedId == null || action == null) {
                logger.error("Malformed command (missing id or action): {}", json as Any)
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
            logger.error("Failed to parse command: {}", json as Any, e)
            if (extractedId != null) {
                sendResponse(gson.toJson(mapOf("id" to extractedId, "status" to "failure", "message" to "Parse error: ${e.message}")))
            }
        }
    }

    fun sendResponse(json: String) {
        val mc = Minecraft.getMinecraft()
        val connection = mc.connection
        if (connection != null) {
            val buf = PacketBuffer(Unpooled.buffer())
            // writeString 内部已包含 VarInt 长度前缀编码，与服务端 decodeString 格式一致
            buf.writeString(json)
            connection.sendPacket(CPacketCustomPayload(BlackBoxChannels.RESPONSE, buf))
            logger.debug("Sent response: {}", json as Any)
        } else {
            logger.warn("Cannot send response, connection is null.")
        }
    }
}
