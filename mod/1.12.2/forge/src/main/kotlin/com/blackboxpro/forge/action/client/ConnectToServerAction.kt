package com.blackboxpro.forge.action.client

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.requireString
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiMainMenu
import net.minecraft.client.multiplayer.GuiConnecting
import net.minecraft.client.multiplayer.ServerData

class ConnectToServerAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val ip = params.requireString("ip")
        val mc = Minecraft.getMinecraft()
        val serverData = ServerData("Test Server", ip, false)
        mc.addScheduledTask {
            mc.displayGuiScreen(GuiConnecting(mc.currentScreen ?: GuiMainMenu(), mc, serverData))
        }
        return ActionResult.ok("Connecting to $ip")
    }
}
