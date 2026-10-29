package com.schedule.qlit.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import top.yukonga.miuix.kmp.theme.Colors
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.material3.LocalContentColor as MaterialLocalContentColor
import top.yukonga.miuix.kmp.theme.darkColorScheme as miuixDarkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme as miuixLightColorScheme

/** Light / dark preference stored in `ui_preferences` → `theme_mode`. */
enum class ThemeMode(val value: String, val label: String) {
    System("system", "跟随系统"),
    Light("light", "浅色"),
    Dark("dark", "深色");

    companion object {
        fun fromValue(value: String?): ThemeMode = entries.firstOrNull { it.value == value } ?: System
    }
}

@Composable
fun QlitTheme(
    themeMode: ThemeMode = ThemeMode.System,
    content: @Composable () -> Unit,
) {
    // 返回事件必须走 Activity 的 NavigationEventDispatcher（由 ComponentActivity
    // 接到系统 OnBackInvokedDispatcher）。禁止在这里用
    // rememberNavigationEventDispatcherOwner(parent = null) 自建根分发器再 provides：
    // 没有输入桥的分发器收不到任何系统返回事件，所有 BackHandler 形同虚设，
    // 返回手势/按键会直接结束 Activity（Android 16 预测性返回下必然复现）。
    val darkTheme = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val palette = if (darkTheme) DarkQlitPalette else LightQlitPalette
    val miuixColors = remember(palette) { palette.toMiuixColors() }
    val materialColors = remember(palette) { palette.toMaterialColors() }

    resolveNumFontFamily()
    ApplySystemBarAppearance(darkTheme)

    MiuixTheme(colors = miuixColors) {
        // Material components that remain (WebView chrome, a few text fields)
        // read the same palette through this bridge.
        MaterialTheme(colorScheme = materialColors, typography = Typography) {
            CompositionLocalProvider(
                LocalQlitPalette provides palette,
                LocalContentColor provides palette.ink,
                MaterialLocalContentColor provides palette.ink,
                content = content,
            )
        }
    }
}

private fun QlitPalette.solidLine(): Color = line.compositeOver(surface)

private fun QlitPalette.toMiuixColors(): Colors {
    val errorContainer = sealSoft.compositeOver(surface)
    val onErrorContainer = if (isDark) Color(0xFFFFB3B0) else Color(0xFF7A0C11)
    val disabled = if (isDark) Color(0xFF3A3632) else Color(0xFFD6D2CB)
    val track = if (isDark) Color(0xFF3A3632) else Color(0xFFDCD7CF)
    val base = if (isDark) miuixDarkColorScheme() else miuixLightColorScheme()
    // Controls (switch, primary button, slider) are drawn in ink; seal red is
    // only the error/attention role so it keeps meaning "right now".
    return base.copy(
        primary = ink,
        onPrimary = surface,
        primaryVariant = ink,
        onPrimaryVariant = ink2,
        error = seal,
        onError = onSeal,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        disabledPrimary = disabled,
        disabledOnPrimary = surface2,
        disabledPrimaryButton = disabled,
        disabledOnPrimaryButton = surface,
        disabledPrimarySlider = disabled,
        primaryContainer = surface2,
        onPrimaryContainer = ink,
        secondary = track,
        onSecondary = surface,
        secondaryVariant = surface2,
        onSecondaryVariant = ink,
        disabledSecondary = surface2,
        disabledOnSecondary = ink3,
        disabledSecondaryVariant = surface2,
        disabledOnSecondaryVariant = ink3,
        secondaryContainer = surface2,
        onSecondaryContainer = ink2,
        secondaryContainerVariant = surface2,
        onSecondaryContainerVariant = ink3,
        tertiaryContainer = surface2,
        onTertiaryContainer = ink,
        tertiaryContainerVariant = surface2,
        background = bg,
        onBackground = ink,
        onBackgroundVariant = ink3,
        surface = bg,
        onSurface = ink,
        surfaceVariant = surface,
        onSurfaceSecondary = ink2,
        onSurfaceVariantSummary = ink2,
        onSurfaceVariantActions = ink3,
        disabledOnSurface = ink3,
        surfaceContainer = surface,
        onSurfaceContainer = ink,
        onSurfaceContainerVariant = ink3,
        surfaceContainerHigh = surface2,
        onSurfaceContainerHigh = ink3,
        surfaceContainerHighest = surface2,
        onSurfaceContainerHighest = ink,
        outline = if (isDark) Color(0xFF3A3632) else Color(0xFFDDD8D0),
        dividerLine = solidLine(),
        windowDimming = scrim,
        sliderKeyPoint = ink3.copy(alpha = 0.3f),
        sliderKeyPointForeground = ink,
        sliderBackground = line,
    )
}

private fun QlitPalette.toMaterialColors(): ColorScheme {
    val errorContainer = sealSoft.compositeOver(surface)
    val onErrorContainer = if (isDark) Color(0xFFFFB3B0) else Color(0xFF7A0C11)
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = ink,
        onPrimary = surface,
        primaryContainer = surface2,
        onPrimaryContainer = ink,
        inversePrimary = ink2,
        secondary = ink2,
        onSecondary = surface,
        secondaryContainer = surface2,
        onSecondaryContainer = ink,
        tertiary = ink2,
        onTertiary = surface,
        tertiaryContainer = surface2,
        onTertiaryContainer = ink,
        background = bg,
        onBackground = ink,
        surface = bg,
        onSurface = ink,
        surfaceVariant = surface2,
        onSurfaceVariant = ink2,
        surfaceTint = ink,
        inverseSurface = ink,
        inverseOnSurface = surface,
        error = seal,
        onError = onSeal,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        outline = ink3,
        outlineVariant = solidLine(),
        scrim = scrim,
        surfaceBright = surface,
        surfaceDim = bg,
        surfaceContainer = bg,
        surfaceContainerHigh = surface2,
        surfaceContainerHighest = surface2,
        surfaceContainerLow = surface,
        surfaceContainerLowest = surface,
    )
}
