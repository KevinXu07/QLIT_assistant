package com.schedule.qlit.data.store

data class HomeDashboardRefreshSettings(
    val autoRefreshEnabled: Boolean,
    val refreshIntervalMinutes: Int
)

internal object HomeDashboardRefreshSettingsStore {
    private const val PREFS_NAME = "home_dashboard_prefs"
    private const val AUTO_REFRESH_ENABLED_KEY = "auto_refresh_enabled"
    private const val REFRESH_INTERVAL_MINUTES_KEY = "refresh_interval_minutes"
    const val DEFAULT_INTERVAL_MINUTES = 15
    val INTERVAL_OPTIONS = listOf(5, 15, 30, 60)

    fun normalizeInterval(minutes: Int): Int =
        INTERVAL_OPTIONS.firstOrNull { it == minutes } ?: DEFAULT_INTERVAL_MINUTES

    fun load(): HomeDashboardRefreshSettings {
        val prefs = kvStore(PREFS_NAME)
        return HomeDashboardRefreshSettings(
            autoRefreshEnabled = prefs.getBoolean(AUTO_REFRESH_ENABLED_KEY, true),
            refreshIntervalMinutes = normalizeInterval(
                prefs.getInt(REFRESH_INTERVAL_MINUTES_KEY, DEFAULT_INTERVAL_MINUTES)
            )
        )
    }

    fun persist(settings: HomeDashboardRefreshSettings) {
        val prefs = kvStore(PREFS_NAME)
        prefs.putBoolean(AUTO_REFRESH_ENABLED_KEY, settings.autoRefreshEnabled)
        prefs.putInt(
            REFRESH_INTERVAL_MINUTES_KEY,
            normalizeInterval(settings.refreshIntervalMinutes)
        )
    }
}
