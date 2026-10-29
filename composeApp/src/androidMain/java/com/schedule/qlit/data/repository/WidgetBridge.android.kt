package com.schedule.qlit.data.repository

import com.schedule.qlit.data.store.AppContext
import com.schedule.qlit.widget.QlitWidgetCache
import com.schedule.qlit.widget.QlitWidgetRefresher

internal actual fun saveChaoxingToWidgetCache(pendingWork: Int, pendingExam: Int) {
    QlitWidgetCache.saveChaoxing(AppContext.context, pendingWork, pendingExam)
}

internal actual fun markChaoxingWidgetExpired() {
    QlitWidgetCache.saveChaoxingExpired(AppContext.context)
}

internal actual fun markChaoxingWidgetLoggedOut() {
    QlitWidgetCache.saveChaoxingLoggedOut(AppContext.context)
}

internal actual fun refreshWidgets() {
    QlitWidgetRefresher.requestRefresh(AppContext.context)
}

internal actual fun loadChaoxingFromWidgetCache(): ChaoxingWidgetStatus {
    val (work, exam, state) = QlitWidgetCache.loadChaoxing(AppContext.context)
    return ChaoxingWidgetStatus(work, exam, state)
}

internal actual fun saveTaobaoToWidgetCache(loggedIn: Boolean, pendingCount: Int) {
    QlitWidgetCache.saveTaobao(AppContext.context, loggedIn = loggedIn, pendingCount = pendingCount)
}

internal actual fun loadToolStatuses(): Map<String, ToolStatus> {
    val context = AppContext.context
    val values = mutableMapOf<String, ToolStatus>()
    val taobao = QlitWidgetCache.loadTaobao(context)
        ?: context.getSharedPreferences("taobao_pickup_prefs", android.content.Context.MODE_PRIVATE).let {
            it.getBoolean("home_login_detected", false) to it.getInt("home_pending_count", -1)
        }
    values["TAOBAO_PICKUP"] = when {
        !taobao.first -> ToolStatus("未登录")
        taobao.second > 0 -> ToolStatus("${taobao.second} 件待取", hot = true)
        else -> ToolStatus("无待取")
    }
    val (work, exam, state) = QlitWidgetCache.loadChaoxing(context)
    when (state) {
        QlitWidgetCache.CX_STATE_OK ->
            values["CHAOXING"] = if (work + exam > 0) ToolStatus("${work + exam} 项", hot = true) else ToolStatus("已完成")
        QlitWidgetCache.CX_STATE_EXPIRED -> values["CHAOXING"] = ToolStatus("需重新登录", hot = true)
        QlitWidgetCache.CX_STATE_LOGGED_OUT -> values["CHAOXING"] = ToolStatus("未登录")
    }
    return values
}
