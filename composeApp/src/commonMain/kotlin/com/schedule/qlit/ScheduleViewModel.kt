package com.schedule.qlit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schedule.qlit.data.model.Course
import com.schedule.qlit.data.model.ScheduleData
import com.schedule.qlit.data.repository.CourseRepository
import com.schedule.qlit.data.store.ScheduleStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** UI 状态封装 */
data class TodayUiState(
    val greeting: String = "",
    val userName: String = "",
    val todayCourses: List<Course> = emptyList(),
    val upcomingCourse: Course? = null,
    val minutesUntil: Int = 0,
    val breakProgress: Int = 0,
    val currentWeek: Int = 1
)

@OptIn(ExperimentalTime::class)
class ScheduleViewModel : ViewModel() {

    private val scheduleStore = ScheduleStore()
    val schedule: StateFlow<ScheduleData?> = scheduleStore.schedule

    // 定时器 Flow：对齐到整分钟发射当前时间戳，用于驱动主页倒计时与课表时间线
    private val _ticker = MutableStateFlow(Clock.System.now().toEpochMilliseconds())

    /** Wall clock that ticks on minute boundaries (every 10 s right before class). */
    val clock: StateFlow<Long> = _ticker

    init {
        // 加载初始数据
        viewModelScope.launch {
            scheduleStore.load()
        }

        // 动态计时器：距上课 <=10 分钟时 10 秒一刷，其余时段在下一个整分钟刷新
        viewModelScope.launch {
            while (true) {
                val now = Clock.System.now().toEpochMilliseconds()
                _ticker.value = now
                val mins = computeMinutesUntil()
                val interval = if (mins in 1..10) 10_000L else 60_000L - now % 60_000L + 50L
                delay(interval)
            }
        }
    }

    /** Re-emit the clock right away (e.g. when the app returns to the foreground). */
    fun refreshClock() {
        _ticker.value = Clock.System.now().toEpochMilliseconds()
    }

    /** 算一下距下一节课还有多少分钟；没课或已过点返回 0 */
    private fun computeMinutesUntil(): Int {
        val s = scheduleStore.schedule.value ?: return 0
        val now = CourseRepository.currentTimeMinutes()
        val dayOfWeek = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).dayOfWeek.isoDayNumber
        val today = CourseRepository.getTodayCourses(s, dayOfWeek)
        return today
            .filter { CourseRepository.timeToMinutes(it.startTime) > now }
            .minByOrNull { CourseRepository.timeToMinutes(it.startTime) }
            ?.let { CourseRepository.timeToMinutes(it.startTime) - now }
            ?: 0
    }

    /** 核心逻辑：组合课表数据与时间状态 */
    val todayUiState: StateFlow<TodayUiState> = combine(scheduleStore.schedule, _ticker) { s, _ ->
        if (s == null) return@combine TodayUiState(greeting = getGreeting())

        val now = CourseRepository.currentTimeMinutes()
        val week = CourseRepository.calculateCurrentWeek(s.meta)
        val dayOfWeek = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).dayOfWeek.isoDayNumber

        val today = CourseRepository.getTodayCourses(s, dayOfWeek)

        // 查找下一节课
        val next = today
            .filter { CourseRepository.timeToMinutes(it.startTime) > now }
            .minByOrNull { CourseRepository.timeToMinutes(it.startTime) }

        val mins = next?.let { CourseRepository.timeToMinutes(it.startTime) - now } ?: 0

        TodayUiState(
            greeting = getGreeting(),
            userName = s.meta.name.take(1) + "同学",
            todayCourses = today,
            upcomingCourse = if (mins in 1..10) next else null,
            minutesUntil = mins,
            breakProgress = ((10 - mins) * 10).coerceIn(0, 100),
            currentWeek = week
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TodayUiState()
    )

    private fun getGreeting(): String {
        return when (Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).hour) {
            in 5..11  -> "早上好"
            in 12..13 -> "中午好"
            in 14..17 -> "下午好"
            else      -> "晚上好"
        }
    }

    fun updateSchedule(newSchedule: ScheduleData?) {
        scheduleStore.replace(newSchedule)
    }

    suspend fun importSchedule(schedule: ScheduleData) {
        scheduleStore.importSchedule(schedule)
    }

    suspend fun saveSchedule(schedule: ScheduleData) {
        scheduleStore.save(schedule)
    }

    suspend fun downloadSchedule(url: String): ScheduleData? {
        return scheduleStore.downloadAndSave(url)
    }

    suspend fun clearSchedule() {
        scheduleStore.clear()
    }
}
