package com.schedule.qlit.ui.components

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Page motion used by the Miuix route/content layers.
 *
 * This mirrors the default transition in Miuix Navigation3: a 500ms damped
 * spring-like slide, with the outgoing page moving only a quarter of the
 * distance so the new page has a heavier, layered feel.
 */
internal fun miuixPageTransition(forward: Boolean): ContentTransform {
    return if (forward) {
        slideInHorizontally(
            initialOffsetX = { width -> width },
            animationSpec = tween(durationMillis = 500, easing = MiuixNavEasing),
        ) togetherWith slideOutHorizontally(
            targetOffsetX = { width -> -width / 4 },
            animationSpec = tween(durationMillis = 500, easing = MiuixNavEasing),
        )
    } else {
        slideInHorizontally(
            initialOffsetX = { width -> -width / 4 },
            animationSpec = tween(durationMillis = 500, easing = MiuixNavEasing),
        ) togetherWith slideOutHorizontally(
            targetOffsetX = { width -> width },
            animationSpec = tween(durationMillis = 500, easing = MiuixNavEasing),
        )
    }
}

private class MiuixNavigationEasing(
    response: Float,
    damping: Float,
) : Easing {
    private val r: Float
    private val w: Float
    private val c2: Float

    init {
        val omega = 2.0 * PI / response
        val k = omega * omega
        val c = damping * 4.0 * PI / response
        w = (sqrt(4.0 * k - c * c) / 2.0).toFloat()
        r = (-c / 2.0).toFloat()
        c2 = r / w
    }

    override fun transform(fraction: Float): Float {
        val t = fraction.toDouble()
        val decay = exp(r * t)
        return (decay * (-cos(w * t) + c2 * sin(w * t)) + 1.0).toFloat()
    }
}

private val MiuixNavEasing = MiuixNavigationEasing(response = 0.8f, damping = 0.95f)
