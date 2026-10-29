package com.schedule.qlit.ui.components

import androidx.compose.runtime.Composable

/**
 * 身份码亮屏增强：active 期间把屏幕亮度拉到最高，退出后恢复原值。
 * Android → window.screenBrightness；iOS → UIScreen.brightness。
 */
@Composable
internal expect fun ScreenBrightnessBoost(active: Boolean)
