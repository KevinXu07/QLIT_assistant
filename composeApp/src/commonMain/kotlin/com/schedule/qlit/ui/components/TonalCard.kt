package com.schedule.qlit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.schedule.qlit.ui.design.qClickable
import com.schedule.qlit.ui.theme.Q
import androidx.compose.material3.LocalContentColor as MaterialLocalContentColor
import top.yukonga.miuix.kmp.theme.LocalContentColor

/** White rounded card with a spring press (no ripple). */
@Composable
fun TonalCard(
    modifier: Modifier = Modifier,
    containerColor: Color = Q.colors.surface,
    contentColor: Color = contentColorFor(containerColor),
    shape: Shape = RoundedCornerShape(24.dp),
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val resolvedContent = if (contentColor == Color.Unspecified) Q.colors.ink else contentColor
    CompositionLocalProvider(
        LocalContentColor provides resolvedContent,
        MaterialLocalContentColor provides resolvedContent,
    ) {
        Box(
            modifier = modifier
                .then(
                    if (onClick != null || onLongClick != null) {
                        Modifier.qClickable(enabled = enabled, pressScale = 0.98f, onLongClick = onLongClick, onClick = onClick ?: {})
                    } else {
                        Modifier
                    },
                )
                .clip(shape)
                .background(containerColor),
        ) { content() }
    }
}
