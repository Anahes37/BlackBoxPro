package com.blackboxpro.fabric.util.pathfinding

import com.blackboxpro.fabric.action.composite.TickScheduler
import com.blackboxpro.fabric.config.NavigationConfig
import com.blackboxpro.fabric.util.calculateYawPitch
import net.minecraft.client.MinecraftClient
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import org.slf4j.LoggerFactory
import kotlin.math.sqrt

/**
 * 路径跟随控制器。
 * 每 tick 驱动玩家沿 A* 路径移动，支持自动跳跃和转向。
 */
class NavigationController(
    private val path: List<PathNode>,
    private val speed: Double,
    private val timeout: Int,
    private val config: NavigationConfig
) {
    private val logger = LoggerFactory.getLogger("BlackBoxPro-Navigation")
    private var currentIndex = 1 // 跳过起点
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
        val client = MinecraftClient.getInstance()
        val player = client.player ?: run { stop(); return }
        val networkHandler = client.networkHandler ?: run { stop(); return }

        if (currentIndex >= path.size) {
            logger.debug("Navigation completed in {} ticks", ticksElapsed)
            stop()
            return
        }

        var target = path[currentIndex]
        val targetX = target.x + 0.5
        val targetZ = target.z + 0.5

        var dx = targetX - player.x
        var dz = targetZ - player.z
        var horizontalDist = sqrt(dx * dx + dz * dz)

        // 到达当前路径点 → 前进
        if (horizontalDist < config.nodeArrivalThreshold) {
            currentIndex++
            if (currentIndex >= path.size) {
                logger.debug("Navigation completed in {} ticks", ticksElapsed)
                stop()
                return
            }
            target = path[currentIndex]
            dx = target.x + 0.5 - player.x
            dz = target.z + 0.5 - player.z
            horizontalDist = sqrt(dx * dx + dz * dz)
        }

        // 跳跃
        if (target.jumpRequired && player.isOnGround && target.y > player.y.toInt()) {
            player.jump()
        }

        // 移动
        val stepSize = config.stepSize * speed
        val ratio = if (horizontalDist > 0.01) (stepSize / horizontalDist).coerceAtMost(1.0) else 1.0
        val newX = player.x + dx * ratio
        val newZ = player.z + dz * ratio
        val newY = if (player.isOnGround) target.y.toDouble() else player.y

        // 转向
        val (yaw, _) = calculateYawPitch(dx, 0.0, dz)
        player.yaw = yaw

        player.setPosition(newX, newY, newZ)
        networkHandler.sendPacket(
            PlayerMoveC2SPacket.Full(newX, newY, newZ, yaw, player.pitch, player.isOnGround, player.horizontalCollision)
        )

        TickScheduler.schedule(1) { tick() }
    }
}
