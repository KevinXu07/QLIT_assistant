package com.schedule.qlit.data.repository

import java.util.Calendar

internal actual fun currentYearMonth(): Pair<Int, Int> {
    val now = Calendar.getInstance()
    return now.get(Calendar.YEAR) to (now.get(Calendar.MONTH) + 1)
}
