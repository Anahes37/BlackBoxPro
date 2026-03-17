package com.blackboxpro.forge

import com.blackboxpro.forge.action.composite.TickScheduler
import com.blackboxpro.forge.config.BlackBoxConfig
import com.blackboxpro.forge.dispatcher.ActionRegistry
import com.blackboxpro.forge.dispatcher.CommandDispatcher
import com.blackboxpro.forge.network.NetworkHandler
import com.blackboxpro.forge.util.ChatHistoryBuffer
import net.minecraft.util.text.ITextComponent
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.apache.logging.log4j.LogManager

@Mod(
    modid = BlackBoxProForge.MOD_ID,
    name = "BlackBoxPro Forge",
    version = BlackBoxProForge.VERSION,
    clientSideOnly = true,
    modLanguageAdapter = "com.blackboxpro.forge.KotlinAdapter"
)
object BlackBoxProForge {

    const val MOD_ID = "blackboxpro"
    const val VERSION = "1.0.0"

    private val logger = LogManager.getLogger("BlackBoxProForge")

    @Mod.EventHandler
    fun init(event: FMLInitializationEvent) {
        // 1. 加载配置
        BlackBoxConfig.load()

        // 2. 注册所有行为执行器
        ActionRegistry.registerAll()

        // 3. 注册事件监听器
        MinecraftForge.EVENT_BUS.register(CommandDispatcher)
        MinecraftForge.EVENT_BUS.register(TickScheduler)
        MinecraftForge.EVENT_BUS.register(this)

        // 4. 注册网络通道
        NetworkHandler.register()

        logger.info("BlackBoxProForge v{} loaded. {} actions registered.", VERSION, ActionRegistry.size())
    }

    @SubscribeEvent
    fun onChatReceived(event: ClientChatReceivedEvent) {
        val type = when (event.type.toInt()) {
            0 -> "CHAT"
            1 -> "SYSTEM"
            2 -> "ACTION_BAR"
            else -> "UNKNOWN"
        }
        ChatHistoryBuffer.addMessage(event.message, type)
    }
}
