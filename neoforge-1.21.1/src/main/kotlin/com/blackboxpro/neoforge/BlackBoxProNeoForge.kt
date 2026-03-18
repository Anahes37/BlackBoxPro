package com.blackboxpro.neoforge

import com.blackboxpro.neoforge.action.client.CreateWorldAction
import com.blackboxpro.neoforge.action.client.JoinWorldAction
import com.blackboxpro.neoforge.action.client.LeaveWorldAction
import com.blackboxpro.neoforge.action.composite.TickScheduler
import com.blackboxpro.neoforge.config.BlackBoxConfig
import com.blackboxpro.neoforge.dispatcher.ActionRegistry
import com.blackboxpro.neoforge.dispatcher.CommandDispatcher
import com.blackboxpro.neoforge.network.NetworkHandler
import com.blackboxpro.neoforge.http.BlackBoxHttpServer
import com.blackboxpro.neoforge.util.ChatHistoryBuffer
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent
import net.neoforged.neoforge.common.NeoForge
import org.slf4j.LoggerFactory
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS

@Mod("blackboxpro")
object BlackBoxProNeoForge {

    const val VERSION = "1.0.0"
    private val logger = LoggerFactory.getLogger("BlackBoxProNeoForge")

    init {
        logger.info("=== BlackBoxPro INIT START ===")

        try {
            BlackBoxConfig.load()
            logger.info("Config loaded")

            ActionRegistry.registerAll()
            logger.info("{} actions registered", ActionRegistry.size())

            CommandDispatcher.init()
            TickScheduler.init()
            NetworkHandler.register(MOD_BUS)
            NeoForge.EVENT_BUS.register(ChatEventListener)

            // 初始化世界管理 Action 的事件监听
            CreateWorldAction.init()
            JoinWorldAction.init()
            LeaveWorldAction.init()

            BlackBoxHttpServer.start()

            Runtime.getRuntime().addShutdownHook(Thread { BlackBoxHttpServer.stop() })

            logger.info("BlackBoxProNeoForge v{} loaded.", VERSION)
        } catch (e: Throwable) {
            logger.error("BlackBoxPro init failed", e)
            throw e
        }
    }

    private object ChatEventListener {
        @SubscribeEvent
        fun onSystemChat(event: ClientChatReceivedEvent.System) {
            val type = if (event.isOverlay()) "ACTION_BAR" else "SYSTEM"
            ChatHistoryBuffer.addMessage(event.message, type)
        }

        @SubscribeEvent
        fun onPlayerChat(event: ClientChatReceivedEvent.Player) {
            ChatHistoryBuffer.addMessage(event.message, "CHAT")
        }
    }
}
