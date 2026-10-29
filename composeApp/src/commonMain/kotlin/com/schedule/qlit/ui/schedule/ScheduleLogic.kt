package com.schedule.qlit.ui.schedule

import com.schedule.qlit.data.model.Course
import com.schedule.qlit.data.model.ScheduleData
import com.schedule.qlit.data.repository.CourseRepository
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** Weekday labels, index 0 = Monday. */
val WEEKDAY_SHORT = listOf("一", "二", "三", "四", "五", "六", "日")

/** What the red hero card on the Today page shows. */
sealed interface HeroState {
    /** Next class today; [minutesUntil] > 0. */
    data class Next(val course: Course, val minutesUntil: Int) : HeroState

    /** A class is running; [minutesLeft] > 0. */
    data class Now(val course: Course, val minutesLeft: Int) : HeroState

    /** Today is done (or empty); first class on a later day. */
    data class Later(
        val course: Course,
        val date: LocalDate,
        val dayOffset: Int,
        val todayHadClasses: Boolean,
    ) : HeroState

    /** Nothing scheduled in the next two weeks. */
    data object Free : HeroState
}

/** Per-course state on today's timeline. */
enum class TimelineState { Done, Now, Next, Upcoming }

/** One row of the week grid (a teaching block such as "1-2"). */
data class GridSlot(
    val key: String,
    val label: String,
    val firstPeriod: String,
    val lastPeriod: String,
    val startMinutes: Int,
    val endMinutes: Int,
)

/** A course block placed on the week grid. */
data class GridBlock(
    val course: Course,
    val row: Int,
    val rowSpan: Int,
    /** False when the course belongs to the term but not to the shown week. */
    val activeThisWeek: Boolean,
)

object ScheduleLogic {

    fun startMinutes(course: Course): Int = CourseRepository.timeToMinutes(course.startTime)
    fun endMinutes(course: Course): Int = CourseRepository.timeToMinutes(course.endTime)

    fun coursesOn(schedule: ScheduleData, week: Int, dayOfWeek: Int): List<Course> =
        schedule.courses
            .filter { it.dayOfWeek == dayOfWeek && CourseRepository.isInCurrentWeek(it.weeks, week) }
            .sortedBy { startMinutes(it) }

    /** Monday of [date]'s week. */
    fun mondayOf(date: LocalDate): LocalDate = date.minus(DatePeriod(days = date.dayOfWeek.isoDayNumber - 1))

    /** Teaching week that contains [date], given that [today] is in [currentWeek]. */
    fun weekOf(date: LocalDate, today: LocalDate, currentWeek: Int): Int {
        val diff = mondayOf(today).daysUntil(mondayOf(date)) / 7
        return currentWeek + diff
    }

    /** Monday-to-Sunday dates of [week]. */
    fun datesOfWeek(week: Int, today: LocalDate, currentWeek: Int): List<LocalDate> {
        val monday = mondayOf(today).plus(DatePeriod(days = (week - currentWeek) * 7))
        return (0..6).map { monday.plus(DatePeriod(days = it)) }
    }

    fun heroState(
        schedule: ScheduleData,
        today: LocalDate,
        nowMinutes: Int,
        currentWeek: Int,
        lookAheadDays: Int = 14,
    ): HeroState {
        val todayList = coursesOn(schedule, currentWeek, today.dayOfWeek.isoDayNumber)
        for (course in todayList) {
            val start = startMinutes(course)
            val end = endMinutes(course)
            if (nowMinutes in start until end) return HeroState.Now(course, end - nowMinutes)
            if (nowMinutes < start) return HeroState.Next(course, start - nowMinutes)
        }
        for (offset in 1..lookAheadDays) {
            val date = today.plus(DatePeriod(days = offset))
            val week = weekOf(date, today, currentWeek)
            val first = coursesOn(schedule, week, date.dayOfWeek.isoDayNumber).firstOrNull()
            if (first != null) {
                return HeroState.Later(first, date, offset, todayHadClasses = todayList.isNotEmpty())
            }
        }
        return HeroState.Free
    }

    fun timelineState(course: Course, nowMinutes: Int, hero: HeroState): TimelineState {
        val start = startMinutes(course)
        val end = endMinutes(course)
        return when {
            nowMinutes >= end -> TimelineState.Done
            nowMinutes >= start -> TimelineState.Now
            hero is HeroState.Next && hero.course == course -> TimelineState.Next
            else -> TimelineState.Upcoming
        }
    }

    /** Remaining classes today (not yet finished). */
    fun remainingToday(list: List<Course>, nowMinutes: Int): Int = list.count { nowMinutes < endMinutes(it) }

    /** Idle gap before [index] in minutes (0 for the first class). */
    fun gapBefore(list: List<Course>, index: Int): Int =
        if (index <= 0) 0 else (startMinutes(list[index]) - endMinutes(list[index - 1])).coerceAtLeast(0)

