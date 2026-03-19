package com.blackboxpro.neoforge.util

import net.minecraft.client.player.Input
import net.minecraft.client.player.LocalPlayer

/**
 * 自定义 Input 子类，用于注入模拟键盘输入。
 * 替换 KeyboardInput 后，每 tick 的 tick() 会从注入的布尔值计算 forwardImpulse/leftImpulse，
 * 而不是从真实按键状态读取。
 *
 * 使用方式：
 * 1. [install] 安装到玩家，保存原始 Input
 * 2. 每 tick 通过 [forward]/[jump] 等属性设置期望输入
 * 3. 移动完成后调用 [uninstall] 恢复原始 Input
 */
class InjectedInput : Input() {

    /** 原始 Input（通常是 KeyboardInput），卸载时恢复 */
    private var original: Input? = null

    /** 安装时的 player 引用，用于卸载时恢复 */
    private var installedPlayer: LocalPlayer? = null

    var forward: Boolean = false
    var backward: Boolean = false
    var left: Boolean = false
    var right: Boolean = false
    var jump: Boolean = false
    var shift: Boolean = false
    var sprinting: Boolean = false

    override fun tick(slowDown: Boolean, sneakSpeedModifier: Float) {
        this.up = forward
        this.down = backward
        this.left = left
        this.right = right
        this.jumping = jump
        this.shiftKeyDown = shift

        this.forwardImpulse = if (up) 1.0f else 0.0f
        this.forwardImpulse -= if (down) 1.0f else 0.0f
        this.leftImpulse = if (this.left) 1.0f else 0.0f
        this.leftImpulse -= if (this.right) 1.0f else 0.0f

        if (slowDown) {
            this.forwardImpulse *= sneakSpeedModifier
            this.leftImpulse *= sneakSpeedModifier
        }
    }

    /** 安装到玩家，替换原始 Input */
    fun install(player: LocalPlayer) {
        original = player.input
        installedPlayer = player
        player.input = this
    }

    /** 卸载，恢复原始 Input。优先使用安装时保存的 player 引用。 */
    fun uninstall(player: LocalPlayer? = null) {
        val target = player ?: installedPlayer
        original?.let { orig ->
            if (target != null && target.input === this) {
                target.input = orig
            }
        }
        original = null
        installedPlayer = null
    }

    /** 重置所有输入为默认值 */
    fun reset() {
        forward = false
        backward = false
        left = false
        right = false
        jump = false
        shift = false
        sprinting = false
    }
}
