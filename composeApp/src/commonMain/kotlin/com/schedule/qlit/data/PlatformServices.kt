package com.schedule.qlit.data

import com.schedule.qlit.data.model.ScheduleData
import com.schedule.qlit.data.model.UpdateInfo

/** 开屏自动更新检查。iOS 恒 null（更新走 App Store）。 */
internal expect suspend fun checkAppUpdate(): UpdateInfo?

/**
 * 课表变动后重排课前提醒。Android → AlarmManager 调度；
 * iOS → UNUserNotificationCenter（实现随 iosApp 壳落地）。
 */
internal expect fun platformScheduleCourseReminders(
    previous: ScheduleData?,
    current: ScheduleData?,
)

/** 课程实时推送（Live Update / 灵动岛 / Live Activity）是否可用。 */
internal expect val platformSupportsLivePush: Boolean

/** 轻量调试日志：Android → Log.d，iOS → NSLog。 */
internal expect fun debugLog(tag: String, message: String)

/**
 * 平台默认 WebView UA：登录页和 mtop 请求统一用它，
 * 与真实 WebView UA 一致避免风控指纹差异。
 */
internal expect val platformWebUserAgent: String

/** 用系统默认浏览器/外链处理器打开 URL。 */
internal expect fun platformOpenUrl(url: String)
