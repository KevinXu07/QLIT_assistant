package com.schedule.qlit.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.schedule.qlit.ui.components.LocalFloatingBottomBarExtraPadding
import com.schedule.qlit.ui.theme.Q
import com.schedule.qlit.ui.theme.QType
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import top.yukonga.miuix.kmp.window.WindowDialog
import kotlin.math.roundToInt

/**
 * Page header: a 52dp bar (back / actions / collapsed title) above a large
 * left-aligned title that folds away as content scrolls.
 */
@Composable
fun QTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    scrollBehavior: ScrollBehavior? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    color: Color = Q.colors.bg,
    largeTitle: Boolean = true,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(color)
            .windowInsetsPadding(
                WindowInsets.statusBars.union(WindowInsets.displayCutout)
                    .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(48.dp), contentAlignment = Alignment.CenterStart) { navigationIcon() }
            QText(
                text = title,
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer {
                        alpha = if (!largeTitle) {
                            1f
                        } else {
                            val fraction = scrollBehavior?.state?.collapsedFraction ?: 0f
                            ((fraction - 0.55f) / 0.4f).coerceIn(0f, 1f)
                        }
                    },
                style = QType.section,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Row(
                modifier = Modifier.width(IntrinsicActionWidth),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
        if (largeTitle) {
            Layout(
                content = {
                    Column(Modifier.padding(start = 22.dp, end = 22.dp, top = 2.dp, bottom = 12.dp)) {
                        QText(title, style = QType.pageTitle, maxLines = 2)
                        if (!subtitle.isNullOrBlank()) {
                            QText(subtitle, modifier = Modifier.padding(top = 3.dp), style = QType.sub, color = Q.colors.ink2)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clipToBounds()
                    .graphicsLayer {
                        val fraction = scrollBehavior?.state?.collapsedFraction ?: 0f
                        alpha = (1f - fraction * 1.6f).coerceIn(0f, 1f)
                    },
            ) { measurables, constraints ->
                val placeable = measurables.first().measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
                val state = scrollBehavior?.state
                if (state != null) {
                    val limit = -placeable.height.toFloat()
                    if (state.heightOffsetLimit != limit) state.heightOffsetLimit = limit
                }
                val height = (placeable.height + (state?.heightOffset ?: 0f)).roundToInt().coerceIn(0, placeable.height)
                layout(constraints.maxWidth, height) { placeable.place(0, height - placeable.height) }
            }
        }
    }
}

private val IntrinsicActionWidth = 96.dp

/** Bottom space so the last row clears the floating tab bar / gesture bar. */
@Composable
fun qBottomInset(extra: Dp = 24.dp): Dp {
    val floating = LocalFloatingBottomBarExtraPadding.current
    val nav = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    return (if (floating > 0.dp) floating else nav) + extra
}

/** Scaffold for every secondary page: back button, large collapsing title, paper background. */
@Composable
fun QPage(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    scrollBehavior: ScrollBehavior = MiuixScrollBehavior(),
    actions: @Composable RowScope.() -> Unit = {},
    largeTitle: Boolean = true,
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            QTopBar(
                title = title,
                subtitle = subtitle,
                scrollBehavior = scrollBehavior,
                largeTitle = largeTitle,
                navigationIcon = { if (onBack != null) QBackButton(onClick = onBack) },
                actions = actions,
            )
        },
        bottomBar = bottomBar,
        snackbarHost = snackbarHost,
        containerColor = Q.colors.bg,
        contentWindowInsets = WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal),
        content = content,
    )
}

/** Section heading inside a page: "今天 · 还有 3 节". */
@Composable
fun QSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    trailingColor: Color = Color.Unspecified,
    onTrailingClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 22.dp, top = 14.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        QText(title, modifier = Modifier.weight(1f), style = QType.section)
        if (trailing != null) {
            QText(
                trailing,
                modifier = if (onTrailingClick != null) Modifier.qClickable(onClick = onTrailingClick) else Modifier,
                style = QType.sub,
                color = if (trailingColor != Color.Unspecified) trailingColor else Q.colors.ink2,
            )
        }
    }
}

// ── Overlays ─────────────────────────────────────────────────────────

/** Rounded floating sheet, inset 8dp from the screen edges (window based). */
@Composable
fun QSheet(
    show: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    onDismissFinished: (() -> Unit)? = null,
    allowDismiss: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    WindowBottomSheet(
        show = show,
        modifier = modifier,
        backgroundColor = Q.colors.surface,
        cornerRadius = 34.dp,
        outsideMargin = DpSize(8.dp, 8.dp),
        insideMargin = DpSize(22.dp, 0.dp),
        dragHandleColor = Q.colors.surface2,
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished,
        allowDismiss = allowDismiss,
    ) {
        Column(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 22.dp)) {
            if (title != null) {
                QText(title, modifier = Modifier.padding(bottom = 16.dp), style = QType.sheetTitle)
            }
            content()
        }
    }
}

/** Two equal buttons at the bottom of a sheet/dialog. */
@Composable
fun QActionRow(
    confirmText: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    dismissText: String? = "取消",
    onDismiss: () -> Unit = {},
    confirmKind: QButtonKind = QButtonKind.Primary,
    confirmEnabled: Boolean = true,
    loading: Boolean = false,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 22.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (dismissText != null) {
            QButton(text = dismissText, onClick = onDismiss, modifier = Modifier.weight(1f), kind = QButtonKind.Tonal)
        }
        QButton(
            text = confirmText,
            onClick = onConfirm,
            modifier = Modifier.weight(1f),
            kind = confirmKind,
            enabled = confirmEnabled,
            loading = loading,
        )
    }
}

/** Centered dialog with the same button language as sheets. */
@Composable
fun QDialog(
    show: Boolean,
    title: String,
    onDismissRequest: () -> Unit,
    confirmText: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
    dismissText: String? = "取消",
    destructive: Boolean = false,
    confirmEnabled: Boolean = true,
    loading: Boolean = false,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    WindowDialog(
        show = show,
        modifier = modifier,
        title = title,
        titleColor = Q.colors.ink,
        summary = message,
        summaryColor = Q.colors.ink2,
        backgroundColor = Q.colors.surface,
        onDismissRequest = onDismissRequest,
        cornerRadius = 30.dp,
    ) {
        Column(Modifier.fillMaxWidth()) {
            content?.invoke(this)
            QActionRow(
                confirmText = confirmText,
                onConfirm = onConfirm,
                dismissText = dismissText,
                onDismiss = onDismissRequest,
                confirmKind = if (destructive) QButtonKind.Seal else QButtonKind.Primary,
                confirmEnabled = confirmEnabled,
                loading = loading,
            )
        }
    }
}

@Composable
internal fun QVerticalSpace(height: Dp) = Spacer(Modifier.height(height))

/** Paper fade behind the status bar for pages that scroll edge to edge. */
@Composable
fun BoxScope.QStatusBarScrim() {
    val bg = Q.colors.bg
    val height = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 10.dp
    Box(
        Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .height(height)
            .background(Brush.verticalGradient(0f to bg, 0.7f to bg, 1f to bg.copy(alpha = 0f))),
    )
}
