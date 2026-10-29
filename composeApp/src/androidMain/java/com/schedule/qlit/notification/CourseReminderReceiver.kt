package com.schedule.qlit.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class CourseReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val serviceIntent = Intent(context, CourseReminderService::class.java).apply {
            putExtra("course_name", intent.getStringExtra("course_name"))
            putExtra("classroom", intent.getStringExtra("classroom"))
            putExtra("start_time", intent.getStringExtra("start_time"))
        }
        context.startForegroundService(serviceIntent)
    }
}
