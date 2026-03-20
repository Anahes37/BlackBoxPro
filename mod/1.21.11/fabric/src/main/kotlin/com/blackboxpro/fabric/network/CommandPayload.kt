package com.blackboxpro.fabric.network

import com.blackboxpro.fabric.config.BlackBoxConfig
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload

data class CommandPayload(val json: String) : CustomPayload {

    override fun getId(): CustomPayload.Id<CommandPayload> = ID

    companion object {
        val ID = CustomPayload.Id<CommandPayload>(BlackBoxChannels.COMMAND)

        val CODEC: PacketCodec<PacketByteBuf, CommandPayload> =
            PacketCodec.of(
                { payload, buf -> buf.writeString(payload.json) },
                { buf ->
                    val maxSize = BlackBoxConfig.current.network.maxPayloadSize
                    CommandPayload(buf.readString(maxSize))
                }
            )
    }
}
