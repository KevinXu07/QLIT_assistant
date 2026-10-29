package com.schedule.qlit.data.repository

import com.schedule.qlit.data.store.NativeWidgetBridgeHolder
import com.schedule.qlit.data.store.kvStore

/** WidgetKit 扩展与主 app 共享的 App Group suite；store 名直接用 group id。 */
private const val WIDGET_SUITE = "group.cn.kevinxu.qlit.free"

private fun widgetCache() = kvStore(WIDGET_SUITE)

internal actual fun saveChaoxingToWidgetCache(pendingWork: Int, pendingExam: Int) {
    val store = widgetCache()
    store.putLong("cx_work", pendingWork.toLong())
    store.putLong("cx_exam", pendingExam.toLong())
    store.putLong("cx_state", 0)
}

internal actual fun markChaoxingWidgetExpired() {
    widgetCache().putLong("cx_state", 1)
}

internal actual fun markChaoxingWidgetLoggedOut() {
    widgetCache().putLong("cx_state", 2)
}

internal actual fun refreshWidgets() {
    NativeWidgetBridgeHolder.impl?.reloadWidgetTimelines()
}

internal actual fun loadToolStatuses(): Map<String, ToolStatus> = emptyMap()

internal actual fun loadChaoxingFromWidgetCache(): ChaoxingWidgetStatus {
    val store = widgetCache()
    if (!store.contains("cx_state")) return ChaoxingWidgetStatus(0, 0, CX_STATE_UNKNOWN)
    return ChaoxingWidgetStatus(
        work = store.getInt("cx_work"),
        exam = store.getInt("cx_exam"),
        state = store.getInt("cx_state", CX_STATE_UNKNOWN),
    )
}

internal actual fun saveTaobaoToWidgetCache(loggedIn: Boolean, pendingCount: Int) {
    val store = widgetCache()
    store.putBoolean("tb_logged_in", loggedIn)
    store.putInt("tb_pending", pendingCount)
}
