package com.blackboxpro.neoforge.action.query

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft

/**
 * 读取玩家完整状态信息。
 * Action ID: "query_player_state"
 */
class QueryPlayerStateAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val client = Minecraft.getInstance()
        val player = client.player
            ?: return ActionResult.fail("Player not available")

        val data = JsonObject().apply {
            addProperty("x", player.x)
            addProperty("y", player.y)
            addProperty("z", player.z)
            addProperty("yaw", player.yRot)
            addProperty("pitch", player.xRot)
            addProperty("health", player.health)
            addProperty("maxHealth", player.maxHealth)
            addProperty("food", player.foodData.foodLevel)
            addProperty("saturation", player.foodData.saturationLevel)
            addProperty("gameMode", client.gameMode?.playerMode?.getName() ?: "unknown")
            addProperty("onGround", player.onGround())
            addProperty("sneaking", player.isShiftKeyDown)
            addProperty("sprinting", player.isSprinting)
            addProperty("flying", player.abilities.flying)
            addProperty("dead", player.isDeadOrDying)
            addProperty("selectedSlot", player.inventory.getSelectedSlot())
            addProperty("experienceLevel", player.experienceLevel)
            addProperty("experienceProgress", player.experienceProgress)
        }

        return ActionResult.ok("Player state queried", data)
    }
}
