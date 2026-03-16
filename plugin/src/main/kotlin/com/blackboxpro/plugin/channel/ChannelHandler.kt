package com.blackboxpro.plugin.channel

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.bukkit.entity.Player
import org.bukkit.plugin.messaging.PluginMessageListener
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.Plugin
import taboolib.common.platform.function.info
import taboolib.common.platform.function.pluginId
import taboolib.common.platform.function.warning
import taboolib.platform.BukkitPlugin
import java.util.UUID
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
     */
    fun send(player: Player, message: CommandMessage, callback: Consumer<ResponseMessage>? = null) {
        if (callback != null) {
            pendingCallbacks[message.id] = callback
        }
        val json = gson.toJson(message)
        player.sendPluginMessage(BukkitPlugin.getInstance(), BlackBoxChannels.COMMAND, json.toByteArray(Charsets.UTF_8))
    }

    // ── 接收 ──────────────────────────────────────────────

    override fun onPluginMessageReceived(channel: String, player: Player, data: ByteArray) {
        if (channel != BlackBoxChannels.RESPONSE) return

        val json = data.toString(Charsets.UTF_8)
        val response = try {
            gson.fromJson(json, ResponseMessage::class.java)
        } catch (e: Exception) {
            warning("[BlackBoxPro] Failed to parse response from ${player.name}: ${e.message}")
            return
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
}
