package com.blackboxpro.neoforge.action.chat

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.requireString
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft

class ChatCommandAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val command = params.requireString("command").removePrefix("/")

        if (command.length > 256) {
            return ActionResult.fail("Command too long: ${command.length} > 256")
        }

        val networkHandler = Minecraft.getInstance().connection
            ?: return ActionResult.fail("Not connected to server")

        networkHandler.sendCommand(command)
        return ActionResult.ok("Sent command: /$command")
    }
}
