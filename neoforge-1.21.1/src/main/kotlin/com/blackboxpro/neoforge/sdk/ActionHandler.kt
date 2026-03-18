package com.blackboxpro.neoforge.sdk

import com.blackboxpro.neoforge.action.ActionResult
import com.google.gson.JsonObject

/**
 * 外部 action 处理器接口。
 *
 * 第三方 Mod 通过 [BlackBoxClient.registerAction] 注册自定义 action 时实现此接口。
 */
fun interface ActionHandler {
    /**
     * 处理 action 请求。
     *
     * @param params   请求参数
     * @param commandId 指令 ID，异步 action 需要通过此 ID 自行发送响应
     * @return 执行结果
     */
    fun handle(params: JsonObject, commandId: String): ActionResult
}
