package com.schedule.qlit.data.store

/** 应用通知权限是否已授予。 */
internal expect suspend fun notificationsPermitted(): Boolean

/**
 * 是否能发"实况/强提醒"：Android 13+ 需 POST_NOTIFICATIONS
 * 且 API 36+ 的 promoted notification 开关打开；iOS 对应 Live
 * Activities（Swift 扩展落地前按通知权限返回）。
 */
internal expect suspend fun promotedNotificationsPermitted(): Boolean

/** 课间实况通知（灵动岛 / Live Update / Live Activity）。 */
internal expect val platformIsXiaomiIslandCapable: Boolean

/** 小米灵动岛依赖 Shizuku 用户服务，未连接时需要权限引导卡。 */
internal expect fun islandShizukuMissing(): Boolean

internal expect fun postBreakLiveNotification(
    courseName: String,
    classroom: String,
    minutesUntil: Int,
    progressPercent: Int,
)

internal expect fun cancelBreakLiveNotification()
