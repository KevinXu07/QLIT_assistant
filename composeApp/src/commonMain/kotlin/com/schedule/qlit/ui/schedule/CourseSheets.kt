package com.schedule.qlit.ui.schedule

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schedule.qlit.data.model.Course
import com.schedule.qlit.data.model.ScheduleData
import com.schedule.qlit.data.repository.CourseRepository
import com.schedule.qlit.ui.components.AdaptiveFilterChip
import com.schedule.qlit.ui.components.AdaptiveTextField
import com.schedule.qlit.ui.components.MiuixTimePickerDialog
import com.schedule.qlit.ui.design.QActionRow
import com.schedule.qlit.ui.design.QButton
import com.schedule.qlit.ui.design.QButtonKind
import com.schedule.qlit.ui.design.QChip
import com.schedule.qlit.ui.design.QDialog
import com.schedule.qlit.ui.design.QSegmented
import com.schedule.qlit.ui.design.QSheet
import com.schedule.qlit.ui.design.QText
import com.schedule.qlit.ui.design.qClickable
import com.schedule.qlit.ui.theme.CoursePalette
import com.schedule.qlit.ui.theme.Q
import com.schedule.qlit.ui.theme.QType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Keeps the last non-null value so a sheet can animate out after its data is cleared. */
@Composable
fun <T : Any> rememberLastNonNull(value: T?): T? {
    val holder = remember { mutableStateOf(value) }
    if (value != null) holder.value = value
    return holder.value
}

/** Stable hue per course name, ordered by first appearance in the week. */
@Composable
fun rememberCourseHues(schedule: ScheduleData?): Map<String, Float> = remember(schedule) {
    schedule?.courses
        ?.sortedWith(compareBy<Course>({ it.dayOfWeek }, { ScheduleLogic.startMinutes(it) }))
        ?.map { it.name }
        ?.let(CoursePalette::assign)
        .orEmpty()
}

/** Faint ring seal of the school, the hero card's only ornament. */
@Composable
fun SealWatermark(modifier: Modifier = Modifier, color: Color = Color.White) {
    val textMeasurer = rememberTextMeasurer()
    Canvas(modifier) {
        val s = size.minDimension / 200f
        val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
        drawCircle(color, radius = 97f * s, center = center, style = Stroke(width = 4f * s))
        drawCircle(color, radius = 58f * s, center = center, style = Stroke(width = 2.5f * s))
        drawSealText(textMeasurer, color, s, left = center.x - 100f * s, top = center.y - 100f * s)
    }
}

/**
 * 校徽环形文字（"齐鲁理工学院" / "QILU INSTITUTE OF TECHNOLOGY"）。
 * 逐字沿弧排布：每个字按弧中点角度切向旋转后 drawText，两端共用实现。
 * 坐标系：以 (left, top) 为左上角的 200*s 方框。
 */
private fun DrawScope.drawSealText(textMeasurer: TextMeasurer, color: Color, s: Float, left: Float, top: Float) {
    val cx = left + 100f * s
    val cy = top + 100f * s
    drawRingText(textMeasurer, "齐鲁理工学院", cx, cy, radius = 74f * s,
        startDeg = 180f, sweepDeg = 180f, textSizePx = 22f * s, tracking = 6f * s, color = color)
    drawRingText(textMeasurer, "QILU INSTITUTE OF TECHNOLOGY", cx, cy, radius = 82f * s,
        startDeg = 180f, sweepDeg = -180f, textSizePx = 13f * s, tracking = 1.5f * s, color = color,
        baselineOffset = 10f * s)
}

private fun DrawScope.drawRingText(
    textMeasurer: TextMeasurer,
    text: String,
    cx: Float,
    cy: Float,
    radius: Float,
    startDeg: Float,
    sweepDeg: Float,
    textSizePx: Float,
    tracking: Float,
    color: Color,
    baselineOffset: Float = 0f,
) {
    val style = TextStyle(
        fontSize = (textSizePx / density).sp,
        fontWeight = FontWeight.Bold,
        color = color,
    )
    val layouts = text.map { textMeasurer.measure(it.toString(), style) }
    val widths = layouts.map { it.size.width.toFloat() }
    val total = widths.sum() + tracking * (text.length - 1).coerceAtLeast(0)
    val arcLen = kotlin.math.abs(sweepDeg) * kotlin.math.PI.toFloat() / 180f * radius
    var pos = (arcLen - total) / 2f
    val dir = if (sweepDeg >= 0f) 1f else -1f
    for (i in text.indices) {
        val w = widths[i]
        val mid = pos + w / 2f
        val angDeg = startDeg + dir * mid / radius * 180f / kotlin.math.PI.toFloat()
        val rad = angDeg * kotlin.math.PI / 180.0
        val r = radius + baselineOffset * dir
        val gx = cx + (r * kotlin.math.cos(rad)).toFloat()
        val gy = cy + (r * kotlin.math.sin(rad)).toFloat()
        withTransform({
            translate(gx, gy)
            rotate(degrees = angDeg + 90f * dir, pivot = Offset.Zero)
        }) {
            drawText(layouts[i], topLeft = Offset(-w / 2f, -layouts[i].size.height / 2f))
        }
        pos += w + tracking
    }
}

