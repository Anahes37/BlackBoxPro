package com.blackboxpro.fabric.util

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.text.Text
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * 聊天消息环形缓冲区。
 * 通过 Fabric ClientReceiveMessageEvents 捕获消息，供 query_chat_history 查询。
 */
object ChatHistoryBuffer {

    private val logger = LoggerFactory.getLogger("BlackBoxPro-ChatHistory")
    private const val MAX_SIZE = 200

    data class ChatEntry(
        val timestamp: Long,
        val raw: String,
        val plain: String,
        val type: String // CHAT, SYSTEM, ACTION_BAR
    )

    private val buffer = ConcurrentLinkedDeque<ChatEntry>()

    fun addMessage(message: Text, type: String) {
        val raw = message.string
        val plain = message.string
        buffer.addLast(ChatEntry(System.currentTimeMillis(), raw, plain, type))
        while (buffer.size > MAX_SIZE) buffer.pollFirst()
    }

    fun query(count: Int, filter: String?, since: Long?): JsonObject {
        var stream = buffer.toList().asSequence()
        if (since != null) stream = stream.filter { it.timestamp >= since }
        if (filter != null) stream = stream.filter { it.plain.contains(filter) }
        val messages = stream.toList().takeLast(count.coerceIn(1, 100))

        val arr = JsonArray()
        messages.forEach { entry ->
            arr.add(JsonObject().apply {
                addProperty("timestamp", entry.timestamp)
                addProperty("raw", entry.raw)
                addProperty("plain", entry.plain)
                addProperty("type", entry.type)
            })
        }
        return JsonObject().apply {
            add("messages", arr)
            addProperty("total", buffer.size)
        }
    }

    fun clear() = buffer.clear()
}
