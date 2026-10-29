package com.schedule.qlit.data.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class CourseMeta(
    val studentId: String,
    val name: String,
    val academicYear: String,
    val term: Int,
    val termLabel: String,
    val currentWeek: Int,
    val exportTime: String
)

@Immutable
@Serializable
data class TimeSlot(
    val label: String,
    val time: String
)

@Immutable
@Serializable
data class Course(
    val name: String,
    val dayOfWeek: Int,       // 1=周一 … 7=周日
    val slot: String,          // "1-2", "3-4" …
    val startTime: String,
    val endTime: String,
    val classroom: String,
    val teacher: String,
    val weeks: String
)

@Immutable
@Serializable
data class ScheduleData(
    val meta: CourseMeta,
    val timeSlots: Map<String, TimeSlot>,
    val courses: List<Course>
)
