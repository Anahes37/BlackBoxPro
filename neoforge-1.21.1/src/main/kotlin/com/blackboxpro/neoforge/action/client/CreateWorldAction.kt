package com.blackboxpro.neoforge.action.client

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.action.composite.TickScheduler
import com.blackboxpro.neoforge.dispatcher.CommandDispatcher
import com.blackboxpro.neoforge.util.getIntOrDefault
import com.blackboxpro.neoforge.util.requireString
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent
import net.neoforged.neoforge.client.event.RenderFrameEvent
import net.neoforged.neoforge.common.NeoForge
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * 创建新的单人世界并加入。
 *
 * Action ID: "create_world"
 * 参数:
 *   - name: String（必填）— 世界名称
 *   - timeout: Int（可选，默认 1200 ticks = 60s）
 */
class CreateWorldAction : ActionExecutor {

    private val logger = LoggerFactory.getLogger("BlackBoxPro-CreateWorld")

    internal data class PendingCreate(
        val commandId: String,
        val name: String,
        val deadline: Long
    )

    companion object {
        internal val pendingCreates = ConcurrentHashMap<String, PendingCreate>()
        private val logger = LoggerFactory.getLogger("BlackBoxPro-CreateWorld")
        private const val ACTION_ID = "create_world"

        fun init() {
            NeoForge.EVENT_BUS.register(CreateWorldEventListener)
        }
    }

    private object CreateWorldEventListener {
        @SubscribeEvent
        fun onLoggedIn(event: ClientPlayerNetworkEvent.LoggingIn) {
            val iterator = pendingCreates.entries.iterator()
            while (iterator.hasNext()) {
                val (_, pending) = iterator.next()
                if (!pendingCreates.remove(pending.commandId, pending)) {
                    continue
                }
                WorldActionCoordinator.finish(ACTION_ID, pending.commandId)
                logger.info("World '{}' created and loaded (LoggingIn event)", pending.name)
                val data = JsonObject().apply {
                    addProperty("name", pending.name)
                }
                CommandDispatcher.sendResponse(pending.commandId, "success", "Created and joined world: ${pending.name}", data)
            }
        }

        @SubscribeEvent
        fun onRenderFrame(event: RenderFrameEvent.Pre) {
            val now = System.currentTimeMillis()
            val iterator = pendingCreates.entries.iterator()
            while (iterator.hasNext()) {
                val (_, pending) = iterator.next()
                if (now < pending.deadline) {
                    continue
                }
                if (!pendingCreates.remove(pending.commandId, pending)) {
                    continue
                }
                WorldActionCoordinator.finish(ACTION_ID, pending.commandId)
                logger.warn("Create world '{}' timed out", pending.name)
                CommandDispatcher.sendResponse(
                    pending.commandId,
                    "failure",
                    "Timed out waiting to create world: ${pending.name}"
                )
            }
        }
    }

    override fun execute(params: JsonObject): ActionResult =
        ActionResult.fail("CreateWorldAction requires commandId")

    override fun execute(params: JsonObject, commandId: String): ActionResult {
        val client = Minecraft.getInstance()
        val name = params.requireString("name")
        val timeout = params.getIntOrDefault("timeout", 1200)

        if (client.level != null) {
            return ActionResult.fail("Already in a world. Use leave_world first.")
        }
        if (name.isBlank()) {
            return ActionResult.fail("World name cannot be blank")
        }
        val beginError = WorldActionCoordinator.tryBegin(ACTION_ID, commandId)
        if (beginError != null) {
            return ActionResult.fail(beginError)
        }

        logger.info("Creating world: {}", name)

        // 注册等待回调
        val pending = PendingCreate(
            commandId = commandId,
            name = name,
            deadline = System.currentTimeMillis() + timeout * 50L
        )
        pendingCreates[commandId] = pending

        try {
            // 打开创建世界屏幕
            CreateWorldScreen.openFresh(client, null)
        } catch (e: Exception) {
            pendingCreates.remove(commandId, pending)
            WorldActionCoordinator.finish(ACTION_ID, commandId)
            logger.error("Failed to open CreateWorldScreen", e)
            return ActionResult.fail("Failed to open CreateWorldScreen: ${e.message}")
        }

        // 等待屏幕打开后设置参数并触发创建
        TickScheduler.schedule(5) {
            val screen = client.screen
            if (screen is CreateWorldScreen) {
                try {
                    setWorldNameAndCreate(screen, name)
                } catch (e: Exception) {
                    failPendingCreate(pending, "Failed to create world: ${e.message}", e)
                    client.setScreen(null)
                }
            } else {
                failPendingCreate(
                    pending,
                    "Failed to open CreateWorldScreen",
                    "CreateWorldScreen not opened, current screen: ${screen?.javaClass?.simpleName}"
                )
            }
        }

        return ActionResult.async()
    }

    private fun setWorldNameAndCreate(screen: CreateWorldScreen, name: String) {
        val uiStateField = screen.javaClass.declaredFields.find { field ->
            field.type.simpleName == "WorldCreationUiState"
        }

        if (uiStateField != null) {
            uiStateField.isAccessible = true
            val uiState = uiStateField.get(screen)
            val setNameMethod = uiState.javaClass.methods.find { it.name == "setName" && it.parameterCount == 1 }
            setNameMethod?.invoke(uiState, name)
            logger.info("Set world name to: {}", name)
        } else {
            logger.warn("Could not find uiState field, using default world name")
        }

        val onCreateMethod = screen.javaClass.declaredMethods.find { it.name == "onCreate" && it.parameterCount == 0 }
        if (onCreateMethod != null) {
            onCreateMethod.isAccessible = true
            onCreateMethod.invoke(screen)
            logger.info("onCreate() called")
        } else {
            throw RuntimeException("Cannot find onCreate() method on CreateWorldScreen")
        }
    }

    private fun failPendingCreate(pending: PendingCreate, message: String, throwable: Throwable) {
        if (!pendingCreates.remove(pending.commandId, pending)) {
            return
        }
        WorldActionCoordinator.finish(ACTION_ID, pending.commandId)
        logger.error("Create world '{}' failed", pending.name, throwable)
        CommandDispatcher.sendResponse(pending.commandId, "failure", message)
    }

    private fun failPendingCreate(pending: PendingCreate, message: String, detail: String) {
        if (!pendingCreates.remove(pending.commandId, pending)) {
            return
        }
        WorldActionCoordinator.finish(ACTION_ID, pending.commandId)
        logger.error(detail)
        CommandDispatcher.sendResponse(pending.commandId, "failure", message)
    }
}
