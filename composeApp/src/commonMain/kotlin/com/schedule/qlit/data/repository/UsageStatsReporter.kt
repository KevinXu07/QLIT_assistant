package com.schedule.qlit.data.repository

import com.schedule.qlit.data.IODispatcher
import com.schedule.qlit.data.deviceInfo
import com.schedule.qlit.data.net.Http
import com.schedule.qlit.data.store.KeyValueStore
import com.schedule.qlit.data.store.kvStore
import io.ktor.http.encodeURLQueryComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.concurrent.Volatile
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * 匿名使用统计。
 *
 * 首次启动弹窗征求用户同意后才开始收集；同意与否都会向服务端上报
 * 一次选择结果（仅设备随机 UUID + App 版本，拒绝者不含任何设备信息），
 * 供后台统计同意/拒绝占比。同意用户每天首次启动上报一条匿名 ping：
 * 品牌 / 型号 / 系统版本 / App 版本。所有请求失败静默，绝不影响启动。
 */
object UsageStatsReporter {

    private const val PING_URL = "https://api.kevinxu.cn/ping"
    private const val PREFS_NAME = "anonymous_usage_stats"
    private const val KEY_DEVICE_ID = "device_id"
    private const val KEY_LAST_PING_DAY = "last_ping_day"
    private const val KEY_CONSENT = "consent"
    private const val KEY_CONSENT_REPORTED = "consent_reported"

    private fun prefs(): KeyValueStore = kvStore(PREFS_NAME)

    /** 三态：null=尚未询问 */
    fun getConsent(): Boolean? =
        prefs().getString(KEY_CONSENT)?.toBooleanStrictOrNull()

    fun needsConsent(): Boolean = getConsent() == null

    /** 进程内去重，避免并发请求 */
    private val pingScope = CoroutineScope(SupervisorJob() + IODispatcher)
    @Volatile
    private var inFlight = false

    /**
     * 记录用户选择并上报一次结果。
     * - 拒绝：只发 uid + 版本 + consent=0，不含任何设备信息；
     * - 同意：单条请求附带设备信息（consent=1），同时充当当日 ping。
     */
    fun setConsent(agreed: Boolean) {
        val prefs = prefs()
        prefs.putString(KEY_CONSENT, agreed.toString())
        if (!agreed) {
            dispatch(
                query = baseQuery(prefs) + "&consent=0",
                onSuccess = { prefs.putBoolean(KEY_CONSENT_REPORTED, true) },
            )
        } else {
            dispatch(
                query = deviceQuery(prefs) + "&consent=1",
                onSuccess = {
                    prefs.putBoolean(KEY_CONSENT_REPORTED, true)
                    prefs.putString(KEY_LAST_PING_DAY, today())
                },
            )
        }
    }

    /** 同意用户的每日匿名 ping；未同意或当天已发则直接返回 */
    fun reportOnceDailyAsync() {
        val prefs = prefs()
        val consent = prefs.getString(KEY_CONSENT) ?: return
        val today = today()
        // 未上报的同意/拒绝结果都要补发（服务端靠它统计同意占比）
        val consentRetry = if (!prefs.getBoolean(KEY_CONSENT_REPORTED, false)) "&consent=${if (consent == "true") "1" else "0"}" else ""
        // 拒绝用户没有每日 ping，只有待补发的选择结果才需要请求
        if (consent != "true" && consentRetry.isEmpty()) return
        if (consent == "true" && prefs.getString(KEY_LAST_PING_DAY, "") == today && consentRetry.isEmpty()) return
        dispatch(
            query = if (consent == "true") deviceQuery(prefs) + consentRetry
                    else baseQuery(prefs) + consentRetry,
            onSuccess = {
                if (consent == "true") prefs.putString(KEY_LAST_PING_DAY, today)
                prefs.putBoolean(KEY_CONSENT_REPORTED, true)
            },
        )
    }

    // ── 内部 ────────────────────────────────────────────────

    private fun today(): String {
        val date = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val mm = date.monthNumber.toString().padStart(2, '0')
        val dd = date.dayOfMonth.toString().padStart(2, '0')
        return "${date.year}$mm$dd"
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun ensureDeviceId(prefs: KeyValueStore): String =
        prefs.getString(KEY_DEVICE_ID) ?: Uuid.random().toString().also {
            prefs.putString(KEY_DEVICE_ID, it)
        }

    /** 基础参数（uid + 版本 + 防缓存时间戳），拒绝上报只含这些 */
    private fun baseQuery(prefs: KeyValueStore): String =
        "?uid=${url(ensureDeviceId(prefs))}&v=${url(deviceInfo().versionName)}&_=${Clock.System.now().toEpochMilliseconds()}"

    /** 完整设备参数（同意用户的每日 ping） */
    private fun deviceQuery(prefs: KeyValueStore): String {
        val info = deviceInfo()
        return baseQuery(prefs) +
            "&brand=${url(info.brand)}" +
            "&model=${url(info.model)}" +
            "&os=${url(info.osVersion)}" +
            "&sdk=${info.sdk}" +
            "&rom=${url(info.rom)}"
    }

    private fun url(value: String): String =
        value.ifBlank { "unknown" }.encodeURLQueryComponent(spaceToPlus = true)

    private fun dispatch(query: String, onSuccess: () -> Unit) {
        if (inFlight) return
        inFlight = true
        pingScope.launch {
            try {
                Http.get(PING_URL + query, timeoutMs = 4_000)
                onSuccess()
            } catch (_: Exception) {
                // 失败静默：每日 ping 与同意结果下次启动都会按标记补发
            } finally {
                inFlight = false
            }
        }
    }
}
