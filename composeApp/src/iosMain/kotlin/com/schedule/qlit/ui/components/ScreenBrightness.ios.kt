package com.schedule.qlit.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import platform.UIKit.UIScreen

@Composable
internal actual fun ScreenBrightnessBoost(active: Boolean) {
    DisposableEffect(active) {
        if (!active) return@DisposableEffect onDispose { }
        val screen = UIScreen.mainScreen
        val previous = screen.brightness
        screen.brightness = 1.0
        onDispose { screen.brightness = previous }
    }
}
