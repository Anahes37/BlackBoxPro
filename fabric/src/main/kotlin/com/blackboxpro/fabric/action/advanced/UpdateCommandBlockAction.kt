package com.blackboxpro.fabric.action.advanced

import com.blackboxpro.fabric.action.ActionExecutor
import com.blackboxpro.fabric.action.ActionResult
import com.blackboxpro.fabric.util.getIntOrDefault
import com.blackboxpro.fabric.util.requireInt
import com.blackboxpro.fabric.util.requireString
import com.google.gson.JsonObject
import net.minecraft.client.MinecraftClient
import net.minecraft.network.packet.c2s.play.UpdateCommandBlockC2SPacket
import net.minecraft.util.math.BlockPos

class UpdateCommandBlockAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val x = params.requireInt("x")
        val y = params.requireInt("y")
        val z = params.requireInt("z")
        val command = params.requireString("command")
        val mode = params.getIntOrDefault("mode", 0)
        val flags = params.getIntOrDefault("flags", 0)

        val networkHandler = MinecraftClient.getInstance().networkHandler
            ?: return ActionResult.fail("Not connected to server")

        networkHandler.sendPacket(
            UpdateCommandBlockC2SPacket(BlockPos(x, y, z), command, mode, flags)
        )
        return ActionResult.ok("Updated command block at $x, $y, $z")
    }
}
