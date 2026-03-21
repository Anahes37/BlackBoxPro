package com.blackboxpro.plugin.http

import com.blackboxpro.common.protocol.CommandMessage
import com.blackboxpro.common.protocol.ResponseMessage
import com.blackboxpro.plugin.config.BlackBoxSettings
import com.google.gson.Gson
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import taboolib.common.platform.function.warning

object ExecuteHandler : HttpHandler {

    private val gson = Gson()

    override fun handle(exchange: HttpExchange) {
        val responseJson = buildResponse(exchange)
        sendHttpResponse(exchange, responseJson)
    }

    private fun buildResponse(exchange: HttpExchange): String {
        if (exchange.requestMethod != "POST") {
            return gson.toJson(ResponseMessage("", "failure", "Method not allowed, use POST"))
        }

        val body = try {
            exchange.requestBody.readBytes().toString(Charsets.UTF_8)
        } catch (e: Exception) {
            return gson.toJson(ResponseMessage("", "failure", "Failed to read request body: ${e.message}"))
        }

        val command = try {
            gson.fromJson(body, CommandMessage::class.java)
                ?: return gson.toJson(ResponseMessage("", "failure", "Parse error: null command"))
        } catch (e: Exception) {
            return gson.toJson(ResponseMessage("", "failure", "Parse error: ${e.message}"))
        }

        return try {
            when (BlackBoxSettings.testMode.lowercase()) {
                "dual" -> {
                    val response = ModRelayClient.forward(command, BlackBoxSettings.responseTimeoutMs)
                    gson.toJson(response)
                }
                "server_only" -> {
                    gson.toJson(ResponseMessage(command.id, "failure", "server_only mode: no client actions available"))
                }
                else -> {
                    gson.toJson(ResponseMessage(command.id, "failure", "Unknown test mode: ${BlackBoxSettings.testMode}"))
                }
            }
        } catch (e: Exception) {
            warning("[BlackBoxPro] ExecuteHandler error for action '${command.action}': ${e.message}")
            gson.toJson(ResponseMessage(command.id, "failure", "Internal error: ${e.message}"))
        }
    }

    private fun sendHttpResponse(exchange: HttpExchange, json: String) {
        try {
            val bytes = json.toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.set("Content-Type", "application/json; charset=utf-8")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        } catch (e: Exception) {
            warning("[BlackBoxPro] Failed to write HTTP response: ${e.message}")
        } finally {
            exchange.close()
        }
    }
}
