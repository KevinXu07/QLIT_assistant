package com.schedule.qlit.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.schedule.qlit.ui.design.QDialog
import com.schedule.qlit.ui.design.QProgressBar
import com.schedule.qlit.ui.design.QSegmented
import com.schedule.qlit.ui.design.QText
import com.schedule.qlit.ui.design.qClickable
import com.schedule.qlit.ui.theme.Q
import com.schedule.qlit.ui.theme.QType
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.Slider as MiuixSlider
import top.yukonga.miuix.kmp.basic.Surface as MiuixSurface
import top.yukonga.miuix.kmp.basic.TabRow as MiuixTabRow
import top.yukonga.miuix.kmp.basic.Text as MiuixText

/**
 * Confirm dialog in the redesign's button language (window based, so it works
 * from any composition, including app-level startup dialogs).
 */
@Composable
fun AdaptiveDialog(
    onDismissRequest: () -> Unit,
    title: String,
    show: Boolean = true,
    content: @Composable () -> Unit,
    confirmText: String,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean = true,
    destructive: Boolean = false,
    dismissText: String? = "取消",
    @Suppress("UNUSED_PARAMETER") window: Boolean = false,
    scrollableContent: Boolean = false,
) {
    val maxBodyHeight = with(LocalDensity.current) {
        LocalWindowInfo.current.containerSize.height.toDp() * 0.5f
    }
    QDialog(
        show = show,
        title = title,
        onDismissRequest = onDismissRequest,
        confirmText = confirmText,
        onConfirm = onConfirm,
        dismissText = dismissText,
        destructive = destructive,
        confirmEnabled = confirmEnabled,
    ) {
        if (scrollableContent) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxBodyHeight)
                    .verticalScroll(rememberScrollState()),
            ) { content() }
        } else {
            content()
        }
    }
}

/** Determinate progress track. */
@Composable
fun AdaptiveLinearProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color? = null,
    trackColor: Color? = null,
) {
    QProgressBar(
        progress = progress(),
        modifier = modifier,
        color = color ?: Q.colors.ink,
        trackColor = trackColor ?: Q.colors.line,
    )
}

/** Indeterminate progress track. */
@Composable
fun AdaptiveLinearProgressIndicator(
    modifier: Modifier = Modifier,
) {
    QProgressBar(progress = null, modifier = modifier)
}

/** Short tab sets render as a segmented control; long ones scroll. */
@Composable
fun AdaptiveTabRow(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") edgePadding: Dp = 16.dp,
) {
    if (tabs.size <= 4) {
        QSegmented(options = tabs, selectedIndex = selectedIndex, onSelect = onSelect, modifier = modifier)
    } else {
        MiuixTabRow(
            tabs = tabs,
            selectedTabIndex = selectedIndex,
            onTabSelected = onSelect,
            modifier = modifier,
        )
    }
}

/** Single-select pill: ink when selected, paper otherwise. */
@Composable
fun AdaptiveFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val fg = if (selected) Q.colors.surface else Q.colors.ink
    Row(
        modifier = modifier
            .qClickable(pressScale = 0.95f, onClick = onClick)
            .clip(CircleShape)
            .background(if (selected) Q.colors.ink else Q.colors.surface2)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            MiuixIcon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = fg)
            Spacer(Modifier.width(4.dp))
        }
        QText(label, style = QType.label, color = fg, maxLines = 1)
    }
}

/** Text that follows the surrounding content color unless [color] is set. */
@Composable
fun AdaptiveText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    style: TextStyle? = null,
) {
    MiuixText(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        maxLines = maxLines,
        style = style ?: QType.body,
    )
}

@Composable
fun AdaptiveSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    color: Color,
    border: BorderStroke? = null,
    @Suppress("UNUSED_PARAMETER") tonalElevation: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    MiuixSurface(
        modifier = modifier,
        shape = shape,
        color = color,
        border = border,
    ) { content() }
}

@Composable
fun AdaptiveIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
) {
    if (tint == Color.Unspecified) {
        MiuixIcon(imageVector = imageVector, contentDescription = contentDescription, modifier = modifier)
    } else {
        MiuixIcon(imageVector = imageVector, contentDescription = contentDescription, modifier = modifier, tint = tint)
    }
}

@Composable
fun AdaptiveIcon(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
) {
    if (tint == Color.Unspecified) {
        MiuixIcon(painter = painter, contentDescription = contentDescription, modifier = modifier)
    } else {
        MiuixIcon(painter = painter, contentDescription = contentDescription, modifier = modifier, tint = tint)
    }
}

@Composable
fun AdaptiveSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    MiuixSlider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
    )
}
