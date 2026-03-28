package com.blackboxpro.neoforge.action.player

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft

class SneakStopAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val client = Minecraft.getInstance()
        val player = client.player
            ?: return ActionResult.fail("Player not available")

        // 清除客户端本地 sneak 状态
        player.input.keyPresses = player.input.keyPresses.withShift(false)
        player.setShiftKeyDown(false)
        return ActionResult.ok("Stopped sneaking")
    }
}
