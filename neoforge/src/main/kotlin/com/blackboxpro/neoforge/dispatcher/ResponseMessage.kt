package com.blackboxpro.neoforge.dispatcher

import com.google.gson.JsonObject

data class ResponseMessage(
    val id: String,
    val status: String,
    val message: String? = null,
    val data: JsonObject? = null
)
