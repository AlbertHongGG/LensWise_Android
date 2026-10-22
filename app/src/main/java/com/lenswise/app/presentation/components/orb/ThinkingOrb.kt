package com.lenswise.app.presentation.components.orb

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 3D 旋轉粒子思考球 (ThinkingOrb)。
 * 1:1 復刻自 EquityTracker / LensWise 之 thinking_orb.dart (Rubik Mode 幾何投影架構)：
 * - 3D 軸向旋轉與透視幾何 (makeProj)
 * - 週期性魔術方塊切片扭轉模型 (solveCycle & applyMoves)
 * - 深度明暗與半徑投影 (Depth-based radius and ink shading)
 */
private fun hashD(a: Double, b: Double): Double {
    val h = sin(a * 12.9898 + b * 78.233) * 43758.5453
    return h - floor(h)
}

private class OrbMove(
    val axis: Int,
    val lo: Double,
    val hi: Double,
    val ang: Double
)

private class OrbSc(
    val amount: DoubleArray,
    val active: Int
)

private fun solveCycle(time: Double, count: Int, slotDur: Double, rest: Double): OrbSc {
    val cyc = 2 * count * slotDur + rest
    val tc = time % cyc
    val amount = DoubleArray(count) { 0.0 }
    var active = -1
    if (tc < 2 * count * slotDur) {
        val slot = floor(tc / slotDur).toInt()
        val p = (tc - slot * slotDur) / slotDur
        val cl = min(1.0, p / 0.7)
        val ep = 1.0 - (1.0 - cl).pow(3.0)
        if (slot < count) {
            for (i in 0 until slot) {
                amount[i] = 1.0
            }
            amount[slot] = ep
            active = slot
        } else {
            val u = 2 * count - 1 - slot
            for (i in 0 until u) {
                amount[i] = 1.0
            }
            amount[u] = 1.0 - ep
            active = u
        }
    }
    return OrbSc(amount, active)
}

private fun makeMoves(count: Int): List<OrbMove> {
    val moves = ArrayList<OrbMove>(count)
    for (i in 0 until count) {
        val axis = min(2, floor(hashD(i.toDouble(), 2.3) * 3).toInt())
        val lo = -1.0 + 0.5 * min(3, floor(hashD(i.toDouble(), 5.9) * 4).toInt())
        val dir = if (hashD(i.toDouble(), 7.7) < 0.5) 1.0 else -1.0
        moves.add(OrbMove(axis, lo, lo + 0.5, dir * (PI / 2)))
    }
    return moves
}

private class Pt3Res(
    val x: Double,
    val y: Double,
    val z: Double,
    val inActive: Boolean
)

private fun applyMoves(px: Double, py: Double, pz: Double, moves: List<OrbMove>, sc: OrbSc): Pt3Res {
    var x = px
    var y = py
    var z = pz
    var inActive = false
    for (i in moves.indices) {
        if (sc.amount[i] <= 0) continue
        val mv = moves[i]
        val coord = when (mv.axis) {
            0 -> x
            1 -> y
            else -> z
        }
        if (coord < mv.lo || coord >= mv.hi) continue
        if (i == sc.active) inActive = true
        val a = mv.ang * sc.amount[i]
        val ca = cos(a)
        val sa = sin(a)
        when (mv.axis) {
            0 -> {
                val y2 = y * ca - z * sa
                z = y * sa + z * ca
                y = y2
            }
            1 -> {
                val x2 = x * ca + z * sa
                z = -x * sa + z * ca
                x = x2
            }
            else -> {
                val x2 = x * ca - y * sa
                y = x * sa + y * ca
                x = x2
            }
        }
    }
    return Pt3Res(x, y, z, inActive)
}

private class Dot(
    val x: Float,
    val y: Float,
    val z: Double,
    val r: Float,
    val white: Double
)

@Composable
fun ThinkingOrb(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    isDark: Boolean = isSystemInDarkTheme(),
    speed: Float = 1.82f
) {
    val latRings = 9
    val lonDensity = 24
    val moveCount = 14
    val moves = remember { makeMoves(moveCount) }

    val infiniteTransition = rememberInfiniteTransition(label = "ThinkingOrbTransition")
    // 週期 100 秒的線性時間軸
    val t by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = ((100f / speed) * 1000f).roundToInt(),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "t"
    )

    Canvas(modifier = modifier.size(size)) {
        val width = size.toPx()
        val height = size.toPx()
        val cx = width / 2f
        val cy = height / 2f
        val rSphere = (width / 2f) * 0.82f

        val timeDouble = t.toDouble()
        val yaw = timeDouble * 0.55
        val tilt = 0.35 + 0.1 * sin(timeDouble * 0.9)

        val st = sin(tilt)
        val ct = cos(tilt)
        val sy = sin(yaw)
        val cyw = cos(yaw)

        val rs = (width / 300.0).pow(0.6).toFloat()
        val sc = solveCycle(timeDouble, 14, 0.42, 1.2)

        val dots = ArrayList<Dot>(latRings * lonDensity)

        val rBase = (0.6 * 1.05).toFloat()
        val rDepth = (1.7 * 1.05).toFloat()
        val rActive = (0.3 * 1.05).toFloat()

        val inkFar = 0.62
        val inkSpan = 0.54

        for (li in 0..latRings) {
            val lat = -PI / 2.0 + (li.toDouble() / latRings) * PI
            val cosLat = cos(lat)
            val sinLat = sin(lat)
            val lonCount = max(1, (abs(cosLat) * lonDensity).roundToInt())
            for (lj in 0 until lonCount) {
                val lon = (lj.toDouble() / lonCount) * 2.0 * PI
                val px = cosLat * cos(lon)
                val py = sinLat
                val pz = cosLat * sin(lon)

                val res = applyMoves(px, py, pz, moves, sc)

                // 3D 幾何投影
                val x1 = res.x * cyw + res.z * sy
                val z1 = -res.x * sy + res.z * cyw
                val y1 = res.y * ct - z1 * st
                val z2 = res.y * st + z1 * ct

                val projX = (cx + x1 * rSphere).toFloat()
                val projY = (cy - y1 * rSphere).toFloat()
                val depth = ((z2 + 1.0) / 2.0).coerceIn(0.0, 1.0)

                val r = (rBase + rDepth * depth.toFloat() + if (res.inActive) rActive else 0f) * rs
                val white = inkFar - inkSpan * depth - (if (res.inActive) 0.14 else 0.0)

                dots.add(Dot(x = projX, y = projY, z = z2, r = r, white = white))
            }
        }

        // Z-Index 景深排序 (由遠及近繪製)
        dots.sortBy { it.z }

        val rMin = 0.3f
        for (d in dots) {
            val w = d.white.coerceIn(0.0, 1.0)
            val g = if (isDark) (1.0 - w) else w
            val gFloat = g.toFloat().coerceIn(0f, 1f)
            val dotColor = Color(red = gFloat, green = gFloat, blue = gFloat, alpha = 1f)
            drawCircle(
                color = dotColor,
                radius = max(rMin, d.r),
                center = Offset(d.x, d.y)
            )
        }
    }
}
