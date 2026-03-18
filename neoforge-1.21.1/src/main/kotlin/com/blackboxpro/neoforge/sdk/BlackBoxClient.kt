package com.blackboxpro.neoforge.sdk

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.dispatcher.ActionRegistry
import com.blackboxpro.neoforge.http.BlackBoxHttpServer
import com.google.gson.JsonObject
import org.slf4j.LoggerFactory

/**
 * BlackBoxPro SDK 入口。
 *
 * 第三方 NeoForge Mod 引入 BlackBoxPro 后，通过此对象控制测试环境的启停和自定义 action 注册。
 *
 * ```kotlin
 * // 启动测试环境
 * BlackBoxClient.start()
 *
 * // 注册自定义 action
 * BlackBoxClient.registerAction("my_test") { params, commandId ->
 *     ActionResult.ok("Test passed")
 * }
 *
 * // 停止测试环境
 * BlackBoxClient.stop()
 * ```
 */
object BlackBoxClient {

    private val logger = LoggerFactory.getLogger("BlackBoxPro-SDK")

    /** 测试环境是否正在运行 */
    val isRunning: Boolean
        get() = BlackBoxHttpServer.running

    /**
     * 启动测试环境（开启 HTTP Server）。
     *
     * @param port        监听端口，默认 25580
     * @param bindAddress 绑定地址，默认 127.0.0.1
     */
    fun start(port: Int = 25580, bindAddress: String = "127.0.0.1") {
        if (isRunning) {
            logger.warn("BlackBoxPro is already running")
            return
        }
        BlackBoxHttpServer.start(port, bindAddress)
        logger.info("BlackBoxPro SDK started")
    }

    /** 停止测试环境 */
    fun stop() {
        if (!isRunning) {
            logger.warn("BlackBoxPro is not running")
            return
        }
        BlackBoxHttpServer.stop()
        logger.info("BlackBoxPro SDK stopped")
    }

    /**
     * 注册外部 action。
     *
     * 注册后可通过 HTTP `POST /api/command` 调用，与内置 action 使用方式一致。
     *
     * @param actionId action 标识符
     * @param handler  处理器
     */
    fun registerAction(actionId: String, handler: ActionHandler) {
        val executor = object : ActionExecutor {
            override fun execute(params: JsonObject): ActionResult =
                handler.handle(params, "")

            override fun execute(params: JsonObject, commandId: String): ActionResult =
                handler.handle(params, commandId)
        }
        ActionRegistry.registerExternal(actionId, executor)
    }

    /**
     * 注销外部 action。
     *
     * @param actionId action 标识符
     * @return 是否成功注销
     */
    fun unregisterAction(actionId: String): Boolean =
        ActionRegistry.unregisterExternal(actionId)
}
