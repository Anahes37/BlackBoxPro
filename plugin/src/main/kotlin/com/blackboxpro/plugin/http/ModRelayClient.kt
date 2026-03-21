package com.blackboxpro.plugin.http

import com.blackboxpro.common.protocol.CommandMessage
import com.blackboxpro.common.protocol.HttpEndpoints
import com.blackboxpro.common.protocol.ResponseMessage
import com.blackboxpro.plugin.config.BlackBoxSettings
import com.google.gson.Gson
import taboolib.common.platform.function.warning
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

object ModRelayClient {

    private val gson = Gson()
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build()

    fun forward(command: CommandMessage, timeoutMs: Long = BlackBoxSettings.responseTimeoutMs): ResponseMessage {
        val json = gson.toJson(command)
        val address = BlackBoxSettings.modHttpAddress
        return try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create("$address${HttpEndpoints.EXECUTE}"))
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(json, Charsets.UTF_8))
                .timeout(Duration.ofMillis(timeoutMs + 2000))
                .build()
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(Charsets.UTF_8))
            gson.fromJson(response.body(), ResponseMessage::class.java)
                ?: ResponseMessage(command.id, "failure", "Empty response from mod")
        } catch (e: Exception) {
            warning("[BlackBoxPro] Mod relay failed for action '${command.action}': ${e.message}")
            ResponseMessage(command.id, "failure", "Mod relay failed: ${e.message}")
        }
    }
}
