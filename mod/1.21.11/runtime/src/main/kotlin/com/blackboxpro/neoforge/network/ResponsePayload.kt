package com.blackboxpro.neoforge.network

import com.blackboxpro.common.protocol.BlackBoxChannels
import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier

data class ResponsePayload(val json: String) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<ResponsePayload> = TYPE

    companion object {
        val TYPE = CustomPacketPayload.Type<ResponsePayload>(
            Identifier.fromNamespaceAndPath(BlackBoxChannels.NAMESPACE, BlackBoxChannels.RESPONSE_PATH)
        )

        val STREAM_CODEC: StreamCodec<ByteBuf, ResponsePayload> =
            ByteBufCodecs.STRING_UTF8.map(::ResponsePayload, ResponsePayload::json)
    }
}
