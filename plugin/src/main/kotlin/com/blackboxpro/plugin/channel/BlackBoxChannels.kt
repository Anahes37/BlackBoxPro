package com.blackboxpro.plugin.channel

import com.blackboxpro.common.protocol.BlackBoxChannels as ProtocolChannels

/**
 * Plugin Message Channel 标识符，与 Fabric/NeoForge 客户端 Mod 对齐。
 *
 * 协议方向：
 * - COMMAND: 服务端 → 客户端 (S2C)
 * - RESPONSE: 客户端 → 服务端 (C2S)
 */
object BlackBoxChannels {
    const val COMMAND = ProtocolChannels.COMMAND
    const val RESPONSE = ProtocolChannels.RESPONSE
}
