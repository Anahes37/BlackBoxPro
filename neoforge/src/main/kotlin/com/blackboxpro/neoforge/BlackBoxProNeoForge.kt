package com.blackboxpro.neoforge

import com.blackboxpro.neoforge.action.composite.TickScheduler
import com.blackboxpro.neoforge.config.BlackBoxConfig
import com.blackboxpro.neoforge.dispatcher.ActionRegistry
import com.blackboxpro.neoforge.dispatcher.CommandDispatcher
import com.blackboxpro.neoforge.network.NetworkHandler
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.common.Mod
import org.slf4j.LoggerFactory

@Mod("blackboxpro")
class BlackBoxProNeoForge(modBus: IEventBus) {

    init {
        val logger = LoggerFactory.getLogger("BlackBoxProNeoForge")

        // 1. 加载配置
        BlackBoxConfig.load()

        // 2. 注册所有行为执行器
        ActionRegistry.registerAll()

        // 3. 初始化调度器
        CommandDispatcher.init()

        // 4. 初始化 Tick 调度器（复合行为用）
        TickScheduler.init()

        // 5. 注册网络通道
        NetworkHandler.register(modBus)

        logger.info("BlackBoxProNeoForge v{} loaded. {} actions registered.", VERSION, ActionRegistry.size())
    }

    companion object {
        const val VERSION = "1.0.0"
    }
}
