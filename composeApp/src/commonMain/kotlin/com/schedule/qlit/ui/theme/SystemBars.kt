package com.schedule.qlit.ui.theme

import androidx.compose.runtime.Composable

/** 主题切换时同步系统状态栏/导航栏图标明暗。iOS 侧目前为空实现。 */
@Composable
expect fun ApplySystemBarAppearance(darkTheme: Boolean)
