package com.blackboxpro.forge.action.query

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.getBooleanOrDefault
import com.blackboxpro.forge.util.requireInt
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.util.text.TextFormatting

class QuerySlotTooltipAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val slotIndex = params.requireInt("slot")
        val advanced = params.getBooleanOrDefault("advanced", false)

        val mc = Minecraft.getMinecraft()
        val player = mc.player ?: return ActionResult.fail("Player not available")
        val container = player.openContainer

        if (slotIndex < 0 || slotIndex >= container.inventorySlots.size) {
            return ActionResult.fail("Slot index $slotIndex out of range [0, ${container.inventorySlots.size})")
        }

        val stack = container.inventorySlots[slotIndex].stack
        if (stack.isEmpty) {
            return ActionResult.ok("Slot $slotIndex is empty", JsonObject().apply {
                addProperty("slot", slotIndex)
                addProperty("empty", true)
            })
        }

        val tooltipFlag = if (advanced) ITooltipFlag.TooltipFlags.ADVANCED else ITooltipFlag.TooltipFlags.NORMAL
        val tooltipFormatted = stack.getTooltip(player, tooltipFlag)
        val tooltipPlain = tooltipFormatted.map { TextFormatting.getTextWithoutFormattingCodes(it) ?: it }

        val data = JsonObject().apply {
            addProperty("slot", slotIndex)
            addProperty("empty", false)
            addProperty("itemId", stack.item.registryName?.toString() ?: "unknown")
            addProperty("itemName", stack.displayName)
            addProperty("count", stack.count)
            addProperty("damage", stack.itemDamage)
            addProperty("maxDamage", stack.maxDamage)
            add("tooltip", JsonArray().apply {
                tooltipPlain.forEach { add(it) }
            })
            add("tooltipFormatted", JsonArray().apply {
                tooltipFormatted.forEach { add(it) }
            })
        }
        return ActionResult.ok("Tooltip queried for slot $slotIndex", data)
    }
}
