package com.schedule.qlit.data

import com.schedule.qlit.data.model.ScheduleData
import com.schedule.qlit.data.model.UpdateInfo
import com.schedule.qlit.data.repository.UpdateManager
import com.schedule.qlit.data.store.AppContext
import com.schedule.qlit.notification.CourseReminderScheduler
import com.schedule.qlit.system.DeviceUtils

internal actual suspend fun checkAppUpdate(): UpdateInfo? {
    // 设置页「接收更新」关闭时，启动自动检查与手动检查都跳过
    if (!UpdateManager.isUpdateEnabled(AppContext.context)) return null
    return UpdateManager.checkUpdate(AppContext.context)
}

internal actual fun platformScheduleCourseReminders(
    previous: ScheduleData?,
    current: ScheduleData?,
) {
    previous?.let { CourseReminderScheduler.cancelAllReminders(AppContext.context, it) }
    current?.let { CourseReminderScheduler.scheduleReminders(AppContext.context, it) }
}

internal actual val platformSupportsLivePush: Boolean
    get() = DeviceUtils.supportsLivePush

internal actual fun debugLog(tag: String, message: String) {
    android.util.Log.d(tag, message)
}

internal actual val platformWebUserAgent: String =
    "Mozilla/5.0 (Linux; Android 15; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"

internal actual fun platformOpenUrl(url: String) {
    runCatching {
        AppContext.context.startActivity(
            android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
