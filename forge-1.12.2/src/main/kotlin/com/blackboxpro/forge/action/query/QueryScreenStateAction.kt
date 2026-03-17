package com.blackboxpro.forge.action.query

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.*

class QueryScreenStateAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val mc = Minecraft.getMinecraft()
        val screen = mc.currentScreen

        val data = JsonObject().apply {
            addProperty("open", screen != null)
            addProperty("screenClass", screen?.javaClass?.simpleName ?: "none")

            if (screen is GuiContainer) {
                addProperty("isContainer", true)
                val container = mc.player?.openContainer
                if (container != null) {
                    addProperty("windowId", container.windowId)
                    addProperty("slotCount", container.inventorySlots.size)
                    val title = container.inventorySlots
                        .firstOrNull()?.inventory?.name ?: screen.javaClass.simpleName
                    addProperty("title", title)
                }
            } else {
                addProperty("isContainer", false)
                addProperty("title", screen?.javaClass?.simpleName ?: "")
            }

            addProperty("screenType", classifyScreen(screen))
        }

        return ActionResult.ok("Screen state queried", data)
    }

    private fun classifyScreen(screen: net.minecraft.client.gui.GuiScreen?): String = when (screen) {
        null -> "none"
        is GuiInventory -> "player_inventory"
        is GuiContainerCreative -> "creative_inventory"
        is GuiChest -> "generic_container"
        is GuiDispenser -> "generic_3x3"
        is GuiCrafting -> "crafting_table"
        is GuiFurnace -> "furnace"
        is GuiBrewingStand -> "brewing_stand"
        is GuiBeacon -> "beacon"
        is GuiScreenHorseInventory -> "horse"
        is GuiContainer -> "container_unknown"
        else -> "other"
    }
}
