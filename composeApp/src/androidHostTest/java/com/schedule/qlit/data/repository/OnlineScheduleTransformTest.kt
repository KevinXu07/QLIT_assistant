package com.schedule.qlit.data.repository

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * OnlineScheduleRepository.transformSchedule 测试。
 *
 * 钉住教务系统课表 JSON 到内部 ScheduleData 的转换：
 * slot → 时间映射、xqm 学期判定、kbList 字段取值。
 */
class OnlineScheduleTransformTest {

    private fun xsxxJson(xh: String = "20240001", xm: String = "张三") =
        buildJsonObject { put("XH", xh); put("XM", xm) }

    private fun kbItem(
        kcmc: String = "高等数学",
        xqj: Int = 1,
        jcs: String = "1-2",
        cdmc: String = "A101",
        xm: String = "李老师",
        zcd: String = "1-16周"
    ): JsonObject = buildJsonObject {
        put("kcmc", kcmc); put("xqj", xqj); put("jcs", jcs)
        put("cdmc", cdmc); put("xm", xm); put("zcd", zcd)
    }

    private fun scheduleMapOf(
        xsxx: JsonObject = xsxxJson(),
        kbList: List<JsonObject> = emptyList(),
    ): JsonObject = buildJsonObject {
        put("xsxx", xsxx)
        put("kbList", JsonArray(kbList))
    }

    @Test
    fun `maps kbList to courses with time slot`() {
        val scheduleMap = scheduleMapOf(
            kbList = listOf(kbItem(jcs = "1-2", cdmc = "A101"))
        )
        val schedule = OnlineScheduleRepository.transformSchedule(
            scheduleMap, currentWeek = 3, xnm = "2024", xqm = "3"
        )

        assertEquals(1, schedule.courses.size)
        val course = schedule.courses.first()
        assertEquals("高等数学", course.name)
        assertEquals(1, course.dayOfWeek)
        assertEquals("A101", course.classroom)
        assertEquals("李老师", course.teacher)
        assertEquals("1-16周", course.weeks)
        // slot 1-2 → 起止 08:30 / 10:05
        assertEquals("08:30", course.startTime)
        assertEquals("10:05", course.endTime)
    }

    @Test
    fun `building JB room uses slotMapB alternate time`() {
        // 教室含 JB 时用 3-4 的备用时段 10:35-12:10
        val scheduleMap = scheduleMapOf(
            kbList = listOf(kbItem(jcs = "3-4", cdmc = "JB201"))
        )
        val schedule = OnlineScheduleRepository.transformSchedule(
            scheduleMap, currentWeek = 1, xnm = "2024", xqm = "3"
        )
        assertEquals("10:35", schedule.courses.first().startTime)
        assertEquals("12:10", schedule.courses.first().endTime)
    }

    @Test
    fun `xqm 3 maps to first semester`() {
        val schedule = OnlineScheduleRepository.transformSchedule(
            scheduleMapOf(), currentWeek = 1, xnm = "2024", xqm = "3"
        )
        assertEquals(1, schedule.meta.term)
        assertEquals("第一学期", schedule.meta.termLabel)
    }

    @Test
    fun `xqm other than 3 maps to second semester`() {
        val schedule = OnlineScheduleRepository.transformSchedule(
            scheduleMapOf(), currentWeek = 1, xnm = "2024", xqm = "12"
        )
        assertEquals(2, schedule.meta.term)
        assertEquals("第二学期", schedule.meta.termLabel)
    }

    @Test
    fun `meta carries student info and week`() {
        val schedule = OnlineScheduleRepository.transformSchedule(
            scheduleMapOf(xsxx = xsxxJson(xh = "S001", xm = "测试")),
            currentWeek = 5, xnm = "2024", xqm = "3"
        )
        assertEquals("S001", schedule.meta.studentId)
        assertEquals("测试", schedule.meta.name)
        assertEquals(5, schedule.meta.currentWeek)
        assertEquals("2024", schedule.meta.academicYear)
    }

    @Test
    fun `empty kbList yields empty courses`() {
        val schedule = OnlineScheduleRepository.transformSchedule(
            scheduleMapOf(), currentWeek = 1, xnm = "2024", xqm = "3"
        )
        assertTrue(schedule.courses.isEmpty())
    }
}
