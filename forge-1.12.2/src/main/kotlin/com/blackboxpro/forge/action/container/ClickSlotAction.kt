package com.blackboxpro.forge.action.container

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.requireInt
import com.blackboxpro.forge.util.requireString
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.inventory.ClickType
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.CPacketClickWindow

class ClickSlotAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val windowId = params.requireInt("windowId")
        val slotId = params.requireInt("slotId")
        val mouseButton = params.requireInt("mouseButton")
        val clickTypeStr = params.requireString("clickType")
        val actionNumber = params.requireInt("actionNumber").toShort()

        val clickType = when (clickTypeStr.lowercase()) {
            "pickup" -> ClickType.PICKUP
            "quick_move" -> ClickType.QUICK_MOVE
            "swap" -> ClickType.SWAP
            "clone" -> ClickType.CLONE
            "throw" -> ClickType.THROW
            "quick_craft" -> ClickType.QUICK_CRAFT
            "pickup_all" -> ClickType.PICKUP_ALL
            else -> return ActionResult.fail("Invalid clickType: $clickTypeStr")
        }

        val connection = Minecraft.getMinecraft().connection
            ?: return ActionResult.fail("Not connected to server")

        connection.sendPacket(
            CPacketClickWindow(windowId, slotId, mouseButton, clickType, ItemStack.EMPTY, actionNumber)
        )
        return ActionResult.ok("Clicked slot $slotId in window $windowId (type=$clickType)")
    }
}