/** Initial-letter avatar ("徐"). */
@Composable
fun QAvatar(name: String, modifier: Modifier = Modifier, size: Dp = 38.dp, onClick: (() -> Unit)? = null) {
    Box(
        modifier = modifier
            .size(size)
            .then(if (onClick != null) Modifier.qClickable(pressScale = 0.92f, onClick = onClick) else Modifier)
            .clip(CircleShape)
            .background(Q.colors.surface2),
        contentAlignment = Alignment.Center,
    ) {
        QText(
            text = name.trim().take(1).ifBlank { "Q" },
            style = QType.rowTitle.copy(fontSize = (size.value * 0.4f).sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
        )
    }
}

fun slotPeriods(course: Course): String {
    val parts = course.slot.split("-")
    return if (parts.size == 2) "第 ${parts[0]}–${parts[1]} 节" else "第 ${course.slot} 节"
}

/** Course detail sheet: chip, title and a definition list (time / place / teacher / weeks). */
@Composable
fun CourseDetailSheet(
    course: Course?,
    hue: Float,
    activeThisWeek: Boolean,
    onDismiss: () -> Unit,
    onEdit: ((Course) -> Unit)?,
) {
    val shown = rememberLastNonNull(course) ?: return
    QSheet(show = course != null, onDismissRequest = onDismiss) {
        QChip(
            text = if (activeThisWeek) "周${WEEKDAY_SHORT[(shown.dayOfWeek - 1).coerceIn(0, 6)]}" else "本周不上",
            hue = hue,
        )
        QText(
            text = shown.name,
            modifier = Modifier.padding(top = 10.dp, bottom = 18.dp),
            style = QType.sheetTitle,
        )
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            DetailLine("时间") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    QText(
                        "周${WEEKDAY_SHORT[(shown.dayOfWeek - 1).coerceIn(0, 6)]} ${slotPeriods(shown)}　",
                        style = QType.body,
                    )
                    QText("${shown.startTime} – ${shown.endTime}", style = QType.num(17.sp))
                }
            }
            DetailLine("地点") {
                QText(shown.classroom.ifBlank { "未填写" }, style = if (shown.classroom.isBlank()) QType.body else QType.num(17.sp))
            }
            DetailLine("教师") { QText(shown.teacher.ifBlank { "未填写" }, style = QType.body) }
            DetailLine("周次") { QText(ScheduleLogic.describeWeeks(shown.weeks), style = QType.body) }
        }
        QActionRow(
            confirmText = "关闭",
            onConfirm = onDismiss,
            confirmKind = QButtonKind.Tonal,
            dismissText = if (onEdit != null) "编辑课程" else null,
            onDismiss = { onEdit?.invoke(shown) },
        )
    }
}

@Composable
private fun DetailLine(label: String, value: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        QText(label, modifier = Modifier.width(56.dp), style = QType.body, color = Q.colors.ink3)
        value()
    }
}

