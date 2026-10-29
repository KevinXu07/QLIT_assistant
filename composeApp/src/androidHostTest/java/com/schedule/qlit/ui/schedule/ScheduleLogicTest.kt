package com.schedule.qlit.ui.schedule

import com.schedule.qlit.data.model.Course
import com.schedule.qlit.data.model.CourseMeta
import com.schedule.qlit.data.model.ScheduleData
import com.schedule.qlit.data.model.TimeSlot
import com.schedule.qlit.ui.theme.CoursePalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

class ScheduleLogicTest {

    private val slots = linkedMapOf(
        "1-2" to TimeSlot("1-2节", "08:30-10:05"),
        "3-4" to TimeSlot("3-4节", "10:20-11:55"),
        "5-6" to TimeSlot("5-6节", "12:30-14:05"),
        "7-8" to TimeSlot("7-8节", "14:15-15:50"),
    )

    private fun course(name: String, day: Int, slot: String, weeks: String = "1-16") {
        val slotInfo = slots.getValue(slot)
        courses += Course(
            name = name,
            dayOfWeek = day,
            slot = slot,
            startTime = slotInfo.time.substringBefore("-"),
            endTime = slotInfo.time.substringAfter("-"),
            classroom = "JB101",
            teacher = "T",
            weeks = weeks,
        )
    }

    private val courses = mutableListOf<Course>()

    private fun schedule() = ScheduleData(
        meta = CourseMeta("1", "徐同学", "2026-2027", 1, "第一学期", 7, "2026-10-19T00:00:00"),
        timeSlots = slots,
        courses = courses.toList(),
    )

    // 2026-10-19 is a Monday.
    private val monday = LocalDate(2026, 10, 19)

    @Test
    fun hero_beforeFirstClass_isNext() {
        course("A", 1, "1-2")
        course("B", 1, "3-4")
        val hero = ScheduleLogic.heroState(schedule(), monday, nowMinutes = 8 * 60, currentWeek = 7)
        assertTrue(hero is HeroState.Next)
        hero as HeroState.Next
        assertEquals("A", hero.course.name)
        assertEquals(30, hero.minutesUntil)
    }

    @Test
    fun hero_duringClass_isNowWithMinutesLeft() {
        course("A", 1, "1-2")
        val hero = ScheduleLogic.heroState(schedule(), monday, nowMinutes = 9 * 60 + 5, currentWeek = 7)
        assertEquals(HeroState.Now(courses[0], 60), hero)
    }

    @Test
    fun hero_betweenClasses_pointsToNext() {
        course("A", 1, "1-2")
        course("B", 1, "3-4")
        val hero = ScheduleLogic.heroState(schedule(), monday, nowMinutes = 10 * 60 + 10, currentWeek = 7)
        assertEquals(HeroState.Next(courses[1], 10), hero)
    }

    @Test
    fun hero_afterLastClass_pointsToTomorrow() {
        course("A", 1, "1-2")
        course("C", 2, "3-4")
        val hero = ScheduleLogic.heroState(schedule(), monday, nowMinutes = 20 * 60, currentWeek = 7)
        hero as HeroState.Later
        assertEquals("C", hero.course.name)
        assertEquals(1, hero.dayOffset)
        assertTrue(hero.todayHadClasses)
    }

    @Test
    fun hero_skipsWeekendAndRespectsNextWeekRange() {
        // Only on Monday from week 8 onwards: next class is next Monday.
        course("Late", 1, "1-2", weeks = "8-16")
        val friday = monday.plus(DatePeriod(days = 4))
        val hero = ScheduleLogic.heroState(schedule(), friday, nowMinutes = 12 * 60, currentWeek = 7)
        hero as HeroState.Later
        assertEquals(monday.plus(DatePeriod(days = 7)), hero.date)
        assertFalse(hero.todayHadClasses)
    }

    @Test
    fun hero_nothingAhead_isFree() {
        course("Old", 1, "1-2", weeks = "1-3")
        assertEquals(HeroState.Free, ScheduleLogic.heroState(schedule(), monday, 8 * 60, currentWeek = 7))
    }

    @Test
    fun datesOfWeek_offsetsFromCurrentWeek() {
        val dates = ScheduleLogic.datesOfWeek(week = 8, today = monday.plus(DatePeriod(days = 2)), currentWeek = 7)
        assertEquals(monday.plus(DatePeriod(days = 7)), dates.first())
        assertEquals(monday.plus(DatePeriod(days = 13)), dates.last())
        assertEquals(7, ScheduleLogic.weekOf(monday.plus(DatePeriod(days = 6)), monday, 7))
        assertEquals(8, ScheduleLogic.weekOf(monday.plus(DatePeriod(days = 7)), monday, 7))
    }

    @Test
    fun gridBlocks_marksOtherWeekCoursesAsGhosts() {
        course("This", 1, "1-2", weeks = "1-16")
        course("Odd", 2, "3-4", weeks = "1-16(单)")
        val data = schedule()
        val gridSlots = ScheduleLogic.gridSlots(data)
        val blocks = ScheduleLogic.gridBlocks(data, week = 8, slots = gridSlots)
        assertTrue(blocks.getValue(1).single().activeThisWeek)
        assertFalse(blocks.getValue(2).single().activeThisWeek)
        assertEquals(listOf(1, 2, 3, 4, 5), ScheduleLogic.visibleDays(blocks))
    }

    @Test
    fun visibleDays_saturdayOnlyAddsSaturday() {
        course("Sat", 6, "1-2")
        val data = schedule()
        val blocks = ScheduleLogic.gridBlocks(data, 7, ScheduleLogic.gridSlots(data))
        assertEquals(listOf(1, 2, 3, 4, 5, 6), ScheduleLogic.visibleDays(blocks))
    }

    @Test
    fun placeCourse_spansRowsByEndTime() {
        val data = schedule()
        val lab = Course("Lab", 3, "1-4", "08:30", "11:55", "R", "T", "1-16")
        assertEquals(0 to 2, ScheduleLogic.placeCourse(lab, ScheduleLogic.gridSlots(data)))
    }

    @Test
    fun coursePalette_neverUsesSealRed() {
        repeat(200) { index ->
            val hue = CoursePalette.hueForIndex(index)
            assertFalse("hue $hue at $index", CoursePalette.isSealBand(hue))
        }
    }

    @Test
    fun formatDuration() {
        assertEquals("45 分钟", ScheduleLogic.formatDuration(45))
        assertEquals("2 小时", ScheduleLogic.formatDuration(120))
        assertEquals("1 小时 5 分", ScheduleLogic.formatDuration(65))
    }
}
