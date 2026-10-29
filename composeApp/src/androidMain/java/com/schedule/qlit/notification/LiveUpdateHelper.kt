package com.schedule.qlit.notification
import com.schedule.qlit.R
import androidx.core.graphics.toColorInt

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.schedule.qlit.MainActivity
import com.schedule.qlit.system.DeviceUtils

class LiveUpdateHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "live_update_channel"
        const val NOTIFICATION_ID = 1001
        const val RESERVATION_NOTIFICATION_ID = 1002
        private const val RESERVATION_TIMEOUT_SECONDS = 480
        private val RESERVATION_ACCENT_COLOR = "#7D7AF8".toColorInt()
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // 小米灵动岛实现
    private val xiaomiHelper: XiaomiIslandHelper? =
        if (DeviceUtils.supportsXiaomiIsland) XiaomiIslandHelper(context) else null

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "实时更新",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "用于演示 Live Update 功能"
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * 检查是否可以发布推广通知（Live Update）
     */
    @RequiresApi(api = 36)
    fun canPostPromotedNotifications(): Boolean {
        return notificationManager.canPostPromotedNotifications()
    }

    /**
     * 创建 Live Update 通知
     * 必须满足以下条件才能成为 Live Update:
     * 1. 使用 BigTextStyle、CallStyle、ProgressStyle 或 MetricStyle
     * 2. 设置 setOngoing(true)
     * 3. 设置 setRequestPromotedOngoing(true)
     * 4. 设置 contentTitle
     * 5. 不能使用自定义视图
     * 6. 通知渠道不能是 IMPORTANCE_MIN
     */
    fun createLiveUpdateNotification(
        title: String,
        content: String,
        bigText: String,
        progress: Int = -1, // -1 表示不显示进度条
        remainingTimeMinutes: Int = 0
    ): NotificationCompat.Builder {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_live_update_status)
            .setContentTitle(title)
            .setContentText(content)
            .setOngoing(true)  // 必须：持续进行状态
            .setRequestPromotedOngoing(true)  // 请求提升为 Live Update
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setColor(RESERVATION_ACCENT_COLOR)

        // 使用 BigTextStyle（Live Update 必须使用特定样式之一）
        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(bigText)
            .setBigContentTitle(title)

        // 如果有剩余时间，显示计时器
        if (remainingTimeMinutes > 0) {
            bigTextStyle.setSummaryText("$remainingTimeMinutes 分钟前")
        }

        builder.setStyle(bigTextStyle)

        // 添加进度条（可选）
        if (progress >= 0 && progress <= 100) {
            builder.setProgress(100, progress, false)
        }

        return builder
    }

    /**
     * 发布 Live Update 通知
     */
    fun postLiveUpdate(
        title: String,
        content: String,
        bigText: String,
        progress: Int = -1,
        remainingTimeMinutes: Int = 0
    ) {
        if (Build.VERSION.SDK_INT < 36) {
            android.util.Log.w("LiveUpdate", "Live Update 需要 Android 15（API 35）及以上版本")
            return
        }
        if (!canPostPromotedNotifications()) {
            android.util.Log.e("LiveUpdate", "无法发布推广通知，请检查权限设置")
            return
        }

        val notification = createLiveUpdateNotification(
            title = title,
            content = content,
            bigText = bigText,
            progress = progress,
            remainingTimeMinutes = remainingTimeMinutes
        ).build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * 更新现有的 Live Update 通知
     */
    fun updateLiveUpdate(
        title: String,
        content: String,
        bigText: String,
        progress: Int = -1,
        remainingTimeMinutes: Int = 0
    ) {
        postLiveUpdate(title, content, bigText, progress, remainingTimeMinutes)
    }

    /**
     * 取消 Live Update 通知
     */
    fun cancelLiveUpdate() {
        xiaomiHelper?.cancelIsland() ?: notificationManager.cancel(NOTIFICATION_ID)
    }

    fun postReservationLiveUpdate(
        areaName: String,
        deviceNumber: String,
        shortAccount: String,
        remainingSeconds: Int
    ) {
        if (xiaomiHelper != null) {
            xiaomiHelper.postReservationIslandNotification(
                areaName = areaName,
                deviceNumber = deviceNumber,
                shortAccount = shortAccount,
                remainingSeconds = remainingSeconds
            )
            return
        }

        if (Build.VERSION.SDK_INT < 36) return
        if (!canPostPromotedNotifications()) return

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "tools")
        }
        val tapPendingIntent = PendingIntent.getActivity(
            context,
            1,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val normalizedDeviceNumber = deviceNumber.ifBlank { "--" }
        val countdown = formatCountdown(remainingSeconds)
        val locationLine = areaName.ifBlank { "浴室" }
        val accountLine = shortAccount.takeIf { it.isNotBlank() }?.let { "短账号 $it" } ?: "短账号暂不可用"
        val title = if (remainingSeconds > 0) {
            "已预约${normalizedDeviceNumber}号"
        } else {
            "预约已过期"
        }
        val body = buildString {
            if (remainingSeconds > 0) {
                appendLine("请在 $countdown 内到达浴室")
            } else {
                appendLine("预约保留时间已结束，请重新预约")
            }
            append(locationLine)
            appendLine()
            append(accountLine)
        }

        val progress = if (remainingSeconds > 0) {
            (((remainingSeconds.coerceAtMost(RESERVATION_TIMEOUT_SECONDS)) / RESERVATION_TIMEOUT_SECONDS.toFloat()) * 100)
                .toInt()
                .coerceIn(0, 100)
        } else {
            0
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_live_update_status)
            .setContentTitle(title)
            .setContentText("${normalizedDeviceNumber}号浴位 · $locationLine")
            .setContentIntent(tapPendingIntent)
            .setOngoing(true)
            .setRequestPromotedOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setColor(RESERVATION_ACCENT_COLOR)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(body)
                    .setBigContentTitle(title)
                    .setSummaryText("待服务")
            )
            .setProgress(100, progress, false)
            .build()

        notificationManager.notify(RESERVATION_NOTIFICATION_ID, notification)
    }

    fun cancelReservationLiveUpdate() {
        xiaomiHelper?.cancelReservationIsland() ?: notificationManager.cancel(RESERVATION_NOTIFICATION_ID)
    }

    /**
     * 专供 XiaomiIslandHelper 降级调用，不走小米分支避免循环
     */
    fun postBreakNotificationFallback(
        courseName: String,
        classroom: String,
        minutesUntil: Int,
        progressPercent: Int
    ) {
        if (Build.VERSION.SDK_INT < 36) return
        if (!canPostPromotedNotifications()) return

        val title = "📖 $courseName"

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "today")
        }
        val tapPendingIntent = PendingIntent.getActivity(
            context,
            0,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_live_update_status)
            .setContentTitle(courseName)
            .setContentText("教室：$classroom · 还有 $minutesUntil 分钟")
            .setContentIntent(tapPendingIntent)
            .setOngoing(true)
            .setRequestPromotedOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setColor(RESERVATION_ACCENT_COLOR)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("教室：$classroom\n还有 $minutesUntil 分钟")
                    .setBigContentTitle(courseName)
            )
            .setProgress(100, progressPercent, false)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * 发布课间倒计时通知
     * 自动根据设备类型选择实现
     */
    fun postBreakNotification(
        courseName: String,
        classroom: String,
        minutesUntil: Int,
        progressPercent: Int
    ) {
        val title = "📖 $courseName"

        if (xiaomiHelper != null) {
            // 小米：走灵动岛
            xiaomiHelper.postIslandNotification(
                courseName = courseName,
                classroom = classroom,
                minutesUntil = minutesUntil,
                progressPercent = progressPercent
            )

        } else {
            // 其他设备：标准 Live Update
            if (Build.VERSION.SDK_INT < 36) return
            if (!canPostPromotedNotifications()) return

            val tapIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("navigate_to", "today")
            }
            val tapPendingIntent = PendingIntent.getActivity(
                context,
                0,
                tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            notificationManager.notify(
                NOTIFICATION_ID,
                NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_live_update_status)
                    .setContentTitle(courseName)
                    .setContentText("教室：$classroom · 还有 $minutesUntil 分钟")
                    .setContentIntent(tapPendingIntent)
                    .setOngoing(true)
                    .setRequestPromotedOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                    .setOnlyAlertOnce(true)
                    .setShowWhen(false)
                    .setColor(RESERVATION_ACCENT_COLOR)
                    .setStyle(
                        NotificationCompat.BigTextStyle()
                            .bigText("教室：$classroom\n还有 $minutesUntil 分钟")
                            .setBigContentTitle(courseName)
                    )
                    .setProgress(100, progressPercent, false)
                    .build()
            )
        }
    }

    /**
     * 检查通知是否已被提升为 Live Update
     */
    @RequiresApi(api = 36)
    fun isNotificationPromoted(): Boolean {
        val notification = notificationManager.activeNotifications
            .find { it.id == NOTIFICATION_ID }
        return notification?.notification?.flags?.and(android.app.Notification.FLAG_PROMOTED_ONGOING) != 0
    }

    private fun formatCountdown(seconds: Int): String {
        if (seconds <= 0) return "00'00''"
        val minutes = seconds / 60
        val remainSeconds = seconds % 60
        return "%02d'%02d''".format(minutes, remainSeconds)
    }
}
