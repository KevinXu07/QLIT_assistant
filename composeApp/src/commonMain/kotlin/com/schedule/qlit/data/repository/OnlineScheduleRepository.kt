package com.schedule.qlit.data.repository

import com.schedule.qlit.data.IODispatcher
import com.schedule.qlit.data.model.Course
import com.schedule.qlit.data.model.CourseMeta
import com.schedule.qlit.data.model.ScheduleData
import com.schedule.qlit.data.model.TimeSlot
import com.schedule.qlit.data.net.Http
import com.schedule.qlit.data.store.kvStore
import io.ktor.http.encodeURLQueryComponent
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.time.Clock

data class OnlineScheduleLoginResult(
    val ok: Boolean,
    val token: String = "",
    val needCaptcha: Boolean = false,
    val errMsg: String = "",
    val failCount: Int = 0
)

data class OnlineScheduleTermInfo(
    val currentWeek: Int,
    val xnm: String,
    val xqm: String
)

data class OnlineScheduleImportResult(
    val schedule: ScheduleData,
    val token: String
)

object OnlineScheduleRepository {
    private const val DEFAULT_BASE_URL = "https://urp.kevinxu.cn"
    private const val PREFS_NAME = "online_schedule_prefs"
    private const val KEY_BASE_URL = "base_url"
    private const val KEY_USERNAME = "username"

    private val json = Json { ignoreUnknownKeys = true }

    private val slotMap = linkedMapOf(
        "1-2" to TimeSlot("1-2节", "08:30-10:05"),
        "3-4" to TimeSlot("3-4节", "10:20-11:55"),
        "5-6" to TimeSlot("5-6节", "12:30-14:05"),
        "7-8" to TimeSlot("7-8节", "14:15-15:50"),
        "9-10" to TimeSlot("9-10节", "16:00-17:35"),
        "11-12" to TimeSlot("11-12节", "18:35-20:10")
    )

    private val slotMapB = slotMap.toMutableMap().apply {
        this["3-4"] = TimeSlot("3-4节", "10:35-12:10")
    }.toMap()

    fun loadSavedBaseUrl(): String =
        kvStore(PREFS_NAME).getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL

    fun loadSavedUsername(): String =
        kvStore(PREFS_NAME).getString(KEY_USERNAME, "") ?: ""

    fun saveBaseUrlAndUsername(
        baseUrl: String,
        username: String
    ) {
        val store = kvStore(PREFS_NAME)
        store.putString(KEY_BASE_URL, normalizeBaseUrl(baseUrl))
        store.putString(KEY_USERNAME, username.trim())
    }

    fun defaultBaseUrl(): String = DEFAULT_BASE_URL

    /** 验证码图片字节（JPEG/PNG）；解码交给各平台 UI 层。 */
    suspend fun fetchCaptchaBytes(
        baseUrl: String,
        token: String
    ): ByteArray? = withContext(IODispatcher) {
        if (token.isBlank()) return@withContext null
        runCatching {
            val url = "${normalizeBaseUrl(baseUrl)}/proxy2/proxy2-captcha?time=${Clock.System.now().toEpochMilliseconds()}&token=${token.encodeURLQueryComponent(spaceToPlus = true)}"
            Http.get(url).body
        }.getOrNull()
    }

