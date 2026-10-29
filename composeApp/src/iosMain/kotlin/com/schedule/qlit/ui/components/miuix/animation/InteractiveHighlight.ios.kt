package com.schedule.qlit.ui.components.miuix.animation

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.util.fastCoerceIn

internal actual fun InteractiveHighlight.glowModifier(): Modifier =
    Modifier.drawWithContent {
        val progress = pressProgressAnimation.value
        if (progress > 0f) {
            drawRect(
                Color.White.copy(0.06f * progress),
                blendMode = BlendMode.Plus
            )
            val pos = position(size, positionAnimation.value)
            val center = Offset(
                pos.x.fastCoerceIn(0f, size.width),
                pos.y.fastCoerceIn(0f, size.height),
            )
            // AGSL smoothstep 辉光的近似：径向渐变到同色透明端
            drawRect(
                Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(0.12f * progress),
                        Color.White.copy(alpha = 0f),
                    ),
                    center = center,
                    radius = size.minDimension * 1.2f,
                ),
                blendMode = BlendMode.Plus
            )
        }

        drawContent()
    }
