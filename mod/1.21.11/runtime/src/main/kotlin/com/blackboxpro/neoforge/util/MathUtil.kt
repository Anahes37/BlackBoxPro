package com.blackboxpro.neoforge.util

import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * 计算从观察者到目标的 yaw 和 pitch。
 * @param dx 目标 X - 观察者 X
 * @param dy 目标 Y - 观察者 eyeY
 * @param dz 目标 Z - 观察者 Z
 * @return Pair(yaw, pitch)
 */
fun calculateYawPitch(dx: Double, dy: Double, dz: Double): Pair<Float, Float> {
    val horizontalDist = sqrt(dx * dx + dz * dz)
    val yaw = (-atan2(dx, dz) * 180.0 / Math.PI).toFloat()
    val pitch = (-atan2(dy, horizontalDist) * 180.0 / Math.PI).toFloat().coerceIn(-90f, 90f)
    return yaw to pitch
}
