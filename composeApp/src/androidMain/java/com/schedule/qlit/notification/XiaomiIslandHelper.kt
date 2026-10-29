package com.schedule.qlit.notification
import com.schedule.qlit.R

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.Log
import androidx.core.app.NotificationCompat
import com.xzakota.hyper.notification.focus.FocusNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.schedule.qlit.MainActivity
import com.schedule.qlit.system.ShizukuManager

class XiaomiIslandHelper(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val networkMutex = Mutex()
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        private const val TAG = "XiaomiIslandHelper"
        private const val CHANNEL_ID = "qlit_island_channel"
        private const val NOTIFICATION_ID = 2001
        private const val RESERVATION_NOTIFICATION_ID = 2002
        private const val TARGET_PACKAGE = "com.xiaomi.xmsf"
    }

    init {
        val channel = NotificationChannel(
            CHANNEL_ID, "课程提醒", NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)
    }

    fun postIslandNotification(
        courseName: String,
        classroom: String,
        minutesUntil: Int,
        progressPercent: Int
    ) {
        scope.launch(Dispatchers.IO) {
            // 1. 在主进程构建好通知（包含超级岛数据）
            val notification = buildIslandNotification(
                courseName, classroom, minutesUntil, progressPercent
            )

            // 2. 获取 xmsf 的 uid
            val targetUid = runCatching {
                context.packageManager.getApplicationInfo(TARGET_PACKAGE, 0).uid
            }.getOrElse { -1 }

            val service = ShizukuManager.userService

            networkMutex.withLock {
                try {
                    // 3. 用 Shizuku 断掉 xmsf 网络
                    if (service != null && targetUid != -1) {
                        runCatching { service.setUidFirewallRule(targetUid, true) }
                        Log.i(TAG, "已断开 xmsf 网络 uid=$targetUid")
                    }

                    // 4. 在主进程发通知（不走 Shizuku）
                    notificationManager.notify(NOTIFICATION_ID, notification)
                    Log.i(TAG, "通知已发送")

                    // 5. 等待 100ms 让异步扫描超时
                    delay(100L)

                } finally {
                    // 6. 恢复 xmsf 网络
                    if (service != null && targetUid != -1) {
                        runCatching { service.setUidFirewallRule(targetUid, false) }
                        Log.i(TAG, "已恢复 xmsf 网络")
                    }
                }
            }
        }
    }

    private fun buildIslandNotification(
        courseName: String,
        classroom: String,
        minutesUntil: Int,
        progressPercent: Int
    ): Notification {
        val title = courseName

        // 构建跳转 Intent
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

        // 用你 app 的图标
        val lightIcon = android.graphics.drawable.Icon.createWithResource(context, android.R.drawable.ic_menu_mylocation)
            .setTint(Color.BLACK)
        val darkIcon = android.graphics.drawable.Icon.createWithResource(context, android.R.drawable.ic_menu_mylocation)
            .setTint(Color.WHITE)

        // 构建超级岛数据包
        val islandExtras = FocusNotification.buildV3 {
            val lightKey = createPicture("key_logo_light", lightIcon)
            val darkKey  = createPicture("key_logo_dark",  darkIcon)

            islandFirstFloat = true
            enableFloat = false   // ongoing 通知不弹展开态
            updatable = true
            ticker = title
            tickerPic = lightKey

            // 摘要态：左侧图标 + 右侧文字
            island {
                islandProperty = 1
                bigIslandArea {
                    imageTextInfoLeft {
                        type = 1
                        picInfo { type = 1; pic = lightKey }
                    }
                    imageTextInfoRight {
                        type = 3
                        textInfo { this.title = title }
                    }
                }
                smallIslandArea {
                    picInfo { type = 1; pic = lightKey }
                }
            }

            // 展开态
            iconTextInfo {
                this.title = title
                this.content = "$minutesUntil 分钟 · $classroom"
                animIconInfo { type = 0; src = lightKey }
            }

            picInfo {
                type = 1
                pic = lightKey
                picDark = darkKey
            }

            // 进度条展示
            if (progressPercent >= 0) {
                textButton {
                    addActionInfo {
                        actionTitle = "知道了"
                    }
                }
            }
        }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle(courseName)
            .setContentText("教室：$classroom · 还有 $minutesUntil 分钟")
            .setContentIntent(tapPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("教室：$classroom\n还有 $minutesUntil 分钟")
                    .setBigContentTitle(courseName)
            )
            .setProgress(100, progressPercent, false)
            .apply { addExtras(islandExtras) }
            .build()
    }

    fun cancelIsland() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    fun postReservationIslandNotification(
        areaName: String,
        deviceNumber: String,
        shortAccount: String,
        remainingSeconds: Int
    ) {
        scope.launch(Dispatchers.IO) {
            val notification = buildReservationIslandNotification(
                areaName = areaName,
                deviceNumber = deviceNumber,
                shortAccount = shortAccount,
                remainingSeconds = remainingSeconds
            )

            val targetUid = runCatching {
                context.packageManager.getApplicationInfo(TARGET_PACKAGE, 0).uid
            }.getOrElse { -1 }

            val service = ShizukuManager.userService

            networkMutex.withLock {
                try {
                    if (service != null && targetUid != -1) {
                        runCatching { service.setUidFirewallRule(targetUid, true) }
                    }
                    notificationManager.notify(RESERVATION_NOTIFICATION_ID, notification)
                    delay(100L)
                } finally {
                    if (service != null && targetUid != -1) {
                        runCatching { service.setUidFirewallRule(targetUid, false) }
                    }
                }
            }
        }
    }

    private fun buildReservationIslandNotification(
        areaName: String,
        deviceNumber: String,
        shortAccount: String,
        remainingSeconds: Int
    ): Notification {
        val title = "已预约 ${deviceNumber.ifBlank { "--" }}号"
        val countdown = formatCountdown(remainingSeconds)
        val content = buildString {
            append(areaName.ifBlank { "浴室" })
            if (shortAccount.isNotBlank()) append(" · $shortAccount")
        }

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

        val lightIcon = android.graphics.drawable.Icon.createWithResource(
            context,
            android.R.drawable.ic_menu_mylocation
        ).setTint(Color.BLACK)
        val darkIcon = android.graphics.drawable.Icon.createWithResource(
            context,
            android.R.drawable.ic_menu_mylocation
        ).setTint(Color.WHITE)

        val islandExtras = FocusNotification.buildV3 {
            val lightKey = createPicture("reservation_logo_light", lightIcon)
            val darkKey = createPicture("reservation_logo_dark", darkIcon)

            islandFirstFloat = true
            enableFloat = false
            updatable = true
            ticker = title
            tickerPic = lightKey

            island {
                islandProperty = 1
                bigIslandArea {
                    imageTextInfoLeft {
                        type = 1
                        picInfo { type = 1; pic = lightKey }
                    }
                    imageTextInfoRight {
                        type = 3
                        textInfo { this.title = countdown }
                    }
                }
                smallIslandArea {
                    picInfo { type = 1; pic = lightKey }
                }
            }

            iconTextInfo {
                this.title = title
                this.content = "$countdown · $content"
                animIconInfo { type = 0; src = lightKey }
            }

            picInfo {
                type = 1
                pic = lightKey
                picDark = darkKey
            }
        }

        val progress = if (remainingSeconds > 0) {
            (((remainingSeconds.coerceAtMost(480)) / 480f) * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle(title)
            .setContentText("$countdown · $content")
            .setContentIntent(tapPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("请在 $countdown 内到达浴室\n$content")
                    .setBigContentTitle(title)
            )
            .setProgress(100, progress, false)
            .apply { addExtras(islandExtras) }
            .build()
    }

    fun cancelReservationIsland() {
        notificationManager.cancel(RESERVATION_NOTIFICATION_ID)
    }

    private fun formatCountdown(seconds: Int): String {
        if (seconds <= 0) return "00'00''"
        val minutes = seconds / 60
        val remainSeconds = seconds % 60
        return "%02d'%02d''".format(minutes, remainSeconds)
    }
}
