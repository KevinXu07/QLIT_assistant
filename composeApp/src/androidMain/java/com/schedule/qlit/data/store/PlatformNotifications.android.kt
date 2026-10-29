package com.schedule.qlit.data.store

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.schedule.qlit.notification.LiveUpdateHelper
import com.schedule.qlit.notification.XiaomiIslandHelper
import com.schedule.qlit.system.DeviceUtils
import com.schedule.qlit.system.ShizukuManager

private val context get() = AppContext.context

internal actual suspend fun notificationsPermitted(): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    else true

internal actual suspend fun promotedNotificationsPermitted(): Boolean =
    if (Build.VERSION.SDK_INT >= 36) NotificationManagerCompat.from(context).canPostPromotedNotifications() else false

internal actual val platformIsXiaomiIslandCapable: Boolean
    get() = DeviceUtils.supportsXiaomiIsland

internal actual fun islandShizukuMissing(): Boolean =
    DeviceUtils.supportsXiaomiIsland && ShizukuManager.userService == null

internal actual fun postBreakLiveNotification(
    courseName: String,
    classroom: String,
    minutesUntil: Int,
    progressPercent: Int,
) {
    if (platformIsXiaomiIslandCapable) {
        XiaomiIslandHelper(context).postIslandNotification(
            courseName = courseName,
            classroom = classroom,
            minutesUntil = minutesUntil,
            progressPercent = progressPercent,
        )
    } else {
        LiveUpdateHelper(context).postBreakNotification(
            courseName = courseName,
            classroom = classroom,
            minutesUntil = minutesUntil,
            progressPercent = progressPercent,
        )
    }
}

internal actual fun cancelBreakLiveNotification() {
    if (platformIsXiaomiIslandCapable) {
        XiaomiIslandHelper(context).cancelIsland()
    } else {
        LiveUpdateHelper(context).cancelLiveUpdate()
    }
}
