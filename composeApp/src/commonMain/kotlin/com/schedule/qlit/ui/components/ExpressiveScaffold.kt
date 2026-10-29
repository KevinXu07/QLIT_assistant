package com.schedule.qlit.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.schedule.qlit.ui.theme.Q
import top.yukonga.miuix.kmp.basic.FabPosition as MiuixFabPosition
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.ToolbarPosition as MiuixToolbarPosition

/** Extra space reserved by scrollable content for the overlay floating bar. */
val LocalFloatingBottomBarExtraPadding = staticCompositionLocalOf<Dp> { 0.dp }

/** Bottom clearance for FABs anchored above the bottom navigation. */
val LocalFloatingActionButtonClearance = staticCompositionLocalOf<Dp> { 0.dp }

/** Page scaffold on the warm paper background. Hosts Miuix overlay dialogs/popups. */
@Composable
fun ExpressiveScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    miuixFloatingActionButtonPosition: MiuixFabPosition = MiuixFabPosition.End,
    miuixFloatingToolbar: @Composable () -> Unit = {},
    miuixFloatingToolbarPosition: MiuixToolbarPosition = MiuixToolbarPosition.BottomCenter,
    containerColor: Color = Q.colors.bg,
    contentWindowInsets: WindowInsets = ExpressiveContentInsets,
    content: @Composable (PaddingValues) -> Unit,
) {
    val floatingBottomBarClearance = LocalFloatingActionButtonClearance.current
    MiuixScaffold(
        modifier = modifier,
        topBar = topBar,
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = miuixFloatingActionButtonPosition,
        floatingToolbar = miuixFloatingToolbar,
        floatingToolbarPosition = miuixFloatingToolbarPosition,
        snackbarHost = {
            Box(modifier = Modifier.padding(bottom = floatingBottomBarClearance)) {
                snackbarHost()
            }
        },
        containerColor = containerColor,
        contentWindowInsets = contentWindowInsets,
        content = content,
    )
}

val ExpressiveContentInsets: WindowInsets
    @Composable
    get() = WindowInsets.safeDrawing.only(
        WindowInsetsSides.Top + WindowInsetsSides.Horizontal
    )
