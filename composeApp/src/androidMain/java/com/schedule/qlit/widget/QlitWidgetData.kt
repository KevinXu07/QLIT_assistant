package com.schedule.qlit.widget

import android.content.Context
import com.schedule.qlit.data.repository.ChaoxingRepository
import com.schedule.qlit.data.repository.CourseRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * 小组件数据装配：课表本地读，超星待办走网络（带超时与缓存兜底），
 * 淘宝取件读 App 已持久化的偏好（它的登录态依赖 WebView，桌面侧只读）。
 */
object QlitWidgetData {

    private const val NETWORK_BUDGET_MS = 8_000L

    enum class CourseState { DONE, ONGOING, UPCOMING }

    data class CourseRow(
        val time: String,
        val name: String,
        val room: String,
        val state: CourseState,
    )

    data class StatRow(
        val iconRes: Int,
        val label: String,
        val value: String,
        val toolDestination: String,
    )

    data class WidgetData(
        val dateLine: String,
        val hasSchedule: Boolean,
        val courses: List<CourseRow>,
        val totalCourses: Int,
        val stats: List<StatRow>,
        /** 下一次需要刷新显示的本地时间点（课程上下课边界或跨天零点） */
        val nextBoundaryAt: Long?,
    )

    suspend fun load(context: Context): WidgetData = coroutineScope {
        val now = Calendar.getInstance()
        val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        // ── 今日课程（本地文件，毫秒级）──────────────────────
        val schedule = CourseRepository.loadSchedule()
        val dayOfWeek = now.get(Calendar.DAY_OF_WEEK).let { if (it == Calendar.SUNDAY) 7 else it - 1 }
        val todayCourses = schedule?.let { CourseRepository.getTodayCourses(it, dayOfWeek) }
            .orEmpty()
        val courseRows = todayCourses.map { course ->
            val start = CourseRepository.timeToMinutes(course.startTime)
            val end = CourseRepository.timeToMinutes(course.endTime)
            CourseRow(
                time = course.startTime,
                name = course.name,
                room = course.classroom,
                state = when {
                    nowMinutes >= end -> CourseState.DONE
                    nowMinutes >= start -> CourseState.ONGOING
                    else -> CourseState.UPCOMING
                },
            )
        }
        val week = schedule?.let { CourseRepository.calculateCurrentWeek(it.meta) }
        val dateLine = buildString {
            append(SimpleDateFormat("M月d日 E", Locale.CHINA).format(now.time))
            week?.let { append(" · 第").append(it).append("周") }
        }

        // ── 网络统计并行拉取（共享 8s 预算）─────────────────────
        val chaoxingDeferred = async {
            withTimeoutOrNull(NETWORK_BUDGET_MS) { loadChaoxing(context) }
        }
        val chaoxing = chaoxingDeferred.await() ?: cachedChaoxingOrFallback(context)

        val stats = listOf(
            StatRow(
                iconRes = com.schedule.qlit.R.drawable.ic_tool_pickup,
                label = "取件",
                value = loadTaobao(context),
                toolDestination = "TAOBAO_PICKUP",
            ),
            StatRow(
                iconRes = com.schedule.qlit.R.drawable.ic_tool_homework,
                label = "待办",
                value = chaoxing,
                toolDestination = "CHAOXING",
            ),
        )

        WidgetData(
            dateLine = dateLine,
            hasSchedule = schedule != null,
            courses = courseRows,
            totalCourses = courseRows.size,
            stats = stats,
            nextBoundaryAt = nextBoundary(now, todayCourses.map { it.startTime to it.endTime }),
        )
    }

    /**
     * 超星待办。
     *
     * widget 独立进程里的 SharedPreferences 可能是旧镜像，登录态以
     * 缓存文件为准：LOGGED_OUT（退出登录）直接显示未登录；
     * prefs 还没同步到 cookie 但文件已有 OK 数据时，先展示主进程刚写入的计数。
     */
    private suspend fun loadChaoxing(context: Context): String {
        val (cachedWork, cachedExam, fileState) = QlitWidgetCache.loadChaoxing(context)
        if (fileState == QlitWidgetCache.CX_STATE_LOGGED_OUT) return "未登录"
        if (!ChaoxingRepository.isLoggedIn()) {
            return if (fileState == QlitWidgetCache.CX_STATE_OK) {
                formatChaoxingPending(cachedWork + cachedExam)
            } else {
                "未登录"
            }
        }
        return try {
            val result = ChaoxingRepository.fetchTasks()
            formatChaoxingPending(result.pendingWorkCount + result.pendingExamCount)
        } catch (e: ChaoxingRepository.CookieExpiredException) {
            "已过期"
        } catch (_: Exception) {
            cachedChaoxingOrFallback(context)
        }
    }

    private fun formatChaoxingPending(pending: Int): String =
        if (pending > 0) "$pending 项" else "已完成"

    private fun cachedChaoxingOrFallback(context: Context): String {
        val (work, exam, state) = QlitWidgetCache.loadChaoxing(context)
        return when {
            state == QlitWidgetCache.CX_STATE_LOGGED_OUT -> "未登录"
            state == QlitWidgetCache.CX_STATE_EXPIRED -> "已过期"
            state == QlitWidgetCache.CX_STATE_OK -> formatChaoxingPending(work + exam)
            else -> "获取失败"
        }
    }

    /**
     * 淘宝取件只读计数（它的登录态依赖 WebView，桌面侧不发起刷新）。
     * 优先 widget 缓存文件；文件没有记录时回退 SharedPreferences（覆盖升级后的老安装）。
     */
    private fun loadTaobao(context: Context): String {
        val (loggedIn, pending) = QlitWidgetCache.loadTaobao(context) ?: run {
            val prefs = context.getSharedPreferences("taobao_pickup_prefs", Context.MODE_PRIVATE)
            prefs.getBoolean("home_login_detected", false) to
                prefs.getInt("home_pending_count", -1)
        }
        if (!loggedIn) return "未登录"
        return when {
            pending > 0 -> "$pending 件"
            pending == 0 -> "无待取"
            else -> "—"
        }
    }

    /** 课程状态翻转点（上课/下课时间）与跨天零点中最近的未来时刻。 */
    private fun nextBoundary(now: Calendar, courseTimes: List<Pair<String, String>>): Long? {
        val candidates = mutableListOf<Calendar>()
        for ((start, end) in courseTimes) {
            for (time in listOf(start, end)) {
                val parts = time.split(":")
                if (parts.size != 2) continue
                val boundary = (now.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, parts[0].toIntOrNull() ?: return@apply)
                    set(Calendar.MINUTE, parts[1].toIntOrNull() ?: return@apply)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (boundary.after(now)) candidates += boundary
            }
        }
        val midnight = (now.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        candidates += midnight
        return candidates.minByOrNull { it.timeInMillis }?.timeInMillis
    }
}
