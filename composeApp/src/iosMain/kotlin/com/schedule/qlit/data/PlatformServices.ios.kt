package com.schedule.qlit.data

import com.schedule.qlit.data.model.ScheduleData
import com.schedule.qlit.data.model.UpdateInfo
import com.schedule.qlit.data.repository.CourseRepository
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import platform.Foundation.NSLog
import platform.Foundation.NSString
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.time.Clock

internal actual suspend fun checkAppUpdate(): UpdateInfo? = null

private const val COURSE_REMINDER_PREFIX = "qlit.course."

/**
 * 课前提醒：与 Android CourseReminderScheduler 同语义——只排「今天」的课，
 * 课前 10 分钟一次性通知（UNTimeIntervalNotificationTrigger），
 * 课表/权限变动时 App 启动路径会重排。
 */
internal actual fun platformScheduleCourseReminders(
    previous: ScheduleData?,
    current: ScheduleData?,
) {
    val center = UNUserNotificationCenter.currentNotificationCenter()

    fun removeOurs() {
        center.getPendingNotificationRequestsWithCompletionHandler { requests ->
            val ours = requests.orEmpty()
                .mapNotNull { (it as? UNNotificationRequest)?.identifier }
                .filter { it.startsWith(COURSE_REMINDER_PREFIX) }
            if (ours.isNotEmpty()) {
                center.removePendingNotificationRequestsWithIdentifiers(ours)
            }
        }
    }

    removeOurs()
    val schedule = current ?: return

    val now = Clock.System.now()
    val tz = TimeZone.currentSystemDefault()
    val today = now.toLocalDateTime(tz)
    // iOS/LocalDateTime 的 dayOfWeek 周一=1，与 Android 约定一致
    val dayOfWeek = today.dayOfWeek.let { if (it == DayOfWeek.SUNDAY) 7 else it.ordinal + 1 }

    CourseRepository.getTodayCourses(schedule, dayOfWeek).forEach { course ->
        val startMinutes = CourseRepository.parseTimeMinutes(course.startTime) ?: return@forEach
        val triggerMinutes = startMinutes - 10
        val triggerLocal = kotlinx.datetime.LocalDateTime(
            today.date,
            kotlinx.datetime.LocalTime(triggerMinutes / 60, triggerMinutes % 60, 0),
        )
        val interval = triggerLocal.toInstant(tz).epochSeconds - now.epochSeconds
        if (interval <= 0) return@forEach

        val content = UNMutableNotificationContent().apply {
            setTitle("上课提醒")
            setBody("${course.name} ${course.startTime} · ${course.classroom}")
            setSound(platform.UserNotifications.UNNotificationSound.defaultSound)
        }
        val id = "$COURSE_REMINDER_PREFIX${(course.name + course.startTime).hashCode()}"
        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
            timeInterval = interval.toDouble(),
            repeats = false,
        )
        center.addNotificationRequest(
            UNNotificationRequest.requestWithIdentifier(id, content, trigger)
        ) { error ->
            error?.let { NSLog("%@", "course reminder $id failed: ${it.localizedDescription}" as NSString) }
        }
    }
}

// Live Activities 由 ActivityKit（Swift 扩展）提供，K/N 侧恒按可用处理，
// 避免"课程实时推送暂不支持"提示误弹。
internal actual val platformSupportsLivePush: Boolean = true

internal actual fun debugLog(tag: String, message: String) {
    NSLog("%@", "$tag: $message" as NSString)
}

internal actual val platformWebUserAgent: String =
    "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Mobile/15E148 Safari/604.1"

internal actual fun platformOpenUrl(url: String) {
    platform.Foundation.NSURL.URLWithString(url)?.let {
        platform.UIKit.UIApplication.sharedApplication.openURL(it, mapOf<Any?, Any>(), null)
    }
}
