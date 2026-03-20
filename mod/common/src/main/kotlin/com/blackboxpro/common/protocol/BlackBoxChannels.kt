package com.blackboxpro.common.protocol

object BlackBoxChannels {
    const val NAMESPACE = "blackbox"
    const val COMMAND_PATH = "command"
    const val RESPONSE_PATH = "response"

    const val COMMAND = "$NAMESPACE:$COMMAND_PATH"
    const val RESPONSE = "$NAMESPACE:$RESPONSE_PATH"
}
