package com.schedule.qlit.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schedule.qlit.ui.design.QDialog
import com.schedule.qlit.ui.design.QText
import com.schedule.qlit.ui.theme.QType
import top.yukonga.miuix.kmp.basic.NumberPicker
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * 日期/时间选择器：用 Miuix 原生 [NumberPicker] 拼，包在窗口对话框里。
 * Miuix 没有现成的日期/时间选择器，Material3 DatePicker 在当前主题下不可用。
 */

/** 时间选择器（时:分，24 小时制）。 */
@Composable
fun MiuixTimePickerDialog(
    show: Boolean,
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    if (!show) return
    var hour by remember { mutableIntStateOf(initialHour.coerceIn(0, 23)) }
    var minute by remember { mutableIntStateOf(initialMinute.coerceIn(0, 59)) }

    QDialog(
        show = true,
        title = "选择时间",
        onDismissRequest = onDismiss,
        confirmText = "确定",
        onConfirm = { onConfirm(hour, minute) },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NumberPicker(
                value = hour,
                onValueChange = { hour = it },
                range = 0..23,
                label = { it.toString().padStart(2, '0') },
                modifier = Modifier.width(96.dp),
            )
            QText(
                text = ":",
                modifier = Modifier.padding(horizontal = 8.dp),
                style = QType.num(30.sp),
            )
            NumberPicker(
                value = minute,
                onValueChange = { minute = it },
                range = 0..59,
                label = { it.toString().padStart(2, '0') },
                modifier = Modifier.width(96.dp),
            )
        }
    }
}

/** 日期选择器（年/月/日）。月份天数按年月动态计算，处理闰年。 */
@Composable
fun MiuixDatePickerDialog(
    show: Boolean,
    initialYear: Int,
    initialMonth: Int,   // 1..12
    initialDay: Int,     // 1..31
    onConfirm: (year: Int, month: Int, day: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    if (!show) return
    @OptIn(ExperimentalTime::class)
    val currentYear = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year
    var year by remember { mutableIntStateOf(initialYear.coerceIn(currentYear - 5, currentYear + 5)) }
    var month by remember { mutableIntStateOf(initialMonth.coerceIn(1, 12)) }

    // 当前年月的天数（处理闰年）
    fun daysInMonth(y: Int, m: Int): Int = when (m) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (y % 4 == 0 && (y % 100 != 0 || y % 400 == 0)) 29 else 28
        else -> 30
    }
    val maxDay = daysInMonth(year, month)
    var day by remember { mutableIntStateOf(initialDay.coerceIn(1, maxDay)) }

    QDialog(
        show = true,
        title = "选择日期",
        onDismissRequest = onDismiss,
        confirmText = "确定",
        onConfirm = { onConfirm(year, month, day) },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NumberPicker(
                value = year,
                onValueChange = {
                    year = it
                    // 年/月变化后，天可能超出新月份的范围
                    val newMax = daysInMonth(year, month)
                    if (day > newMax) day = newMax
                },
                range = (currentYear - 5)..(currentYear + 5),
                label = { "${it}年" },
                modifier = Modifier.width(108.dp),
            )
            NumberPicker(
                value = month,
                onValueChange = {
                    month = it
                    val newMax = daysInMonth(year, month)
                    if (day > newMax) day = newMax
                },
                range = 1..12,
                label = { "${it}月" },
                modifier = Modifier.width(96.dp),
            )
            NumberPicker(
                value = day,
                onValueChange = { day = it },
                range = 1..daysInMonth(year, month),
                label = { "${it}日" },
                modifier = Modifier.width(96.dp),
            )
        }
    }
}
