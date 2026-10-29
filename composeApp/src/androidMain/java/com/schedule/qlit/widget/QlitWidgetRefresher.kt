package com.schedule.qlit.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

/**
 * 小组件刷新入口：
 *  - [requestRefresh]：App 内数据变更后调用，广播触发整表重绘（带去抖，无实例时短路）
 *  - [scheduleNextBoundary]：在最近的课程上下课边界/跨天零点排一次精确刷新
 */
object QlitWidgetRefresher {

    const val ACTION_REFRESH = "com.schedule.qlit.widget.action.REFRESH"

    private const val REQUEST_CODE = 7201
    private const val MIN_TRIGGER_INTERVAL_MS = 5_000L

    /** 数据变更后触发；桌面没挂小组件时不做任何事，避免无谓唤醒进程。 */
    fun requestRefresh(context: Context) {
        val appContext = context.applicationContext
        if (!QlitWidgetReceiver.hasInstances(appContext)) return
        val now = System.currentTimeMillis()
        if (now - QlitWidgetCache.lastTriggerAt(appContext) < MIN_TRIGGER_INTERVAL_MS) return
        QlitWidgetCache.markTriggered(appContext)
        appContext.sendBroadcast(
            Intent(appContext, QlitWidgetReceiver::class.java).setAction(ACTION_REFRESH)
        )
    }

    /** 每次小组件完成数据装配后调用，保证课程高亮态和跨天切换能准时翻转。 */
    fun scheduleNextBoundary(context: Context, atMillis: Long?) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            REQUEST_CODE,
            Intent(appContext, QlitWidgetReceiver::class.java).setAction(ACTION_REFRESH),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.cancel(pendingIntent)
        if (atMillis == null || atMillis <= System.currentTimeMillis()) return
        if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pendingIntent)
        }
    }
}
