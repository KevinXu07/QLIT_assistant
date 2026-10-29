package com.schedule.qlit.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * OKLCH → sRGB. The course/tool palette keeps lightness and chroma fixed and
 * only rotates hue, so every tint has the same visual weight.
 */
fun oklch(l: Float, c: Float, hueDegrees: Float, alpha: Float = 1f): Color {
    val h = hueDegrees * PI / 180.0
    val a = c * cos(h)
    val b = c * sin(h)
    val l1 = l + 0.3963377774 * a + 0.2158037573 * b
    val m1 = l - 0.1055613458 * a - 0.0638541728 * b
    val s1 = l - 0.0894841775 * a - 1.2914855480 * b
    val lc = l1 * l1 * l1
    val mc = m1 * m1 * m1
    val sc = s1 * s1 * s1
    val r = 4.0767416621 * lc - 3.3077115913 * mc + 0.2309699292 * sc
    val g = -1.2684380046 * lc + 2.6097574011 * mc - 0.3413193965 * sc
    val bl = -0.0041960863 * lc - 0.7034186147 * mc + 1.7076147010 * sc
    return Color(encode(r), encode(g), encode(bl), alpha)
}

private fun encode(linear: Double): Float {
    val v = linear.coerceIn(0.0, 1.0)
    val srgb = if (v <= 0.0031308) 12.92 * v else 1.055 * v.pow(1 / 2.4) - 0.055
    return srgb.toFloat().coerceIn(0f, 1f)
}
