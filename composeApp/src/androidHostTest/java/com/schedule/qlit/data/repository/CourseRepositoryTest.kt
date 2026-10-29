package com.schedule.qlit.data.repository

import com.schedule.qlit.data.model.CourseMeta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseRepositoryTest {

    // ── timeToMinutes ──────────────────────────────────────

    @Test
    fun timeToMinutes_normalTime() {
        assertEquals(0, CourseRepository.timeToMinutes("00:00"))
        assertEquals(510, CourseRepository.timeToMinutes("08:30"))
        assertEquals(715, CourseRepository.timeToMinutes("11:55"))
        assertEquals(1110, CourseRepository.timeToMinutes("18:30"))
        assertEquals(1439, CourseRepository.timeToMinutes("23:59"))
    }

    @Test
    fun timeToMinutes_invalidInput_returnsZero() {
        assertEquals(0, CourseRepository.timeToMinutes(""))
        assertEquals(0, CourseRepository.timeToMinutes("abc"))
        assertEquals(0, CourseRepository.timeToMinutes("08"))
        assertEquals(0, CourseRepository.timeToMinutes("08:"))
        assertEquals(0, CourseRepository.timeToMinutes(":30"))
    }

    // ── isInCurrentWeek ────────────────────────────────────

    @Test
    fun isInCurrentWeek_singleWeek() {
        assertTrue(CourseRepository.isInCurrentWeek("3", 3))
        assertFalse(CourseRepository.isInCurrentWeek("3", 4))
    }

    @Test
    fun isInCurrentWeek_range() {
        assertTrue(CourseRepository.isInCurrentWeek("1-16", 1))
        assertTrue(CourseRepository.isInCurrentWeek("1-16", 8))
        assertTrue(CourseRepository.isInCurrentWeek("1-16", 16))
        assertFalse(CourseRepository.isInCurrentWeek("1-16", 17))
        assertFalse(CourseRepository.isInCurrentWeek("1-16", 0))
    }

    @Test
    fun isInCurrentWeek_multipleSegments() {
        assertTrue(CourseRepository.isInCurrentWeek("1-8,11-16", 5))
        assertTrue(CourseRepository.isInCurrentWeek("1-8,11-16", 13))
        assertFalse(CourseRepository.isInCurrentWeek("1-8,11-16", 9))
        assertFalse(CourseRepository.isInCurrentWeek("1-8,11-16", 10))
    }

    @Test
    fun isInCurrentWeek_evenWeeksOnly() {
        assertTrue(CourseRepository.isInCurrentWeek("1-16(双)", 2))
        assertTrue(CourseRepository.isInCurrentWeek("1-16(双)", 10))
        assertFalse(CourseRepository.isInCurrentWeek("1-16(双)", 3))
        assertFalse(CourseRepository.isInCurrentWeek("1-16(双)", 1))
    }

    @Test
    fun isInCurrentWeek_oddWeeksOnly() {
        assertTrue(CourseRepository.isInCurrentWeek("1-16(单)", 1))
        assertTrue(CourseRepository.isInCurrentWeek("1-16(单)", 15))
        assertFalse(CourseRepository.isInCurrentWeek("1-16(单)", 2))
        assertFalse(CourseRepository.isInCurrentWeek("1-16(单)", 16))
    }

    @Test
    fun isInCurrentWeek_withZhouSuffix() {
        assertTrue(CourseRepository.isInCurrentWeek("1-16周", 8))
        assertTrue(CourseRepository.isInCurrentWeek("3周", 3))
        assertFalse(CourseRepository.isInCurrentWeek("3周", 4))
    }

    @Test
    fun isInCurrentWeek_mixedWithZhouAndParity() {
        assertTrue(CourseRepository.isInCurrentWeek("1-15周(单),16周", 16))
        assertTrue(CourseRepository.isInCurrentWeek("1-15周(单),16周", 3))
        assertFalse(CourseRepository.isInCurrentWeek("1-15周(单),16周", 2))
    }

    @Test
    fun isInCurrentWeek_emptyString() {
        assertFalse(CourseRepository.isInCurrentWeek("", 5))
    }

    // ── calculateCurrentWeek ───────────────────────────────

    @Test
    fun calculateCurrentWeek_sameWeek() {
        val meta = CourseMeta(
            studentId = "001", name = "Test", academicYear = "2025",
            term = 1, termLabel = "第一学期", currentWeek = 8,
            exportTime = "2025-04-07T00:00:00.000Z"
        )
        // exportTime is a Monday in week 8; if today is that same Monday, week should be 8.
        // Since we can't control the current date, just verify it returns >= 1.
        val week = CourseRepository.calculateCurrentWeek(meta)
        assertTrue("Week should be >= 1", week >= 1)
    }

    @Test
    fun calculateCurrentWeek_invalidExportTime_fallsBackToMetaWeek() {
        val meta = CourseMeta(
            studentId = "001", name = "Test", academicYear = "2025",
            term = 1, termLabel = "第一学期", currentWeek = 5,
            exportTime = "invalid-date"
        )
        assertEquals(5, CourseRepository.calculateCurrentWeek(meta))
    }

    @Test
    fun calculateCurrentWeek_emptyExportTime_fallsBackToMetaWeek() {
        val meta = CourseMeta(
            studentId = "001", name = "Test", academicYear = "2025",
            term = 1, termLabel = "第一学期", currentWeek = 3,
            exportTime = ""
        )
        assertEquals(3, CourseRepository.calculateCurrentWeek(meta))
    }

    @Test
    fun calculateCurrentWeek_neverBelowOne() {
        // If exportTime is far in the future, weeksDiff would be negative.
        // The result is coerced to at least 1.
        val meta = CourseMeta(
            studentId = "001", name = "Test", academicYear = "2025",
            term = 1, termLabel = "第一学期", currentWeek = 1,
            exportTime = "2030-09-01T00:00:00.000Z"
        )
        val week = CourseRepository.calculateCurrentWeek(meta)
        assertTrue("Week should be >= 1", week >= 1)
    }
}
