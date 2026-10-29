package com.schedule.qlit.data.repository

/**
 * 仓储层 → 小组件缓存的桥。各仓储拿到新数据后调用，
 * Android 落 widget_cache.json（供 Glance Widget 读），
 * iOS 先写共享偏好，等 WidgetKit 接入后切 App Group。
 */

internal expect fun saveChaoxingToWidgetCache(pendingWork: Int, pendingExam: Int)

internal expect fun markChaoxingWidgetExpired()

internal expect fun markChaoxingWidgetLoggedOut()

/** 课表等数据变化后请求各平台小组件刷新。 */
internal expect fun refreshWidgets()

/** 工具列表行尾状态（文本 + 是否热点标记）。 */
data class ToolStatus(val text: String, val hot: Boolean = false)

/**
 * 各工具的行尾状态：Android 读小组件缓存 + 登录标记，
 * iOS 暂无对应缓存，返回空（后续接共享容器）。
 */
internal expect fun loadToolStatuses(): Map<String, ToolStatus>

/** 超星小组件缓存（待完成作业数, 待办考试数, 状态）。 */
data class ChaoxingWidgetStatus(val work: Int, val exam: Int, val state: Int)

const val CX_STATE_OK = 0
const val CX_STATE_EXPIRED = 1
const val CX_STATE_LOGGED_OUT = 2
const val CX_STATE_UNKNOWN = -1

internal expect fun loadChaoxingFromWidgetCache(): ChaoxingWidgetStatus

/** 淘宝取件主页状态写入小组件缓存。 */
internal expect fun saveTaobaoToWidgetCache(loggedIn: Boolean, pendingCount: Int)
