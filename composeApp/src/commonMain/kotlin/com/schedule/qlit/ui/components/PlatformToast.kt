package com.schedule.qlit.ui.components

/** 轻提示。Android → Toast；iOS 暂记日志，待 iOS 壳接入 HUD。 */
internal expect fun showPlatformToast(message: String)
