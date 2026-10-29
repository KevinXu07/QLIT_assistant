package com.schedule.qlit.ui.components

import androidx.compose.runtime.Composable

/**
 * "检查更新"入口行。Android：渲染 QRow + 更新对话框；
 * iOS：更新只走 App Store，渲染空（调用处用 platformShowsUpdateCheck 跳过分隔线）。
 */
@Composable
internal expect fun PlatformUpdateEntry()
