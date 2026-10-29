package com.schedule.qlit.data.store

/**
 * WidgetKit / ActivityKit 只在 Swift 侧可用：iosApp 原生壳实现本协议并在
 * 启动时注册到 [NativeWidgetBridgeHolder]，Kotlin 侧经它驱动时间线刷新和
 * 课间 Live Activity。未注册（扩展未集成）时调用方自行降级。
 */
interface NativeWidgetBridge {

    /** 刷新全部 WidgetKit 时间线。 */
    fun reloadWidgetTimelines()

    /** 是否允许开启 Live Activity（ActivityAuthorizationInfo.areActivitiesEnabled）。 */
    fun liveActivitiesEnabled(): Boolean

    /** 创建或更新课间实况活动。 */
    fun updateCourseLiveActivity(
        courseName: String,
        classroom: String,
        minutesUntil: Int,
        progressPercent: Int,
    )

    /** 立即结束课间实况活动。 */
    fun endCourseLiveActivity()
}

object NativeWidgetBridgeHolder {
    var impl: NativeWidgetBridge? = null
}
