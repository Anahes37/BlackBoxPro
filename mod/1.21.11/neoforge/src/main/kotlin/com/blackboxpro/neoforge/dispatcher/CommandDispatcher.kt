package com.blackboxpro.neoforge.dispatcher

import com.blackboxpro.neoforge.network.NetworkHandler
import com.blackboxpro.runtime.dispatcher.RuntimeCommandDispatcher
import com.blackboxpro.runtime.dispatcher.RuntimeResponseSender
import com.google.gson.Gson
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.common.NeoForge
import org.slf4j.LoggerFactory

object CommandDispatcher {

    private val logger = LoggerFactory.getLogger("BlackBoxPro-Dispatcher")
    private val gson = Gson()

    fun init() {
        RuntimeCommandDispatcher.bind(
            logger = logger,
            mainThreadExecutor = object : RuntimeCommandDispatcher.MainThreadExecutor {
                override fun execute(task: () -> Unit) {
                    Minecraft.getInstance().execute(task)
                }
            },
            actionResolver = object : RuntimeCommandDispatcher.ActionResolver {
                override fun find(actionId: String) = ActionRegistry.find(actionId)
            }
        )

        RuntimeResponseSender.bind(object : RuntimeResponseSender.Sender {
            override fun sendResponse(id: String, status: String, message: String?, data: JsonObject?) {
                val response = ResponseMessage(id, status, message, data)
                NetworkHandler.sendResponse(gson.toJson(response))
            }
        })

        NeoForge.EVENT_BUS.register(this)
    }

    @SubscribeEvent
    fun onClientTick(event: ClientTickEvent.Post) {
        RuntimeCommandDispatcher.tick()
    }

    @SubscribeEvent
    fun onDisconnect(event: ClientPlayerNetworkEvent.LoggingOut) {
        val count = RuntimeCommandDispatcher.clear()
        if (count > 0) {
            logger.info("Cleared {} delayed commands on disconnect", count)
        }
    }

    fun dispatch(message: CommandMessage) {
        RuntimeCommandDispatcher.dispatch(message)
    }

    /** 供异步 Action 自行发送响应，不要在普通 Action 中调用 */
    internal fun sendResponse(
        id: String,
        status: String,
        message: String? = null,
        data: JsonObject? = null
    ) {
        RuntimeCommandDispatcher.sendResponse(id, status, message, data)
    }
}
