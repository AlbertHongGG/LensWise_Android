package com.lenswise.app.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

// 移植自參考專案 (LensWise / Jakubantalik's ThinkingOrb 3D 幾何投影)
private fun hashD(a: Double, b: Double): Double {
    val h = sin(a * 12.9898 + b * 78.233) * 43758.5453
    return h - floor(h)
}

private class Move(val axis: Int, val lo: Double, val hi: Double, val ang: Double)
private class Sc(val amount: DoubleArray, val active: Int)

private fun solveCycle(time: Double, count: Int, slotDur: Double, rest: Double): Sc {
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
            for (i in 0 until slot) amount[i] = 1.0
            amount[slot] = ep
            active = slot
        } else {
            val u = 2 * count - 1 - slot
            for (i in 0 until u) amount[i] = 1.0
            amount[u] = 1.0 - ep
            active = u
        }
    }
    return Sc(amount, active)
}

private fun makeMoves(count: Int): List<Move> {
    val moves = mutableListOf<Move>()
    for (i in 0 until count) {
        val axis = min(2, floor(hashD(i.toDouble(), 2.3) * 3).toInt())
        val lo = -1.0 + 0.5 * min(3, floor(hashD(i.toDouble(), 5.9) * 4).toInt())
        val dir = if (hashD(i.toDouble(), 7.7) < 0.5) 1.0 else -1.0
        moves.add(Move(axis, lo, lo + 0.5, dir * PI / 2))
    }
    return moves
}

private class Pt3Res(val x: Double, val y: Double, val z: Double, val inActive: Boolean)

private fun applyMoves(px: Double, py: Double, pz: Double, moves: List<Move>, sc: Sc): Pt3Res {
    var x = px
    var y = py
    var z = pz
    var inActive = false
    for (i in moves.indices) {
        if (sc.amount[i] <= 0) continue
        val mv = moves[i]
        val coord = if (mv.axis == 0) x else if (mv.axis == 1) y else z
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

private class Dot(val x: Float, val y: Float, val z: Float, val r: Float, val white: Float)

@Composable
fun ThinkingOrb(
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    isDark: Boolean = true
) {
    val transition = rememberInfiniteTransition(label = "ThinkingOrbLoop")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 60f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 33000, easing = LinearEasing)
        ),
        label = "t"
    )

    val moves = remember { makeMoves(14) }
    val latRings = 9
    val lonDensity = 24

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f
        val radius = (w / 2f) * 0.82f

        val yaw = (t * 1.82 * 0.55)
        val tilt = (0.35 + 0.1 * sin(t * 1.82 * 0.9))
        val st = sin(tilt)
        val ct = cos(tilt)
        val sy = sin(yaw)
        val cyw = cos(yaw)

        val rs = (w / 300.0).pow(0.6)
        val sc = solveCycle(t * 1.82, 14, 0.42, 1.2)

        val rBase = 0.6 * 1.05
        val rDepth = 1.7 * 1.05
        val rActive = 0.3 * 1.05
        val inkFar = 0.62
        val inkSpan = 0.54

        val dots = mutableListOf<Dot>()

        for (li in 0..latRings) {
            val lat = -PI / 2 + (li.toDouble() / latRings) * PI
            val cosLat = cos(lat)
            val sinLat = sin(lat)
            val lonCount = max(1, (kotlin.math.abs(cosLat) * lonDensity).roundToInt())
            for (lj in 0 until lonCount) {
                val lon = (lj.toDouble() / lonCount) * 2 * PI
                val res = applyMoves(
                    cosLat * cos(lon),
                    sinLat,
                    cosLat * sin(lon),
                    moves,
                    sc
                )

                val x1 = res.x * cyw + res.z * sy
                val z1 = -res.x * sy + res.z * cyw
                val y1 = res.y * ct - z1 * st
                val z2 = res.y * st + z1 * ct

                val px = (cx + x1 * radius).toFloat()
                val py = (cy - y1 * radius).toFloat()
                val depth = (z2 + 1.0) / 2.0

                val r = ((rBase + rDepth * depth + (if (res.inActive) rActive else 0.0)) * rs).toFloat()
                val whiteVal = (inkFar - inkSpan * depth - (if (res.inActive) 0.14 else 0.0)).toFloat()

                dots.add(Dot(px, py, z2.toFloat(), r, whiteVal))
            }
        }

        dots.sortBy { it.z }

        for (d in dots) {
            val clampedW = max(0f, min(1f, d.white))
            val g = if (isDark) 1f - clampedW else clampedW
            val color = Color(red = g, green = g, blue = g)
            drawCircle(
                color = color,
                radius = max(0.5f, d.r),
                center = Offset(d.x, d.y)
            )
        }
    }
}
