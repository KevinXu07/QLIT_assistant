package com.schedule.qlit.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.launch

class QlitWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = QlitWidget()

    override fun onReceive(context: Context, intent: android.content.Intent) {
        // MIUI 曝光刷新广播与 App 侧主动刷新共用同一条 updateAll 路径
        if (intent.action == QlitWidgetRefresher.ACTION_REFRESH ||
            intent.action == MIUI_WIDGET_UPDATE_ACTION
        ) {
            val pendingResult = goAsync()
            kotlinx.coroutines.CoroutineScope(
                kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default
            ).launch {
                glanceAppWidget.updateAll(context.applicationContext)
                pendingResult.finish()
            }
            return
        }
        super.onReceive(context, intent)
    }

    companion object {
        private const val MIUI_WIDGET_UPDATE_ACTION = "miui.appwidget.action.APPWIDGET_UPDATE"

        fun hasInstances(context: Context): Boolean {
            val manager = AppWidgetManager.getInstance(context) ?: return false
            return manager.getAppWidgetIds(
                ComponentName(context, QlitWidgetReceiver::class.java)
            ).isNotEmpty()
        }
    }
}
