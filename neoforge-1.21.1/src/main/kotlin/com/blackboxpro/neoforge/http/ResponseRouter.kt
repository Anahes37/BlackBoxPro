package com.blackboxpro.neoforge.http

import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * HTTP 响应路由器。
 * 将 commandId 映射到 CompletableFuture，
 * 使异步 Action 的响应能正确路由回 HTTP 请求。
 */
object ResponseRouter {

    private val routes = ConcurrentHashMap<String, CompletableFuture<String>>()

    /** 注册一个 commandId，返回等待响应的 Future */
    fun register(commandId: String): CompletableFuture<String> {
        val future = CompletableFuture<String>()
        routes[commandId] = future
        return future
    }

    /**
     * 尝试将响应路由到 HTTP 请求。
     * @return true 如果成功路由（commandId 有对应的 future），false 则 fallback 到网络
     */
    fun tryRoute(commandId: String, json: String): Boolean {
        val future = routes.remove(commandId) ?: return false
        future.complete(json)
        return true
    }

    /** 移除指定 commandId 的路由（超时清理） */
    fun remove(commandId: String) {
        routes.remove(commandId)
    }
}
