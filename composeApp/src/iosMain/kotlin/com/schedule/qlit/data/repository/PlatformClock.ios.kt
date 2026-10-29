package com.schedule.qlit.data.repository

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate

@OptIn(ExperimentalForeignApi::class)
internal actual fun currentYearMonth(): Pair<Int, Int> {
    val components = NSCalendar.currentCalendar.components(
        NSCalendarUnitYear or NSCalendarUnitMonth,
        fromDate = NSDate(),
    )
    return components.year.toInt() to components.month.toInt()
}
