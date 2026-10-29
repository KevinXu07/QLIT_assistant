package com.schedule.qlit.data.store

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSLog
import platform.Foundation.NSString
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume

internal actual suspend fun notificationsPermitted(): Boolean =
    suspendCancellableCoroutine { cont ->
        UNUserNotificationCenter.currentNotificationCenter()
            .getNotificationSettingsWithCompletionHandler { settings ->
                val status = settings?.authorizationStatus
                cont.resume(
                    status == UNAuthorizationStatusAuthorized ||
                        status == UNAuthorizationStatusProvisional
                )
            }
    }

// Live Activity 授权走 ActivityKit（Swift 桥实现）；桥未注册时退回通知权限。
internal actual suspend fun promotedNotificationsPermitted(): Boolean =
    NativeWidgetBridgeHolder.impl?.liveActivitiesEnabled()
        ?: notificationsPermitted()

internal actual val platformIsXiaomiIslandCapable: Boolean = false

internal actual fun islandShizukuMissing(): Boolean = false

internal actual fun postBreakLiveNotification(
    courseName: String,
    classroom: String,
    minutesUntil: Int,
    progressPercent: Int,
) {
    NativeWidgetBridgeHolder.impl
        ?.updateCourseLiveActivity(courseName, classroom, minutesUntil, progressPercent)
        ?: NSLog("%@", "breakLiveNotification(no bridge): $courseName $classroom $minutesUntil $progressPercent" as NSString)
}

internal actual fun cancelBreakLiveNotification() {
    NativeWidgetBridgeHolder.impl?.endCourseLiveActivity()
        ?: NSLog("cancelBreakLiveNotification(no bridge)")
}
