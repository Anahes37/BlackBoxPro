package com.blackboxpro.fabric

import com.blackboxpro.fabric.action.composite.TickScheduler
import com.blackboxpro.fabric.config.BlackBoxConfig
import com.blackboxpro.fabric.dispatcher.ActionRegistry
import com.blackboxpro.fabric.dispatcher.CommandDispatcher
import com.blackboxpro.fabric.network.NetworkHandler
import com.blackboxpro.fabric.util.ChatHistoryBuffer
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import org.slf4j.LoggerFactory

object BlackBoxProFabric : ClientModInitializer {

    private val logger = LoggerFactory.getLogger("BlackBoxProFabric")
    const val VERSION = "1.0.0"

    override fun onInitializeClient() {
        // 1. 加载配置
        BlackBoxConfig.load()

        // 2. 注册所有行为执行器
        ActionRegistry.registerAll()

        // 3. 初始化调度器
        CommandDispatcher.init()

        // 4. 初始化 Tick 调度器（复合行为用）
        TickScheduler.init()

        // 5. 注册网络通道（最后注册，确保其他组件已就绪）
        NetworkHandler.register()

        // 6. 注册聊天消息监听器（供 query_chat_history 使用）
        ClientReceiveMessageEvents.CHAT.register { message, _, _, _, _ ->
            ChatHistoryBuffer.addMessage(message, "CHAT")
        }
        ClientReceiveMessageEvents.GAME.register { message, isOverlay ->
            ChatHistoryBuffer.addMessage(message, if (isOverlay) "ACTION_BAR" else "SYSTEM")
        }

        logger.info("BlackBoxProFabric v{} loaded. {} actions registered.", VERSION, ActionRegistry.size())
    }
}
