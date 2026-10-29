package com.schedule.qlit.data

import android.os.Build
import com.schedule.qlit.data.store.AppContext
import com.schedule.qlit.system.DeviceUtils

internal actual fun deviceInfo(): DeviceInfo {
    val context = AppContext.context
    val packageInfo = try {
        context.packageManager.getPackageInfo(context.packageName, 0)
    } catch (_: Exception) {
        null
    }
    return DeviceInfo(
        versionName = packageInfo?.versionName ?: "",
        versionCode = packageInfo?.longVersionCode?.toString() ?: "",
        brand = Build.BRAND,
        model = Build.MODEL,
        osName = "Android",
        osVersion = Build.VERSION.RELEASE,
        sdk = Build.VERSION.SDK_INT,
        rom = "${DeviceUtils.romName} ${DeviceUtils.romVersion}".trim(),
    )
}

@Suppress("DEPRECATION")
internal actual fun deviceHardwareSeed(): String = buildString {
    append("35")
    append(Build.BOARD.length % 10)
    append(Build.BRAND.length % 10)
    append(Build.CPU_ABI.length % 10)
    append(Build.DEVICE.length % 10)
    append(Build.DISPLAY.length % 10)
    append(Build.HOST.length % 10)
    append(Build.ID.length % 10)
    append(Build.MANUFACTURER.length % 10)
    append(Build.MODEL.length % 10)
    append(Build.PRODUCT.length % 10)
    append(Build.TAGS.length % 10)
    append(Build.TYPE.length % 10)
    append(Build.USER.length % 10)
}

internal actual val platformSupportsLiquidBlur: Boolean
    get() = Build.VERSION.SDK_INT >= 33

internal actual val platformShowsUpdateCheck: Boolean = true
