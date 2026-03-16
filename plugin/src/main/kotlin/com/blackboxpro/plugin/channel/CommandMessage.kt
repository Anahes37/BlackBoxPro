package com.blackboxpro.plugin.channel

import com.google.gson.JsonObject

/**
 * 发送给客户端 Mod 的指令消息。
 *
 * @param id 唯一标识符，用于关联请求与响应
 * @param action 行为 ID，对应客户端 ActionRegistry 中注册的 action
 * @param params 行为参数
 * @param delay 延迟执行（毫秒），0 表示立即执行
 */
data class CommandMessage(
    val id: String,
    val action: String,
    val params: JsonObject = JsonObject(),
    val delay: Long = 0L
)
