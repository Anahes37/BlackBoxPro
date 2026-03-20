package com.blackboxpro.fabric.dispatcher

import com.blackboxpro.fabric.network.NetworkHandler
import com.blackboxpro.runtime.dispatcher.RuntimeCommandDispatcher
import com.blackboxpro.runtime.dispatcher.RuntimeResponseSender
import com.google.gson.Gson
import com.google.gson.JsonObject
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.minecraft.client.MinecraftClient
import org.slf4j.LoggerFactory

object CommandDispatcher {

    private val logger = LoggerFactory.getLogger("BlackBoxPro-Dispatcher")
    private val gson = Gson()

    fun init() {
        RuntimeCommandDispatcher.bind(
            logger = logger,
            mainThreadExecutor = object : RuntimeCommandDispatcher.MainThreadExecutor {
                override fun execute(task: () -> Unit) {
                    MinecraftClient.getInstance().execute(task)
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

        ClientTickEvents.END_CLIENT_TICK.register {
            RuntimeCommandDispatcher.tick()
        }

        ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
            val count = RuntimeCommandDispatcher.clear()
            if (count > 0) {
                logger.info("Cleared {} delayed commands on disconnect", count)
            }
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
