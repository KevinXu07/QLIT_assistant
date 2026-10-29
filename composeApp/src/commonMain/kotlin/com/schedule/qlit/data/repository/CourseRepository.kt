package com.schedule.qlit.data.repository

import com.schedule.qlit.data.IODispatcher
import com.schedule.qlit.data.model.Course
import com.schedule.qlit.data.model.CourseMeta
import com.schedule.qlit.data.model.ScheduleData
import com.schedule.qlit.data.net.Http
import com.schedule.qlit.data.store.FileStore
import com.schedule.qlit.data.store.kvStore
import kotlinx.coroutines.withContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.Instant

object CourseRepository {

    private const val FILE_NAME = "schedule.json"
    private const val PREFS_NAME = "course_prefs"
    private const val KEY_IMPORT_TIME = "import_time"

    private val json = Json { ignoreUnknownKeys = true }

    /** 保存 JSON 字符串到本地（先写临时文件再重命名，避免并发读到半截内容） */
    fun saveJson(rawJson: String) {
        // 所有写入入口（包括手动编辑）遵守与加载相同的校验，先拒绝再碰正式文件。
        require(parseSchedule(rawJson) != null) { "课表格式不正确" }
        FileStore.writeTextAtomic(FILE_NAME, rawJson)
        refreshWidgets()
    }

    /** 解析并校验导入的课表 JSON；结构不完整（meta/courses/timeSlots 缺失或字段非法）返回 null */
    fun parseSchedule(rawJson: String): ScheduleData? = try {
        json.decodeFromString<ScheduleData>(rawJson).takeIf { s ->
            val m = s.meta
            m.studentId.isNotBlank() && m.name.isNotBlank() &&
                m.exportTime.isNotBlank() &&
                m.academicYear.isNotBlank() &&
                m.termLabel.isNotBlank() &&
                s.courses.all { c -> courseValidationError(c) == null } &&
                s.timeSlots.values.all { t ->
                    t.label.isNotBlank() && t.time.isNotBlank() &&
                        // time 是时间段（如 08:30-10:05）：两侧分别校验 HH:MM
                        t.time.split("-").let { parts ->
                            parts.size == 2 &&
                                TIME_FORMAT.matches(parts[0].trim()) &&
                                TIME_FORMAT.matches(parts[1].trim())
                        }
                }
        }
    } catch (_: Exception) {
        null
    }

    /** HH:MM（24 小时制）格式 */
    private val TIME_FORMAT = Regex("^([01]?[0-9]|2[0-3]):[0-5][0-9]$")

    /** 导入与编辑共用字段校验。 */
    fun courseValidationError(course: Course): String? = when {
        course.name.isBlank() -> "请填写课程名称"
        course.dayOfWeek !in 1..7 -> "请选择有效的星期"
        course.slot.isBlank() -> "请填写节次"
        parseTimeMinutes(course.startTime) == null -> "开始时间格式不正确，请填写 HH:MM"
        parseTimeMinutes(course.endTime) == null -> "结束时间格式不正确，请填写 HH:MM"
        course.weeks.isBlank() -> "请填写周次"
        else -> null
    }

    /** 与课表导入相同的 24 小时制校验，调用方可安全区分非法输入与午夜。 */
    fun parseTimeMinutes(time: String): Int? {
        if (!TIME_FORMAT.matches(time)) return null
        val parts = time.split(":")
        return parts[0].toInt() * 60 + parts[1].toInt()
    }

    /**
     * 记录最近一次“导入”发生的时间（链接/在线/文件导入成功时调用，
     * 手动增删课程不刷新），用于课表页展示课表新鲜度。
     */
    fun recordImportTime() {
        kvStore(PREFS_NAME).putLong(KEY_IMPORT_TIME, Clock.System.now().toEpochMilliseconds())
    }

    /** 最近一次导入时间；从未导入过（或老版本安装）返回 null */
    fun getImportTime(): Long? {
        val value = kvStore(PREFS_NAME).getLong(KEY_IMPORT_TIME, 0L)
        return value.takeIf { it > 0L }
    }

    fun clearSchedule() {
        FileStore.delete(FILE_NAME)
        kvStore(PREFS_NAME).remove(KEY_IMPORT_TIME)
        refreshWidgets()
    }

