package com.schedule.qlit.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.TextStyle

/**
 * Role-based text styles for the tool sub-pages. Sizes follow the Material
 * roles; the font family is cleared so text renders in the system CJK face.
 * New screens should prefer [QType].
 */
object QlitTypography {
    @Composable @ReadOnlyComposable
    private fun TextStyle.retoken(): TextStyle = copy(fontFamily = null)

    val displaySmall: TextStyle @Composable @ReadOnlyComposable get() = MaterialTheme.typography.displaySmall.retoken()
    val headlineMedium: TextStyle @Composable @ReadOnlyComposable get() = MaterialTheme.typography.headlineMedium.retoken()
    val headlineSmall: TextStyle @Composable @ReadOnlyComposable get() = MaterialTheme.typography.headlineSmall.retoken()
    val titleLarge: TextStyle @Composable @ReadOnlyComposable get() = MaterialTheme.typography.titleLarge.retoken()
    val titleMedium: TextStyle @Composable @ReadOnlyComposable get() = MaterialTheme.typography.titleMedium.retoken()
    val titleSmall: TextStyle @Composable @ReadOnlyComposable get() = MaterialTheme.typography.titleSmall.retoken()
    val bodyLarge: TextStyle @Composable @ReadOnlyComposable get() = MaterialTheme.typography.bodyLarge.retoken()
    val bodyMedium: TextStyle @Composable @ReadOnlyComposable get() = MaterialTheme.typography.bodyMedium.retoken()
    val bodySmall: TextStyle @Composable @ReadOnlyComposable get() = MaterialTheme.typography.bodySmall.retoken()
    val labelLarge: TextStyle @Composable @ReadOnlyComposable get() = MaterialTheme.typography.labelLarge.retoken()
    val labelMedium: TextStyle @Composable @ReadOnlyComposable get() = MaterialTheme.typography.labelMedium.retoken()
    val labelSmall: TextStyle @Composable @ReadOnlyComposable get() = MaterialTheme.typography.labelSmall.retoken()
}
