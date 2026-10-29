package com.schedule.qlit.ui.screens

/** iOS 展示与 Android 相同的工具入口（超星待办/淘宝取件均有 common 实现）。 */
internal actual fun platformToolRoutes(): Set<ToolsDestination> = setOf(
    ToolsDestination.TAOBAO_PICKUP,
    ToolsDestination.CHAOXING,
)
