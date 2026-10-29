package com.schedule.qlit.data.store

/** 设置页「接收更新」开关。iOS 只存标志位（更新走 App Store）。 */
expect fun platformIsUpdateCheckEnabled(): Boolean

expect fun platformSetUpdateCheckEnabled(enabled: Boolean)
