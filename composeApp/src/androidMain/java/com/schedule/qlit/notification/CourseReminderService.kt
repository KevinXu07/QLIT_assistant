package com.schedule.qlit.notification

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.*
import java.util.Calendar
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

class CourseReminderService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var updateJob: Job? = null

    // 缓存当前任务的 ID，如果是同一门课再次触发，我们只更新数据不重启任务
    private var currentTaskKey: String? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val name = intent?.getStringExtra("course_name") ?: return START_NOT_STICKY
        val classroom = intent.getStringExtra("classroom") ?: "未知"
        val startTime = intent.getStringExtra("start_time") ?: return START_NOT_STICKY

        val taskKey = "$name@$startTime"

        // 1. 如果新意图和当前任务一致，无需操作（或者仅更新通知）
        if (currentTaskKey == taskKey) return START_STICKY

        // 2. 如果是新课程，先取消旧任务
        updateJob?.cancel()
        currentTaskKey = taskKey

        val helper = LiveUpdateHelper(this)

        // 3. 立即启动前台服务，防止 ANR 或被系统杀掉
        startForegroundCompat(name, classroom)

        // 4. 启动协程处理倒计时
        updateJob = serviceScope.launch {
            try {
                runCountdown(name, classroom, startTime, helper)
            } finally {
                // 确保在任何情况下（完成或被取消）都能正确关闭
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        return START_REDELIVER_INTENT
    }

    private fun startForegroundCompat(name: String, classroom: String) {
        val helper = LiveUpdateHelper(this)
        val notification = helper.createLiveUpdateNotification(
            title = name,
            content = classroom,
            bigText = "正在计算上课时间...",
            progress = 0
        ).build()

        ServiceCompat.startForeground(
            this,
            LiveUpdateHelper.NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else 0
        )
    }

    private suspend fun runCountdown(
        name: String,
        classroom: String,
        startTime: String,
        helper: LiveUpdateHelper
    ) {
        val targetMinutes = timeToMinutes(startTime)

        // 修改这里：增加 coroutineContext. 前缀
        while (coroutineContext.isActive) {
            val nowMinutes = currentTimeInMinutes()
            val remaining = targetMinutes - nowMinutes

            if (remaining <= 0) {
                Log.d("CourseService", "课程已开始，结束提醒")
                break
            }

            val progress = ((10 - remaining) * 10).coerceIn(0, 100)

            helper.postBreakNotification(
                courseName = name,
                classroom = classroom,
                minutesUntil = remaining.toInt(), // 确保转为 Int
                progressPercent = progress.toInt()
            )

            val nowMillis = System.currentTimeMillis()
            val secondsUntilNextMinute = 60 - (nowMillis / 1000 % 60)

            val waitTime = (secondsUntilNextMinute * 1000).coerceIn(10_000, 30_000)
            delay(waitTime)
        }
    }

    private fun timeToMinutes(time: String): Long {
        return try {
            val parts = time.split(":")
            parts[0].toLong() * 60 + parts[1].toLong()
        } catch (e: Exception) { 0L }
    }

    private fun currentTimeInMinutes(): Long {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.HOUR_OF_DAY).toLong() * 60 + cal.get(Calendar.MINUTE).toLong()
    }

    override fun onDestroy() {
        updateJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}