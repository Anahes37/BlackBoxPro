package com.blackboxpro.neoforge.action.container

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.ContainerTooltipHelper
import com.blackboxpro.neoforge.util.requireInt
import com.google.gson.JsonObject

class HoverSlotAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val windowId = params.requireInt("windowId")
        val slotIndex = params.requireInt("slot")
        val durationTicks = params.get("durationTicks")?.asInt ?: 0
        return runCatching {
            val hoverState = ContainerTooltipHelper.hoverSlot(windowId, slotIndex)
            val data = hoverState.toQueryJson().apply {
                addProperty("tooltipVisible", hoverState.visible)
                addProperty("durationTicks", durationTicks)
            }
            ActionResult.ok("Hovered slot $slotIndex in window $windowId", data)
        }.getOrElse {
            ActionResult.fail(it.message ?: "Failed to hover slot $slotIndex in window $windowId")
        }
    }
}
