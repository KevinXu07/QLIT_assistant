package com.schedule.qlit.data

/** 平台设备信息（遥测、UA 等场景共用）。 */
internal data class DeviceInfo(
    /** App 版本名（Android versionName / iOS CFBundleShortVersionString）。 */
    val versionName: String,
    /** Android longVersionCode / iOS CFBundleVersion。 */
    val versionCode: String,
    val brand: String,
    val model: String,
    /** "Android" / "iOS"。 */
    val osName: String,
    /** 系统版本号（Android RELEASE / iOS systemVersion）。 */
    val osVersion: String,
    /** Android SDK INT；iOS 恒 0。 */
    val sdk: Int,
    /** Android ROM 名+版本（如 MIUI 14）；iOS 空串。 */
    val rom: String,
)

internal expect fun deviceInfo(): DeviceInfo

/**
 * 设备硬件指纹种子（Hydream client uuid 用）。
 * 只需同设备稳定、跨设备大概率不同。
 */
internal expect fun deviceHardwareSeed(): String

/**
 * 液态玻璃模糊是否可用。Android 需要 API 33+（RuntimeShader）；
 * iOS 上 blur 由 UIGlassEffect/系统效果提供，恒 true。
 */
internal expect val platformSupportsLiquidBlur: Boolean

/**
 * 是否显示应用内"检查更新"入口。Android 走 APK 自更新；
 * iOS 更新只走 App Store，隐藏该入口。
 */
internal expect val platformShowsUpdateCheck: Boolean
