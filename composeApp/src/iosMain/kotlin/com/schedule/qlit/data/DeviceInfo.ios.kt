package com.schedule.qlit.data

import platform.Foundation.NSBundle
import platform.UIKit.UIDevice

internal actual fun deviceInfo(): DeviceInfo {
    val versionName = NSBundle.mainBundle
        .objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: ""
    return DeviceInfo(
        versionName = versionName,
        versionCode = NSBundle.mainBundle
            .objectForInfoDictionaryKey("CFBundleVersion") as? String ?: "",
        brand = "Apple",
        model = UIDevice.currentDevice.model,
        osName = UIDevice.currentDevice.systemName,
        osVersion = UIDevice.currentDevice.systemVersion,
        sdk = 0,
        rom = "",
    )
}

internal actual fun deviceHardwareSeed(): String {
    val device = UIDevice.currentDevice
    val vendor = device.identifierForVendor?.UUIDString ?: ""
    return buildString {
        append("35")
        append(device.model.length % 10)
        append(device.systemName.length % 10)
        append(device.systemVersion.length % 10)
        append(device.name.length % 10)
        append(vendor.length % 10)
        append((device.userInterfaceIdiom % 10).toInt())
        append(device.batteryLevel.toString().length % 10)
        append(device.localizedModel.length % 10)
        append(vendor.getOrNull(0)?.digitToIntOrNull() ?: 0)
        append(vendor.getOrNull(8)?.digitToIntOrNull() ?: 0)
        append(vendor.getOrNull(16)?.digitToIntOrNull() ?: 0)
        append(vendor.getOrNull(24)?.digitToIntOrNull() ?: 0)
    }
}

internal actual val platformSupportsLiquidBlur: Boolean = true

internal actual val platformShowsUpdateCheck: Boolean = false
