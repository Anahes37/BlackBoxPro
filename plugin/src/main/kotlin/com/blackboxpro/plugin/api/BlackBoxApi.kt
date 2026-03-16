package com.blackboxpro.plugin.api

import com.blackboxpro.plugin.channel.ChannelHandler
import com.blackboxpro.plugin.channel.CommandMessage
import com.blackboxpro.plugin.channel.ResponseMessage
import com.blackboxpro.plugin.config.BlackBoxSettings
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

/**
 * BlackBoxPro 服务端 API 入口。
 *
 * 提供向客户端 Mod 发送指令并接收响应的能力。
 *
 * 三种调用模式：
 * 1. fire-and-forget: [send] 不等待响应
 * 2. 回调模式: [send] 带 callback
 * 3. Future 模式: [sendAsync] 返回 CompletableFuture（推荐）
 */
object BlackBoxApi {

    /**
     * 向玩家发送指令（fire-and-forget，不等待响应）。
     */
    fun send(player: Player, action: String, params: JsonObject = JsonObject(), delay: Long = 0L) {
        val message = CommandMessage(
            id = UUID.randomUUID().toString(),
            action = action,
            params = params,
            delay = delay
        )
        ChannelHandler.send(player, message)
    }

    /**
     * 向玩家发送指令并通过回调接收响应。
     */
    fun send(player: Player, action: String, params: JsonObject = JsonObject(), delay: Long = 0L, callback: Consumer<ResponseMessage>) {
        val message = CommandMessage(
            id = UUID.randomUUID().toString(),
            action = action,
            params = params,
            delay = delay
        )
        ChannelHandler.send(player, message, callback)
    }

    /**
     * 向玩家发送指令并返回 CompletableFuture。
     *
     * @param timeoutMs 超时时间（毫秒），默认读取配置 response-timeout-ms。超时后 future 以 failure 完成。
     */
    fun sendAsync(
        player: Player,
        action: String,
        params: JsonObject = JsonObject(),
        delay: Long = 0L,
        timeoutMs: Long = BlackBoxSettings.responseTimeoutMs
    ): CompletableFuture<ResponseMessage> {
        val future = CompletableFuture<ResponseMessage>()
        val id = UUID.randomUUID().toString()
        val message = CommandMessage(id = id, action = action, params = params, delay = delay)

        ChannelHandler.send(player, message) { response ->
            future.complete(response)
        }

        // 超时处理
        CompletableFuture.delayedExecutor(timeoutMs, TimeUnit.MILLISECONDS).execute {
            if (!future.isDone) {
                ChannelHandler.cancelPending(id)
                future.complete(
                    ResponseMessage(id = id, status = "failure", message = "Response timed out after ${timeoutMs}ms")
                )
            }
        }

        return future
    }

    /**
     * 批量发送多个指令（通过客户端 batch action 顺序执行）。
     */
    fun sendBatch(player: Player, actions: List<Pair<String, JsonObject>>) {
        val batchParams = JsonObject().apply {
            add("actions", JsonArray().apply {
                actions.forEach { (action, params) ->
                    add(JsonObject().apply {
                        addProperty("action", action)
                        add("params", params)
                    })
                }
            })
        }
        send(player, "batch", batchParams)
    }
}
