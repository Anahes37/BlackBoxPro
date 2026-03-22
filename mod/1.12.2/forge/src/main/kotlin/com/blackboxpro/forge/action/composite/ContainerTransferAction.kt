package com.blackboxpro.forge.action.composite

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.dispatcher.ActionRegistry
import com.blackboxpro.forge.util.requireInt
import com.google.gson.JsonObject

class ContainerTransferAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val windowId = params.requireInt("windowId")
        val slotId = params.requireInt("slotId")
        val actionNumber = params.requireInt("actionNumber")

        // Shift-click: clickType=QUICK_MOVE, mouseButton=0
        val clickSlot = ActionRegistry.find("click_slot")
            ?: return ActionResult.fail("click_slot action not registered")
        val result = clickSlot.execute(JsonObject().apply {
            addProperty("windowId", windowId)
            addProperty("slotId", slotId)
            addProperty("mouseButton", 0)
            addProperty("clickType", "quick_move")
            addProperty("actionNumber", actionNumber)
        })
        if (!result.success) return ActionResult.fail("Failed to transfer slot $slotId: ${result.message}")

        return ActionResult.ok("Transferred slot $slotId in window $windowId")
    }
}
