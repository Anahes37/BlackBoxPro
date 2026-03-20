package com.blackboxpro.plugin.channel

import com.blackboxpro.plugin.config.BlackBoxSettings
import com.google.gson.Gson
import org.bukkit.entity.Player
import org.bukkit.plugin.messaging.PluginMessageListener
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.info
import taboolib.common.platform.function.warning
import taboolib.platform.BukkitPlugin
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer

/**
 * 服务端 Plugin Message Channel 管理器。
 *
 * 负责：
 * 1. 注册 blackbox:command / blackbox:response 通道
 * 2. 向指定玩家发送 CommandMessage
 * 3. 接收并分发客户端返回的 ResponseMessage
 */
object ChannelHandler : PluginMessageListener {

    private val gson = Gson()

    /**
     * 等待响应的回调表。key = CommandMessage.id
     */
    private val pendingCallbacks = ConcurrentHashMap<String, Consumer<ResponseMessage>>()

    /**
     * 全局响应监听器（用于不关心特定 id 的场景，如日志、调试）
     */
    private val globalListeners = ConcurrentHashMap.newKeySet<Consumer<ResponseMessage>>()

    // ── 生命周期 ──────────────────────────────────────────

    @Awake(LifeCycle.ENABLE)
    fun enable() {
        val plugin = BukkitPlugin.getInstance()
        plugin.server.messenger.registerOutgoingPluginChannel(plugin, BlackBoxChannels.COMMAND)
        plugin.server.messenger.registerIncomingPluginChannel(plugin, BlackBoxChannels.RESPONSE, this)
        info("[BlackBoxPro] Plugin message channels registered.")
    }

    @Awake(LifeCycle.DISABLE)
    fun disable() {
        val plugin = BukkitPlugin.getInstance()
        plugin.server.messenger.unregisterOutgoingPluginChannel(plugin, BlackBoxChannels.COMMAND)
        plugin.server.messenger.unregisterIncomingPluginChannel(plugin, BlackBoxChannels.RESPONSE, this)
        pendingCallbacks.clear()
        globalListeners.clear()
    }

    // ── 发送 ──────────────────────────────────────────────

    /**
     * 向玩家客户端发送指令。
     *
     * @param player 目标玩家
     * @param message 指令消息
     * @param callback 可选的响应回调，收到对应 id 的响应后触发（仅触发一次）
     * @throws IllegalArgumentException 如果序列化后的载荷超过 maxPayloadSize
     */
    fun send(player: Player, message: CommandMessage, callback: Consumer<ResponseMessage>? = null) {
        val json = gson.toJson(message)
        val utf8Bytes = json.toByteArray(Charsets.UTF_8)

        val maxSize = BlackBoxSettings.maxPayloadSize
        if (utf8Bytes.size > maxSize) {
            warning("[BlackBoxPro] Payload too large for action '${message.action}': ${utf8Bytes.size} > $maxSize bytes, dropping.")
            return
        }

        if (callback != null) {
            pendingCallbacks[message.id] = callback
        }

        // 编码为 Minecraft PacketByteBuf 格式: VarInt(length) + UTF-8 bytes
        val payload = encodeString(utf8Bytes)

        if (BlackBoxSettings.debug) {
            info("[BlackBoxPro] → ${player.name} | ${message.action} (id=${message.id}, ${utf8Bytes.size}B)")
        }

        player.sendPluginMessage(BukkitPlugin.getInstance(), BlackBoxChannels.COMMAND, payload)
    }

    // ── 接收 ──────────────────────────────────────────────

    override fun onPluginMessageReceived(channel: String, player: Player, data: ByteArray) {
        if (channel != BlackBoxChannels.RESPONSE) return

        val json = try {
            decodeString(data)
        } catch (e: Exception) {
            warning("[BlackBoxPro] Failed to decode response bytes from ${player.name}: ${e.message}")
            return
        }

        val response = try {
            gson.fromJson(json, ResponseMessage::class.java)
        } catch (e: Exception) {
            warning("[BlackBoxPro] Failed to parse response from ${player.name}: ${e.message}")
            return
        }

        if (BlackBoxSettings.debug) {
            val data = response.data
            val dataInfo = if (data != null && data.size() > 0) " data=$data" else ""
            info("[BlackBoxPro] ← ${player.name} | ${response.status} (id=${response.id}) ${response.message ?: ""}$dataInfo")
        }

        // 触发一次性回调
        pendingCallbacks.remove(response.id)?.accept(response)

        // 触发全局监听
        globalListeners.forEach { it.accept(response) }
    }

    // ── 监听器管理 ────────────────────────────────────────

    fun addGlobalListener(listener: Consumer<ResponseMessage>) {
        globalListeners.add(listener)
    }

    fun removeGlobalListener(listener: Consumer<ResponseMessage>) {
        globalListeners.remove(listener)
    }

    /**
     * 取消等待中的回调。
     */
    fun cancelPending(id: String) {
        pendingCallbacks.remove(id)
    }

    /**
     * 当前等待响应的数量。
     */
    fun pendingCount(): Int = pendingCallbacks.size

    // ── VarInt 编解码（兼容 Minecraft PacketByteBuf 格式）────

    /**
     * 将 UTF-8 字节编码为 VarInt(length) + bytes 格式。
     */
    private fun encodeString(utf8Bytes: ByteArray): ByteArray {
        val out = ByteArrayOutputStream(utf8Bytes.size + 5)
        writeVarInt(out, utf8Bytes.size)
        out.write(utf8Bytes)
        return out.toByteArray()
    }

    /**
     * 从 VarInt(length) + bytes 格式解码出字符串。
     */
    private fun decodeString(data: ByteArray): String {
        val buf = ByteBuffer.wrap(data)
        val length = readVarInt(buf)
        val bytes = ByteArray(length)
        buf.get(bytes)
        return String(bytes, Charsets.UTF_8)
    }

    private fun writeVarInt(out: ByteArrayOutputStream, value: Int) {
        var v = value
        while (true) {
            if (v and 0x7F.inv() == 0) {
                out.write(v)
                return
            }
            out.write(v and 0x7F or 0x80)
            v = v ushr 7
        }
    }

    private fun readVarInt(buf: ByteBuffer): Int {
        var result = 0
        var shift = 0
        while (true) {
            val b = buf.get().toInt() and 0xFF
            result = result or ((b and 0x7F) shl shift)
            if (b and 0x80 == 0) return result
            shift += 7
            if (shift >= 32) error("VarInt too big")
        }
    }
}
