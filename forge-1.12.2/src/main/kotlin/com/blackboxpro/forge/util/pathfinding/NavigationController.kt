package com.blackboxpro.forge.util.pathfinding

import com.blackboxpro.forge.action.composite.TickScheduler
import com.blackboxpro.forge.config.NavigationConfig
import com.blackboxpro.forge.util.calculateYawPitch
import net.minecraft.client.Minecraft
import net.minecraft.network.play.client.CPacketPlayer
import org.apache.logging.log4j.LogManager
import kotlin.math.sqrt

class NavigationController(
    private val path: List<PathNode>,
    private val speed: Double,
    private val timeout: Int,
    private val config: NavigationConfig
) {
    private val logger = LogManager.getLogger("BlackBoxPro-Navigation")
    private var currentIndex = 1
    private var ticksElapsed = 0
    private var active = false

    fun start() {
        if (path.size < 2) return
        active = true
        TickScheduler.schedule(1) { tick() }
    }

    fun stop() {
        active = false
    }

    private fun tick() {
        if (!active) return
        if (ticksElapsed >= timeout) {
            logger.debug("Navigation timed out after {} ticks", ticksElapsed)
            stop()
            return
        }

        ticksElapsed++
        val mc = Minecraft.getMinecraft()
        val player = mc.player ?: run { stop(); return }
        val connection = mc.connection ?: run { stop(); return }

        if (currentIndex >= path.size) {
            logger.debug("Navigation completed in {} ticks", ticksElapsed)
            stop()
            return
        }

        var target = path[currentIndex]
        val targetX = target.x + 0.5
        val targetZ = target.z + 0.5

        var dx = targetX - player.posX
        var dz = targetZ - player.posZ
        var horizontalDist = sqrt(dx * dx + dz * dz)

        if (horizontalDist < config.nodeArrivalThreshold) {
            currentIndex++
            if (currentIndex >= path.size) {
                logger.debug("Navigation completed in {} ticks", ticksElapsed)
                stop()
                return
            }
            target = path[currentIndex]
            dx = target.x + 0.5 - player.posX
            dz = target.z + 0.5 - player.posZ
            horizontalDist = sqrt(dx * dx + dz * dz)
        }

        if (target.jumpRequired && player.onGround && target.y > player.posY.toInt()) {
            player.jump()
        }

        val stepSize = config.stepSize * speed
        val ratio = if (horizontalDist > 0.01)
            (stepSize / horizontalDist).coerceAtMost(1.0) else 1.0
        val newX = player.posX + dx * ratio
        val newZ = player.posZ + dz * ratio
        val newY = if (player.onGround) target.y.toDouble() else player.posY

        val (yaw, _) = calculateYawPitch(dx, 0.0, dz)
        player.rotationYaw = yaw

        player.setPosition(newX, newY, newZ)
        connection.sendPacket(
            CPacketPlayer.PositionRotation(newX, newY, newZ, yaw, player.rotationPitch, player.onGround)
        )

        TickScheduler.schedule(1) { tick() }
    }
}
