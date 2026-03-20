package com.blackboxpro.fabric.network

import com.blackboxpro.common.protocol.BlackBoxChannels as ProtocolChannels
import net.minecraft.util.Identifier

object BlackBoxChannels {
    val COMMAND: Identifier = Identifier.of(ProtocolChannels.NAMESPACE, ProtocolChannels.COMMAND_PATH)
    val RESPONSE: Identifier = Identifier.of(ProtocolChannels.NAMESPACE, ProtocolChannels.RESPONSE_PATH)
}
