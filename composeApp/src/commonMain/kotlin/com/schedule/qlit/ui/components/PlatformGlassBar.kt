package com.schedule.qlit.ui.components

/** iOS 底栏由 SwiftUI 层的原生 UITabBarController 提供，Compose 侧不渲染。 */
internal expect val platformUsesNativeBarGlass: Boolean

/** 应用内 tab 选中变化时通知平台层（iOS 驱动原生 tab bar 的 selectedIndex）。 */
internal expect fun platformNotifyTabChanged(index: Int)
