package com.blackboxpro.fabric.action.query

import com.blackboxpro.fabric.action.ActionExecutor
import com.blackboxpro.fabric.action.ActionResult
import com.google.gson.JsonObject
import net.minecraft.client.MinecraftClient

/**
 * 读取玩家完整状态信息。
 * Action ID: "query_player_state"
 */
class QueryPlayerStateAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val client = MinecraftClient.getInstance()
        val player = client.player
            ?: return ActionResult.fail("Player not available")

        val data = JsonObject().apply {
            addProperty("x", player.x)
            addProperty("y", player.y)
            addProperty("z", player.z)
            addProperty("yaw", player.yaw)
            addProperty("pitch", player.pitch)
            addProperty("health", player.health)
            addProperty("maxHealth", player.maxHealth)
            addProperty("food", player.hungerManager.foodLevel)
            addProperty("saturation", player.hungerManager.saturationLevel)
            addProperty("gameMode", client.interactionManager?.currentGameMode?.getId() ?: "unknown")
            addProperty("onGround", player.isOnGround)
            addProperty("sneaking", player.isSneaking)
            addProperty("sprinting", player.isSprinting)
            addProperty("flying", player.abilities.flying)
            addProperty("dead", player.isDead)
            addProperty("selectedSlot", player.inventory.selectedSlot)
            addProperty("experienceLevel", player.experienceLevel)
            addProperty("experienceProgress", player.experienceProgress)
        }

        return ActionResult.ok("Player state queried", data)
    }
}
