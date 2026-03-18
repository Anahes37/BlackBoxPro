package com.blackboxpro.neoforge.action.movement

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.requireDouble
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket

class MoveVehicleAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val x = params.requireDouble("x")
        val y = params.requireDouble("y")
        val z = params.requireDouble("z")
        val yaw = params.requireDouble("yaw").toFloat()
        val pitch = params.requireDouble("pitch").toFloat()

        val client = Minecraft.getInstance()
        val player = client.player
            ?: return ActionResult.fail("Player not available")
        val handler = client.connection
            ?: return ActionResult.fail("Not connected to server")

        val vehicle = player.vehicle
            ?: return ActionResult.fail("Player is not riding a vehicle")

        vehicle.setPos(x, y, z)
        vehicle.yRot = yaw
        vehicle.xRot = pitch
        handler.send(ServerboundMoveVehiclePacket(vehicle))
        return ActionResult.ok()
    }
}
