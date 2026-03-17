package com.blackboxpro.fabric.network

import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload

data class ResponsePayload(val json: String) : CustomPayload {

    override fun getId(): CustomPayload.Id<ResponsePayload> = ID

    companion object {
        val ID = CustomPayload.Id<ResponsePayload>(BlackBoxChannels.RESPONSE)

        val CODEC: PacketCodec<PacketByteBuf, ResponsePayload> =
            PacketCodec.of(
                { payload, buf -> buf.writeString(payload.json) },
                { buf -> ResponsePayload(buf.readString()) }
            )
    }
}
