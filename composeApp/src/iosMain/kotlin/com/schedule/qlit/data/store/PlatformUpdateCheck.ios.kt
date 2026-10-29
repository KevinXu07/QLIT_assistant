package com.schedule.qlit.data.store

private const val KEY_UPDATE_CHECK_ENABLED = "update_check_enabled"

// iOS 更新只走 App Store；标志位仅作设置页状态回显
actual fun platformIsUpdateCheckEnabled(): Boolean =
    kvStore("ui_preferences").getBoolean(KEY_UPDATE_CHECK_ENABLED, true)

actual fun platformSetUpdateCheckEnabled(enabled: Boolean) {
    kvStore("ui_preferences").putBoolean(KEY_UPDATE_CHECK_ENABLED, enabled)
}
