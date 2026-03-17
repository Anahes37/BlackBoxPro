package com.blackboxpro.forge.action.query

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.world.GameType

class QueryPlayerStateAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val mc = Minecraft.getMinecraft()
        val player = mc.player
            ?: return ActionResult.fail("Player not available")

        val data = JsonObject().apply {
            addProperty("x", player.posX)
            addProperty("y", player.posY)
            addProperty("z", player.posZ)
            addProperty("yaw", player.rotationYaw)
            addProperty("pitch", player.rotationPitch)
            addProperty("health", player.health)
            addProperty("maxHealth", player.maxHealth)
            addProperty("food", player.foodStats.foodLevel)
            addProperty("saturation", player.foodStats.saturationLevel)
            addProperty("gameMode", mc.playerController?.currentGameType?.getName() ?: "unknown")
            addProperty("dimension", player.dimension)
            addProperty("onGround", player.onGround)
            addProperty("sneaking", player.isSneaking)
            addProperty("sprinting", player.isSprinting)
            addProperty("flying", player.capabilities.isFlying)
            addProperty("dead", player.isDead)
            addProperty("selectedSlot", player.inventory.currentItem)
            addProperty("experienceLevel", player.experienceLevel)
            addProperty("experienceProgress", player.experience)
        }

        return ActionResult.ok("Player state queried", data)
    }
}