/** Add / edit a course. [editing] null means "add". */
@Composable
fun CourseEditorSheet(
    show: Boolean,
    schedule: ScheduleData?,
    editing: Course?,
    initialDay: Int,
    onDismiss: () -> Unit,
    onSave: suspend (ScheduleData) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var day by remember { mutableIntStateOf(1) }
    var slot by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("08:00") }
    var endTime by remember { mutableStateOf("09:50") }
    var classroom by remember { mutableStateOf("") }
    var teacher by remember { mutableStateOf("") }
    var weeksRange by remember { mutableStateOf("1-16") }
    var weekType by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var pickingStart by remember { mutableStateOf(false) }
    var pickingEnd by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(show, editing) {
        if (!show) return@LaunchedEffect
        error = null
        val c = editing
        name = c?.name.orEmpty()
        day = c?.dayOfWeek ?: initialDay.coerceIn(1, 7)
        slot = c?.slot.orEmpty()
        startTime = c?.startTime ?: "08:00"
        endTime = c?.endTime ?: "09:50"
        classroom = c?.classroom.orEmpty()
        teacher = c?.teacher.orEmpty()
        weeksRange = c?.weeks?.substringBefore("周")?.ifBlank { "1-16" } ?: "1-16"
        weekType = when {
            c?.weeks?.endsWith("(单)") == true -> 1
            c?.weeks?.endsWith("(双)") == true -> 2
            else -> 0
        }
    }

    fun persist(updated: ScheduleData) {
        saving = true
        scope.launch {
            try {
                onSave(updated)
                onDismiss()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                error = "保存失败，请检查课表格式或稍后重试"
            } finally {
                saving = false
            }
        }
    }

    fun save() {
        if (saving || schedule == null) return
        val suffix = when (weekType) { 1 -> "(单)"; 2 -> "(双)"; else -> "" }
        val weeks = weeksRange.replace("周", "").trim()
        val course = Course(
            name.trim(), day, slot.trim(),
            startTime.trim(), endTime.trim(),
            classroom.trim(), teacher.trim(),
            if (weeks.isBlank()) "" else "${weeks}周$suffix",
        )
        error = CourseRepository.courseValidationError(course)
        if (error != null) return
        if (CourseRepository.parseTimeMinutes(course.endTime)!! <= CourseRepository.parseTimeMinutes(course.startTime)!!) {
            error = "结束时间必须晚于开始时间"
            return
        }
        val updated = if (editing != null) {
            schedule.copy(courses = schedule.courses.map { if (it == editing) course else it })
        } else {
            schedule.copy(courses = schedule.courses + course)
        }
        persist(updated)
    }

    val density = LocalDensity.current
    val maxHeight = with(density) { LocalWindowInfo.current.containerSize.height.toDp() } * 0.62f
    val slotTable = remember(schedule) { schedule?.let(ScheduleLogic::gridSlots).orEmpty() }

    QSheet(
        show = show,
        onDismissRequest = { if (!saving) onDismiss() },
        title = if (editing == null) "添加课程" else "编辑课程",
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AdaptiveTextField(value = name, onValueChange = { name = it }, label = "课程名称", modifier = Modifier.fillMaxWidth())
            FieldLabel("星期")
            QSegmented(options = WEEKDAY_SHORT, selectedIndex = day - 1, onSelect = { day = it + 1 }, modifier = Modifier.fillMaxWidth())
            if (slotTable.isNotEmpty()) {
                FieldLabel("节次")
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    slotTable.forEach { row ->
                        AdaptiveFilterChip(
                            selected = slot == row.key,
                            onClick = {
                                slot = row.key
                                startTime = ScheduleLogic.formatClock(row.startMinutes)
                                endTime = ScheduleLogic.formatClock(row.endMinutes)
                            },
                            label = "${row.key} 节",
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AdaptiveTextField(value = slot, onValueChange = { slot = it }, label = "节次(1-2)", modifier = Modifier.weight(1f))
                AdaptiveTextField(value = classroom, onValueChange = { classroom = it }, label = "教室", modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TimeField("开始", startTime, Modifier.weight(1f)) { pickingStart = true }
                TimeField("结束", endTime, Modifier.weight(1f)) { pickingEnd = true }
            }
            AdaptiveTextField(value = teacher, onValueChange = { teacher = it }, label = "教师", modifier = Modifier.fillMaxWidth())
            FieldLabel("周次")
            AdaptiveTextField(
                value = weeksRange,
                onValueChange = { weeksRange = it },
                label = "如 1-16 或 3,6,9",
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            )
            QSegmented(options = listOf("每周", "单周", "双周"), selectedIndex = weekType, onSelect = { weekType = it }, modifier = Modifier.fillMaxWidth())
            error?.let { QText(it, style = QType.sub, color = Q.colors.seal) }
            if (editing != null) {
                QButton(
                    text = "删除这门课",
                    onClick = { confirmDelete = true },
                    modifier = Modifier.fillMaxWidth(),
                    kind = QButtonKind.Plain,
                )
            }
        }
        QActionRow(
            confirmText = if (saving) "保存中" else "保存",
            onConfirm = ::save,
            onDismiss = onDismiss,
            confirmEnabled = !saving,
            loading = saving,
        )
    }

    fun parse(time: String): Pair<Int, Int> {
        val minutes = CourseRepository.parseTimeMinutes(time) ?: 8 * 60
        return minutes / 60 to minutes % 60
    }
    MiuixTimePickerDialog(
        show = pickingStart,
        initialHour = parse(startTime).first,
        initialMinute = parse(startTime).second,
        onConfirm = { h, m -> startTime = "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}"; pickingStart = false },
        onDismiss = { pickingStart = false },
    )
    MiuixTimePickerDialog(
        show = pickingEnd,
        initialHour = parse(endTime).first,
        initialMinute = parse(endTime).second,
        onConfirm = { h, m -> endTime = "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}"; pickingEnd = false },
        onDismiss = { pickingEnd = false },
    )
    QDialog(
        show = confirmDelete,
        title = "删除课程",
        message = "确认从课表中删除“${editing?.name.orEmpty()}”吗？",
        onDismissRequest = { confirmDelete = false },
        confirmText = "删除",
        destructive = true,
        onConfirm = {
            confirmDelete = false
            val target = editing
            if (schedule != null && target != null) {
                persist(schedule.copy(courses = schedule.courses.filter { it != target }))
            }
        },
    )
}

@Composable
private fun FieldLabel(text: String) {
    QText(text, modifier = Modifier.padding(start = 4.dp, top = 4.dp), style = QType.label, color = Q.colors.ink2)
}

@Composable
private fun TimeField(label: String, value: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .qClickable(pressScale = 0.98f, onClick = onClick)
            .clip(RoundedCornerShape(16.dp))
            .background(Q.colors.surface2)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        QText(label, style = QType.caption, color = Q.colors.ink3)
        QText(value, style = QType.num(20.sp))
    }
}
