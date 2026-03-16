package com.blackboxpro.plugin.channel

import com.google.gson.JsonObject

/**
 * 客户端 Mod 返回的响应消息。
 *
 * @param id 对应 CommandMessage 的 id
 * @param status "success" 或 "failure"
 * @param message 可选的描述信息
 * @param data 可选的附加数据
 */
data class ResponseMessage(
    val id: String,
    val status: String,
    val message: String? = null,
    val data: JsonObject? = null
) {
    val isSuccess: Boolean get() = status == "success"
}
