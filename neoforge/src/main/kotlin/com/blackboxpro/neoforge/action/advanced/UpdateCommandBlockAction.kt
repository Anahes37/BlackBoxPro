package com.blackboxpro.neoforge.action.advanced

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.getIntOrDefault
import com.blackboxpro.neoforge.util.requireInt
import com.blackboxpro.neoforge.util.requireString
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundSetCommandBlockPacket
import net.minecraft.core.BlockPos

class UpdateCommandBlockAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val x = params.requireInt("x")
        val y = params.requireInt("y")
        val z = params.requireInt("z")
        val command = params.requireString("command")
        val mode = params.getIntOrDefault("mode", 0)
        val flags = params.getIntOrDefault("flags", 0)

        val networkHandler = Minecraft.getInstance().connection
            ?: return ActionResult.fail("Not connected to server")

        networkHandler.send(
            ServerboundSetCommandBlockPacket(BlockPos(x, y, z), command, mode, flags)
        )
        return ActionResult.ok("Updated command block at $x, $y, $z")
    }
}