    suspend fun login(
        baseUrl: String,
        username: String,
        password: String,
        captcha: String?
    ): OnlineScheduleLoginResult = withContext(IODispatcher) {
        val normalizedBase = normalizeBaseUrl(baseUrl)
        val body = buildJsonObject {
            put("username", username.trim())
            put("password", password)
            if (!captcha.isNullOrBlank()) {
                put("captcha", captcha.trim())
            }
        }.toString().encodeToByteArray()

        runCatching {
            val raw = Http.post(
                "$normalizedBase/proxy2-login",
                body,
                headers = mapOf(
                    "Content-Type" to "application/json",
                    "Accept" to "application/json",
                ),
            ).text()
            val map = json.parseToJsonElement(raw) as? JsonObject ?: JsonObject(emptyMap())
            OnlineScheduleLoginResult(
                ok = map["ok"]?.jsonPrimitive?.contentOrNull == "true",
                token = map["token"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                needCaptcha = map["needCaptcha"]?.jsonPrimitive?.contentOrNull == "true",
                errMsg = map["errMsg"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                failCount = map["failCount"]?.jsonPrimitive?.intOrNull
                    ?: map["failCount"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0
            )
        }.getOrElse {
            OnlineScheduleLoginResult(
                ok = false,
                errMsg = it.message ?: "登录失败"
            )
        }
    }

    suspend fun importSchedule(
        baseUrl: String,
        token: String
    ): OnlineScheduleImportResult = withContext(IODispatcher) {
        val normalizedBase = normalizeBaseUrl(baseUrl)
        val termInfo = fetchTermInfo(normalizedBase, token)
        val scheduleMap = fetchScheduleMap(
            baseUrl = normalizedBase,
            token = token,
            xnm = termInfo.xnm,
            xqm = termInfo.xqm
        )
        val schedule = transformSchedule(
            scheduleMap = scheduleMap,
            currentWeek = termInfo.currentWeek,
            xnm = termInfo.xnm,
            xqm = termInfo.xqm
        )
        OnlineScheduleImportResult(schedule = schedule, token = token)
    }

    private suspend fun fetchTermInfo(
        normalizedBase: String,
        token: String
    ): OnlineScheduleTermInfo {
        val raw = Http.get(
            "$normalizedBase/proxy2-terminfo",
            headers = mapOf(
                "Accept" to "application/json",
                "X-New-Session-Token" to token,
            ),
        ).text()
        val map = json.parseToJsonElement(raw) as? JsonObject ?: JsonObject(emptyMap())
        val xnm = map["xnm"]?.jsonPrimitive?.contentOrNull.orEmpty().ifBlank { guessAcademicYear() }
        val xqm = map["xqm"]?.jsonPrimitive?.contentOrNull.orEmpty().ifBlank { guessTermCode() }
        val currentWeek = map["dqzc"]?.jsonPrimitive?.intOrNull
            ?: map["dqzc"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 1
        return OnlineScheduleTermInfo(
            currentWeek = currentWeek,
            xnm = xnm,
            xqm = xqm
        )
    }

    private suspend fun fetchScheduleMap(
        baseUrl: String,
        token: String,
        xnm: String,
        xqm: String
    ): JsonObject {
        val body = listOf(
            "xnm" to xnm,
            "xqm" to xqm,
            "kzlx" to "ck",
            "xsdm" to "",
            "kclbdm" to "",
            "kclxdm" to ""
        ).joinToString("&") { (key, value) ->
            "${key.encodeURLQueryComponent(spaceToPlus = true)}=${value.encodeURLQueryComponent(spaceToPlus = true)}"
        }

        val raw = Http.post(
            "$baseUrl/proxy2/jwglxt/kbcx/xskbcx_cxXsgrkb.html?gnmkdm=N2151",
            body.encodeToByteArray(),
            headers = mapOf(
                "Content-Type" to "application/x-www-form-urlencoded",
                "Accept" to "application/json",
                "X-Requested-With" to "XMLHttpRequest",
                "X-New-Session-Token" to token,
            ),
        ).text()
        val map = json.parseToJsonElement(raw) as? JsonObject ?: JsonObject(emptyMap())
        val xsxx = map["xsxx"] as? JsonObject
        if (xsxx?.get("XH")?.jsonPrimitive?.contentOrNull.isNullOrBlank()) {
            error("课表读取失败，请重新登录")
        }
        return map
    }

    internal fun transformSchedule(
        scheduleMap: JsonObject,
        currentWeek: Int,
        xnm: String,
        xqm: String
    ): ScheduleData {
        val xsxx = scheduleMap["xsxx"] as? JsonObject ?: JsonObject(emptyMap())
        val kbList = scheduleMap["kbList"]?.jsonArray ?: emptyList()

        val meta = CourseMeta(
            studentId = xsxx["XH"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            name = xsxx["XM"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            academicYear = xnm,
            term = if (xqm == "3") 1 else 2,
            termLabel = if (xqm == "3") "第一学期" else "第二学期",
            currentWeek = currentWeek,
            exportTime = Clock.System.now().toString()
        )

        val courses = kbList.mapNotNull { element ->
            val item = element as? JsonObject ?: return@mapNotNull null
            val slot = item["jcs"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val dayOfWeek = item["xqj"]?.jsonPrimitive?.intOrNull
                ?: item["xqj"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
                ?: return@mapNotNull null
            val classroom = item["cdmc"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val slotInfo = if (classroom.contains("JB", ignoreCase = true)) {
                slotMapB[slot]
            } else {
                slotMap[slot]
            }
            Course(
                name = item["kcmc"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                dayOfWeek = dayOfWeek,
                slot = slot,
                startTime = slotInfo?.time?.substringBefore("-").orEmpty(),
                endTime = slotInfo?.time?.substringAfter("-", "").orEmpty(),
                classroom = classroom,
                teacher = item["xm"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                weeks = item["zcd"]?.jsonPrimitive?.contentOrNull.orEmpty()
            )
        }

        return ScheduleData(
            meta = meta,
            timeSlots = slotMap,
            courses = courses
        )
    }

    private fun guessAcademicYear(): String {
        val (year, month) = currentYearMonth()
        return (if (month >= 8) year else year - 1).toString()
    }

    private fun guessTermCode(): String {
        val (_, month) = currentYearMonth()
        return if (month in 2..7) "12" else "3"
    }

    private fun normalizeBaseUrl(baseUrl: String): String =
        baseUrl.trim().removeSuffix("/")
}

/** 当前本地 (年, 月 1-12)。 */
internal expect fun currentYearMonth(): Pair<Int, Int>
