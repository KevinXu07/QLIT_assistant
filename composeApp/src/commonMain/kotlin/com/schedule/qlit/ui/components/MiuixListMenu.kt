package com.schedule.qlit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.DropdownColors
import top.yukonga.miuix.kmp.basic.DropdownDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 锚定到图标下方展开的菜单定位器，移植自 SukiSU-Ultra 的
 * ListPopupDefaults.MenuPositionProvider（Apache License 2.0）。
 */
object MiuixMenuDefaults {
    val BelowAnchorPositionProvider = object : PopupPositionProvider {
        override fun calculatePosition(
            anchorBounds: IntRect,
            windowBounds: IntRect,
            layoutDirection: LayoutDirection,
            popupContentSize: IntSize,
            popupMargin: IntRect,
            alignment: PopupPositionProvider.Align,
        ): IntOffset {
            val below = windowBounds.bottom - anchorBounds.bottom > popupContentSize.height
            val offsetY = if (below) {
                anchorBounds.bottom + popupMargin.top
            } else {
                anchorBounds.top - popupContentSize.height - popupMargin.bottom
            }
            val offsetX = (anchorBounds.right - popupContentSize.width - popupMargin.right)
                .coerceAtLeast(windowBounds.left)
            return IntOffset(
                x = offsetX.coerceIn(
                    windowBounds.left,
                    (windowBounds.right - popupContentSize.width - popupMargin.right)
                        .coerceAtLeast(windowBounds.left),
                ),
                y = offsetY.coerceIn(
                    (windowBounds.top + popupMargin.top)
                        .coerceAtMost(windowBounds.bottom - popupContentSize.height - popupMargin.bottom),
                    windowBounds.bottom - popupContentSize.height - popupMargin.bottom,
                ),
            )
        }

        override fun getMargins(): PaddingValues = PaddingValues(start = 20.dp, end = 20.dp)
    }
}

/** Miuix 风格的弹出菜单行：图标 + 文字，首尾行加大内边距贴合官方 Dropdown 观感 */
@Composable
fun MiuixMenuRow(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    optionSize: Int = 1,
    index: Int = 0,
    dropdownColors: DropdownColors = DropdownDefaults.dropdownColors(),
) {
    val currentOnClick by rememberUpdatedState(onClick)
    val topPadding = if (index == 0) 14.dp else 11.dp
    val bottomPadding = if (index == optionSize - 1) 14.dp else 11.dp
    Row(
        modifier = modifier
            .clickable { currentOnClick() }
            .background(dropdownColors.containerColor)
            .padding(horizontal = 20.dp)
            .padding(top = topPadding, bottom = bottomPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = dropdownColors.contentColor.copy(alpha = 0.9f),
                modifier = Modifier.width(20.dp),
            )
            Spacer(Modifier.width(12.dp))
        }
        Text(
            text = text,
            fontSize = MiuixTheme.textStyles.body1.fontSize,
            color = dropdownColors.contentColor,
        )
    }
}
