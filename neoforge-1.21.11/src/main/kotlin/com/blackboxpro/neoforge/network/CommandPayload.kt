package com.blackboxpro.neoforge.network

import com.blackboxpro.common.protocol.BlackBoxChannels
import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier

data class CommandPayload(val json: String) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<CommandPayload> = TYPE

    companion object {
        val TYPE = CustomPacketPayload.Type<CommandPayload>(
            Identifier.fromNamespaceAndPath(BlackBoxChannels.NAMESPACE, BlackBoxChannels.COMMAND_PATH)
        )

        val STREAM_CODEC: StreamCodec<ByteBuf, CommandPayload> =
            ByteBufCodecs.STRING_UTF8.map(::CommandPayload, CommandPayload::json)
    }
}
