package com.schedule.qlit.notification
import androidx.core.content.edit
import com.schedule.qlit.R

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.schedule.qlit.data.model.ScheduleData
import com.schedule.qlit.data.repository.CourseRepository
import java.util.Calendar

object CourseReminderScheduler {
    private const val PREFS_NAME = "course_reminder_scheduler"
    private const val LAST_SCHEDULE_SIGNATURE_KEY = "last_schedule_signature"

    fun scheduleReminders(context: Context, schedule: ScheduleData) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val currentWeek = CourseRepository.calculateCurrentWeek(schedule.meta)
        val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
            .let { if (it == 1) 7 else it - 1 }

        // 取今天的课，各安排一个提前 10 分钟的闹钟
        val todayCourses = CourseRepository.getTodayCourses(schedule, dayOfWeek)
        // 把精确闹钟权限纳入签名：权限被收回后旧 exact alarm 会失效，
        // 签名随权限变化即可触发重排，落到下方的 setAndAllowWhileIdle 降级路径。
        val canExact = canScheduleExactAlarms(context)
        val signature = buildScheduleSignature(
            currentWeek = currentWeek,
            dayOfWeek = dayOfWeek,
            todayCourses = todayCourses,
            canScheduleExactAlarms = canExact
        )
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getString(LAST_SCHEDULE_SIGNATURE_KEY, null) == signature) {
            return
        }

        todayCourses.forEach { course ->
            val startMinutes = CourseRepository.parseTimeMinutes(course.startTime) ?: return@forEach
            val triggerCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, startMinutes / 60)
                set(Calendar.MINUTE, startMinutes % 60 - 10) // 提前 10 分钟
                set(Calendar.SECOND, 0)
            }
            // 如果已经过了今天的时间，就跳过
            if (triggerCal.timeInMillis <= System.currentTimeMillis()) return@forEach

            val intent = Intent(context, CourseReminderReceiver::class.java).apply {
                putExtra("course_name", course.name)
                putExtra("classroom", course.classroom)
                putExtra("start_time", course.startTime)
            }
            val pi = PendingIntent.getBroadcast(
                context,
                (course.name + course.startTime).hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            // Android 12+ 缺 SCHEDULE_EXACT_ALARM 时精确闹钟会抛 SecurityException，
            // 这里按权限降级：能精确就精确，否则退到 inexact 不让提醒整体崩溃。
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerCal.timeInMillis,
                    pi
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerCal.timeInMillis,
                    pi
                )
            }
        }

        prefs.edit { putString(LAST_SCHEDULE_SIGNATURE_KEY, signature) }
    }

    private fun canScheduleExactAlarms(context: Context): Boolean =
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
            .canScheduleExactAlarms()

    fun cancelAllReminders(context: Context, schedule: ScheduleData) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        schedule.courses.forEach { course ->
            val intent = Intent(context, CourseReminderReceiver::class.java)
            val pi = PendingIntent.getBroadcast(
                context,
                (course.name + course.startTime).hashCode(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            ) ?: return@forEach
            alarmManager.cancel(pi)
            pi.cancel()
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { remove(LAST_SCHEDULE_SIGNATURE_KEY) }
    }

    private fun buildScheduleSignature(
        currentWeek: Int,
        dayOfWeek: Int,
        todayCourses: List<com.schedule.qlit.data.model.Course>,
        canScheduleExactAlarms: Boolean
    ): String {
        val calendar = Calendar.getInstance()
        val dayStamp = "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.DAY_OF_YEAR)}"
        val courseStamp = todayCourses
            .sortedWith(compareBy({ it.startTime }, { it.name }, { it.classroom }))
            .joinToString("|") { course ->
                listOf(
                    course.name,
                    course.startTime,
                    course.classroom,
                    course.teacher,
                    course.weeks
                ).joinToString("#")
            }
        // 精确闹钟权限参与签名：权限被系统/用户收回后签名变化，
        // 下次 scheduleReminders 才会重排并落到 inexact 降级路径。
        return "$dayStamp@$currentWeek@$dayOfWeek@$canScheduleExactAlarms@$courseStamp"
    }
}
