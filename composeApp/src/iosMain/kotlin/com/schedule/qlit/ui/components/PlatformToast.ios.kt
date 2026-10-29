package com.schedule.qlit.ui.components

import platform.Foundation.NSLog
import platform.Foundation.NSString

internal actual fun showPlatformToast(message: String) {
    // iOS 壳落地后换成 in-app HUD
    NSLog("%@", "toast: $message" as NSString)
}
