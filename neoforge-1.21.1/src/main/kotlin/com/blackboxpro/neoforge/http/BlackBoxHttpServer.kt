package com.blackboxpro.neoforge.http

import com.blackboxpro.neoforge.config.BlackBoxConfig
import com.blackboxpro.neoforge.dispatcher.CommandDispatcher
import com.blackboxpro.neoforge.dispatcher.CommandMessage
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * 内嵌 HTTP Server，基于 JDK com.sun.net.httpserver。
 *
 * POST /api/command — 发送指令并同步等待响应返回。
 */
object BlackBoxHttpServer {

    private val logger = LoggerFactory.getLogger("BlackBoxPro-HttpServer")
    private val gson = Gson()
    private var server: HttpServer? = null

    @Volatile
    var running = false
        private set

    fun start() {
        val config = BlackBoxConfig.current.http
        if (!config.enabled) {
            logger.info("HTTP server is disabled in config")
            return
        }
        if (running) {
            logger.warn("HTTP server is already running")
            return
        }

        try {
            val addr = InetSocketAddress(config.bindAddress, config.port)
            val httpServer = HttpServer.create(addr, 0)
            httpServer.executor = Executors.newCachedThreadPool { r ->
                Thread(r, "BlackBoxPro-Http").apply { isDaemon = true }
            }
            httpServer.createContext("/api/command", ::handleCommand)
            httpServer.start()
            server = httpServer
            running = true
            logger.info("HTTP server started on {}:{}", config.bindAddress, config.port)
        } catch (e: Exception) {
            logger.error("Failed to start HTTP server", e)
        }
    }

    fun stop() {
        if (!running) return
        running = false
        server?.stop(0)
        server = null
        logger.info("HTTP server shut down")
    }

    private fun handleCommand(exchange: HttpExchange) {
        // CORS
        exchange.responseHeaders.add("Access-Control-Allow-Origin", "*")
        exchange.responseHeaders.add("Access-Control-Allow-Methods", "POST, OPTIONS")
        exchange.responseHeaders.add("Access-Control-Allow-Headers", "Content-Type")

        if (exchange.requestMethod == "OPTIONS") {
            exchange.sendResponseHeaders(204, -1)
            exchange.close()
            return
        }

        if (exchange.requestMethod != "POST") {
            sendError(exchange, 405, "Method not allowed")
            return
        }

        val body = exchange.requestBody.bufferedReader(Charsets.UTF_8).use { it.readText() }
        if (body.isBlank()) {
            sendError(exchange, 400, "Empty request body")
            return
        }

        // 解析超时参数 ?timeout=60
        val query = exchange.requestURI.query ?: ""
        val timeoutSec = query.split("&")
            .firstOrNull { it.startsWith("timeout=") }
            ?.substringAfter("timeout=")
            ?.toLongOrNull() ?: 60L

        var commandId: String? = null
        try {
            val obj = gson.fromJson(body, JsonObject::class.java)
            val action = obj.get("action")?.asString
            if (action == null) {
                sendError(exchange, 400, "Missing 'action' field")
                return
            }

            commandId = obj.get("id")?.asString ?: UUID.randomUUID().toString()
            val params = obj.getAsJsonObject("params") ?: JsonObject()
            val delay = obj.get("delay")?.asLong ?: 0L

            val maxSize = BlackBoxConfig.current.network.maxPayloadSize
            if (body.toByteArray(Charsets.UTF_8).size > maxSize) {
                sendError(exchange, 413, "Payload too large")
                return
            }

            val message = CommandMessage(id = commandId, action = action, params = params, delay = delay)

            // 注册响应路由，拿到 future
            val future = ResponseRouter.register(commandId)

            // 调度到主线程执行
            CommandDispatcher.dispatch(message)

            // 阻塞等待响应
            val responseJson = try {
                future.get(timeoutSec, TimeUnit.SECONDS)
            } catch (_: TimeoutException) {
                ResponseRouter.remove(commandId)
                gson.toJson(mapOf("id" to commandId, "status" to "failure", "message" to "Timeout after ${timeoutSec}s"))
            }

            sendJson(exchange, 200, responseJson)
        } catch (e: Exception) {
            logger.error("Error handling HTTP command", e)
            val errorJson = gson.toJson(mapOf(
                "id" to (commandId ?: "unknown"),
                "status" to "failure",
                "message" to "Server error: ${e.message}"
            ))
            sendJson(exchange, 500, errorJson)
        }
    }

    private fun sendJson(exchange: HttpExchange, code: Int, json: String) {
        val bytes = json.toByteArray(Charsets.UTF_8)
        exchange.responseHeaders.add("Content-Type", "application/json; charset=utf-8")
        exchange.sendResponseHeaders(code, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }

    private fun sendError(exchange: HttpExchange, code: Int, message: String) {
        sendJson(exchange, code, gson.toJson(mapOf("status" to "failure", "message" to message)))
    }
}
