package com.blackboxpro.fabric.dispatcher

import com.google.gson.JsonObject

data class CommandMessage(
    val id: String,
    val action: String,
    val params: JsonObject,
    val delay: Long = 0L
)
