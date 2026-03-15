package com.blackboxpro.neoforge.action.advanced

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.getBooleanOrDefault
import com.blackboxpro.neoforge.util.requireInt
import com.blackboxpro.neoforge.util.requireString
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket
import net.minecraft.resources.ResourceLocation

class SelectRecipeAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val windowId = params.requireInt("windowId")
        val recipe = params.requireString("recipe")
        val makeAll = params.getBooleanOrDefault("makeAll", false)

        val networkHandler = Minecraft.getInstance().connection
            ?: return ActionResult.fail("Not connected to server")

        networkHandler.send(
            ServerboundPlaceRecipePacket(windowId, ResourceLocation.parse(recipe), makeAll)
        )
        return ActionResult.ok("Selected recipe $recipe in window $windowId (makeAll=$makeAll)")
    }
}