    /** 读取本地保存的课表 */
    suspend fun loadSchedule(): ScheduleData? {
        return withContext(IODispatcher) {
            try {
                FileStore.readText(FILE_NAME)?.let { parseSchedule(it) }
            } catch (e: Exception) {
                null
            }
        }
    }

    // ── 时间转换工具 ──────────────────────────────────────────

    fun timeToMinutes(time: String): Int {
        return try {
            val parts = time.split(":")
            parts[0].toInt() * 60 + parts[1].toInt()
        } catch (e: Exception) { 0 }
    }

    fun currentTimeMinutes(): Int {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        return now.hour * 60 + now.minute
    }

    // ── 课表逻辑 ────────────────────────────────────────────

    fun isInCurrentWeek(weeks: String, currentWeek: Int): Boolean {
        val normalized = weeks.replace("周", "").replace(" ", "")
        val segments = normalized.split(",")
        for (seg in segments) {
            val isEvenOnly = seg.contains("(双)")
            val isOddOnly = seg.contains("(单)")
            val rangePart = seg.replace("(双)", "").replace("(单)", "")

            if (isEvenOnly && currentWeek % 2 != 0) continue
            if (isOddOnly && currentWeek % 2 == 0) continue

            if (rangePart.contains("-")) {
                val bounds = rangePart.split("-")
                val start = bounds.getOrNull(0)?.toIntOrNull() ?: 0
                val end = bounds.getOrNull(1)?.toIntOrNull() ?: 0
                if (currentWeek in start..end) return true
            } else {
                if (rangePart.toIntOrNull() == currentWeek) return true
            }
        }
        return false
    }

    fun calculateCurrentWeek(meta: CourseMeta): Int = try {
        val exportInstant = parseExportInstant(meta.exportTime) ?: return meta.currentWeek
        val tz = TimeZone.currentSystemDefault()

        fun mondayOf(date: LocalDate): LocalDate =
            date.minus(date.dayOfWeek.ordinal, DateTimeUnit.DAY)

        val nowMonday = mondayOf(Clock.System.now().toLocalDateTime(tz).date)
        val exportMonday = mondayOf(exportInstant.toLocalDateTime(tz).date)
        val weeksDiff = exportMonday.daysUntil(nowMonday) / 7
        (meta.currentWeek + weeksDiff).coerceAtLeast(1)
    } catch (e: Exception) {
        meta.currentWeek
    }

    /** exportTime 兼容两种格式：ISO-8601（带 Z）与旧版 "yyyy-MM-dd'T'HH:mm:ss"（UTC）。 */
    private fun parseExportInstant(text: String): Instant? {
        runCatching { return Instant.parse(text) }
        return runCatching {
            LocalDateTime.parse(text.substringBefore(".").removeSuffix("Z"))
                .toInstant(TimeZone.UTC)
        }.getOrNull()
    }

    /** 获取某天所有课（过滤当前周次，用于周视图列表） */
    fun getCoursesByDay(schedule: ScheduleData, dayOfWeek: Int): List<Course> {
        val currentWeek = calculateCurrentWeek(schedule.meta)
        return schedule.courses
            .filter { it.dayOfWeek == dayOfWeek }
            .filter { isInCurrentWeek(it.weeks, currentWeek) }
            .sortedBy { it.startTime }
    }

    fun getTodayCourses(schedule: ScheduleData, dayOfWeek: Int): List<Course> {
        val currentWeek = calculateCurrentWeek(schedule.meta)
        return schedule.courses
            .filter { it.dayOfWeek == dayOfWeek }
            .filter { isInCurrentWeek(it.weeks, currentWeek) }
            .sortedBy { it.startTime }
    }

    suspend fun downloadAndSave(url: String): Boolean {
        return withContext(IODispatcher) {
            val resp = try {
                Http.get(url, timeoutMs = 10_000)
            } catch (e: Exception) {
                return@withContext false
            }
            try {
                if (resp.code != 200) return@withContext false
                val rawJson = resp.text()
                if (parseSchedule(rawJson) == null) return@withContext false
                saveJson(rawJson)
                recordImportTime()
                true
            } catch (e: Exception) {
                false
            }
        }
    }
}