    fun formatDuration(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h == 0 -> "$m 分钟"
            m == 0 -> "$h 小时"
            else -> "$h 小时 $m 分"
        }
    }

    fun formatClock(minutes: Int): String = "${(minutes / 60).toString().padStart(2, '0')}:${(minutes % 60).toString().padStart(2, '0')}"

    // ── Week grid ─────────────────────────────────────────────────────

    fun gridSlots(schedule: ScheduleData): List<GridSlot> {
        val fromTable = schedule.timeSlots.entries.mapNotNull { (key, slot) ->
            val start = slot.time.substringBefore("-").trim()
            val end = slot.time.substringAfter("-", "").trim()
            val startMin = CourseRepository.parseTimeMinutes(start) ?: return@mapNotNull null
            val endMin = CourseRepository.parseTimeMinutes(end) ?: return@mapNotNull null
            GridSlot(
                key = key,
                label = slot.label,
                firstPeriod = key.substringBefore("-"),
                lastPeriod = key.substringAfter("-", key.substringBefore("-")),
                startMinutes = startMin,
                endMinutes = endMin,
            )
        }.sortedBy { it.startMinutes }
        if (fromTable.isNotEmpty()) return fromTable
        // No time table: derive rows from the courses themselves.
        return schedule.courses
            .groupBy { it.slot }
            .map { (key, list) ->
                GridSlot(
                    key = key,
                    label = "${key}节",
                    firstPeriod = key.substringBefore("-"),
                    lastPeriod = key.substringAfter("-", key.substringBefore("-")),
                    startMinutes = list.minOf { startMinutes(it) },
                    endMinutes = list.maxOf { endMinutes(it) },
                )
            }
            .sortedBy { it.startMinutes }
    }

    /** Row index and span for a course: its own slot key, else by time overlap. */
    fun placeCourse(course: Course, slots: List<GridSlot>): Pair<Int, Int>? {
        if (slots.isEmpty()) return null
        val exact = slots.indexOfFirst { it.key == course.slot }
        val start = startMinutes(course)
        val end = endMinutes(course)
        val row = if (exact >= 0) {
            exact
        } else {
            slots.indexOfLast { it.startMinutes <= start }.takeIf { it >= 0 }
                ?: slots.indexOfFirst { it.endMinutes > start }.takeIf { it >= 0 }
                ?: return null
        }
        var span = 1
        while (row + span < slots.size && slots[row + span].startMinutes < end) span++
        return row to span
    }

    /**
     * Blocks for [week]. A cell shows the course of that week; if none, the
     * term's course for that cell is shown as a dashed "not this week" ghost.
     */
    fun gridBlocks(schedule: ScheduleData, week: Int, slots: List<GridSlot>): Map<Int, List<GridBlock>> {
        val byDay = schedule.courses.groupBy { it.dayOfWeek }
        return (1..7).associateWith { day ->
            val placed = byDay[day].orEmpty().mapNotNull { course ->
                placeCourse(course, slots)?.let { (row, span) ->
                    GridBlock(course, row, span, CourseRepository.isInCurrentWeek(course.weeks, week))
                }
            }
            val active = placed.filter { it.activeThisWeek }
            val occupied = active.flatMap { it.row until it.row + it.rowSpan }.toSet()
            val ghosts = placed
                .filter { !it.activeThisWeek && (it.row until it.row + it.rowSpan).none { r -> r in occupied } }
                .distinctBy { it.row }
            (active.distinctBy { it.row } + ghosts).sortedBy { it.row }
        }
    }

    /** Monday–Friday, plus Saturday if anything happens on the weekend, plus Sunday if needed. */
    fun visibleDays(blocks: Map<Int, List<GridBlock>>): List<Int> {
        val sat = blocks[6].orEmpty().any { it.activeThisWeek }
        val sun = blocks[7].orEmpty().any { it.activeThisWeek }
        return buildList {
            addAll(1..5)
            if (sat || sun) add(6)
            if (sun) add(7)
        }
    }

    /** Max teaching week found in the course table (falls back to 20). */
    fun lastWeek(schedule: ScheduleData): Int {
        val numbers = schedule.courses.flatMap { course ->
            Regex("\\d+").findAll(course.weeks).map { it.value.toInt() }.toList()
        }
        return (numbers.maxOrNull() ?: 20).coerceIn(1, 30)
    }

    /** Human readable weeks: "1-16周(单)" → "第 1–16 周 · 单周". */
    fun describeWeeks(weeks: String): String {
        val normalized = weeks.replace("周", "")
        val odd = normalized.contains("(单)")
        val even = normalized.contains("(双)")
        val core = normalized.replace("(单)", "").replace("(双)", "").replace("-", "–").replace(",", "、")
        return buildString {
            append("第 ").append(core).append(" 周")
            if (odd) append(" · 单周")
            if (even) append(" · 双周")
        }
    }
}
