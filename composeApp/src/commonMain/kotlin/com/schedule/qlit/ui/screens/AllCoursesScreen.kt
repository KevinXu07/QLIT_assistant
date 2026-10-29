package com.schedule.qlit.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schedule.qlit.data.model.Course
import com.schedule.qlit.data.model.ScheduleData
import com.schedule.qlit.data.repository.CourseRepository
import com.schedule.qlit.ui.components.decodeImageBitmap
import com.schedule.qlit.ui.components.rememberTextFilePicker
import com.schedule.qlit.ui.components.showPlatformToast
import com.schedule.qlit.data.repository.OnlineScheduleLoginResult
import com.schedule.qlit.data.repository.OnlineScheduleRepository
import com.schedule.qlit.ui.components.AdaptiveTextField
import com.schedule.qlit.ui.design.QButton
import com.schedule.qlit.ui.design.QButtonKind
import com.schedule.qlit.ui.design.QDialog
import com.schedule.qlit.ui.design.QDivider
import com.schedule.qlit.ui.design.QEmpty
import com.schedule.qlit.ui.design.QIconButton
import com.schedule.qlit.ui.design.QIcons
import com.schedule.qlit.ui.design.QList
import com.schedule.qlit.ui.design.QProgressBar
import com.schedule.qlit.ui.design.QRow
import com.schedule.qlit.ui.design.QSheet
import com.schedule.qlit.ui.design.QText
import com.schedule.qlit.ui.design.qBottomInset
import com.schedule.qlit.ui.design.qClickable
import com.schedule.qlit.ui.schedule.CourseDetailSheet
import com.schedule.qlit.ui.schedule.CourseEditorSheet
import com.schedule.qlit.ui.schedule.GridBlock
import com.schedule.qlit.ui.schedule.GridSlot
import com.schedule.qlit.ui.schedule.ScheduleLogic
import com.schedule.qlit.ui.schedule.WEEKDAY_SHORT
import com.schedule.qlit.ui.schedule.rememberCourseHues
import com.schedule.qlit.ui.theme.Q
import com.schedule.qlit.ui.theme.QType
import com.schedule.qlit.ui.theme.QlitHues
import kotlinx.coroutines.launch
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs

private val RowHeight = 104.dp
private val RowGap = 4.dp
private val SectionColumn = 22.dp
private val ColumnGap = 4.dp
private val EaseOutQuint = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

@Composable
fun AllCoursesScreen(
    schedule: ScheduleData?,
    clockMillis: Long,
    onImportSchedule: suspend (ScheduleData) -> Unit,
    onSaveSchedule: suspend (ScheduleData) -> Unit,
    onDownloadSchedule: suspend (String) -> ScheduleData?,
    onClearSchedule: suspend () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val now = remember(clockMillis) { Instant.fromEpochMilliseconds(clockMillis).toLocalDateTime(TimeZone.currentSystemDefault()) }
    val today = now.date
    val nowMinutes = now.hour * 60 + now.minute
    val currentWeek = remember(schedule, today) { schedule?.let { CourseRepository.calculateCurrentWeek(it.meta) } ?: 1 }
    val lastWeek = remember(schedule) { schedule?.let(ScheduleLogic::lastWeek) ?: 20 }
    var shownWeek by rememberSaveable(currentWeek) { mutableIntStateOf(currentWeek) }
    var weekDirection by remember { mutableIntStateOf(0) }
    val hues = rememberCourseHues(schedule)

    var showActions by remember { mutableStateOf(false) }
    var showUrlDialog by remember { mutableStateOf(false) }
    var showOnline by remember { mutableStateOf(false) }
    var showClear by remember { mutableStateOf(false) }
    var editorOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Course?>(null) }
    var detail by remember { mutableStateOf<GridBlock?>(null) }

    fun changeWeek(delta: Int) {
        val target = (shownWeek + delta).coerceIn(1, lastWeek.coerceAtLeast(currentWeek))
        if (target == shownWeek) return
        weekDirection = if (target > shownWeek) 1 else -1
        shownWeek = target
    }

    val filePicker = rememberTextFilePicker { json ->
        json ?: return@rememberTextFilePicker
        scope.launch {
            try {
                // 先解析校验结构（meta/courses 必须存在），避免 {} 之类的
                // 合法 JSON 被保存后在课表页触发空指针
                val parsed = CourseRepository.parseSchedule(json)
                if (parsed == null) {
                    showPlatformToast("文件格式不正确")
                    return@launch
                }
                onImportSchedule(parsed)
            } catch (_: Exception) {
                showPlatformToast("文件读取失败")
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Q.colors.bg)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        if (schedule == null) {
            EmptySchedule(
                onOnline = { showOnline = true },
                onLink = { showUrlDialog = true },
                onFile = { filePicker() },
            )
        } else {
            val isCurrent = shownWeek == currentWeek
            val dates = remember(shownWeek, currentWeek, today) { ScheduleLogic.datesOfWeek(shownWeek, today, currentWeek) }
            Column(Modifier.fillMaxSize()) {
                WeekHeader(
                    week = shownWeek,
                    dates = dates,
                    isCurrent = isCurrent,
                    canPrev = shownWeek > 1,
                    canNext = shownWeek < lastWeek.coerceAtLeast(currentWeek),
                    onPrev = { changeWeek(-1) },
                    onNext = { changeWeek(1) },
                    onBack = { changeWeek(currentWeek - shownWeek) },
                    onMore = { showActions = true },
                )
                val density = LocalDensity.current
                AnimatedContent(
                    targetState = shownWeek,
                    transitionSpec = {
                        val offset = with(density) { 36.dp.roundToPx() }
                        val dir = if (targetState > initialState) 1 else -1
                        (slideInHorizontally(tween(320, easing = EaseOutQuint)) { dir * offset } +
                            fadeIn(tween(320, easing = EaseOutQuint), initialAlpha = 0.2f)) togetherWith
                            fadeOut(tween(120))
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(shownWeek, lastWeek) {
                            var total = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { total = 0f },
                                onDragEnd = {
                                    if (abs(total) > 50.dp.toPx()) changeWeek(if (total < 0) 1 else -1)
                                },
                                onHorizontalDrag = { _, amount -> total += amount },
                            )
                        },
                    label = "weekGrid",
                ) { week ->
                    val weekDates = remember(week, currentWeek, today) { ScheduleLogic.datesOfWeek(week, today, currentWeek) }
                    WeekGrid(
                        schedule = schedule,
                        week = week,
                        dates = weekDates,
                        today = today,
                        nowMinutes = nowMinutes,
                        hues = hues,
                        onBlockClick = { detail = it },
                    )
                }
            }
        }
    }

    CourseDetailSheet(
        course = detail?.course,
        hue = detail?.course?.let { hues[it.name] } ?: QlitHues.NEUTRAL,
        activeThisWeek = detail?.activeThisWeek ?: true,
        onDismiss = { detail = null },
        onEdit = { course ->
            detail = null
            editing = course
            editorOpen = true
        },
    )

    CourseEditorSheet(
        show = editorOpen,
        schedule = schedule,
        editing = editing,
        initialDay = today.dayOfWeek.isoDayNumber,
        onDismiss = { editorOpen = false },
        onSave = onSaveSchedule,
    )

    ScheduleActionsSheet(
        show = showActions,
        schedule = schedule,
        onDismiss = { showActions = false },
        onFile = { showActions = false; filePicker() },
        onLink = { showActions = false; showUrlDialog = true },
        onOnline = { showActions = false; showOnline = true },
        onAdd = { showActions = false; editing = null; editorOpen = true },
        onClear = { showActions = false; showClear = true },
    )

    UrlImportDialog(
        show = showUrlDialog,
        onDismiss = { showUrlDialog = false },
        onDownload = onDownloadSchedule,
    )

    OnlineImportSheet(
        show = showOnline,
        onDismiss = { showOnline = false },
        onImport = onImportSchedule,
    )

    QDialog(
        show = showClear,
        title = "清除课表",
        message = "确认清除当前本地课表吗？清除后需要重新导入。",
        onDismissRequest = { showClear = false },
        confirmText = "清除",
        destructive = true,
        onConfirm = {
            scope.launch {
                onClearSchedule()
                showClear = false
            }
        },
    )
}

// ── Header ───────────────────────────────────────────────────────────

@Composable
private fun WeekHeader(
    week: Int,
    dates: List<LocalDate>,
    isCurrent: Boolean,
    canPrev: Boolean,
    canNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onMore: () -> Unit,
) {
    val first = dates.first()
    val last = dates.last()
    val range = buildString {
        append("${first.monthNumber}月${first.dayOfMonth}日 – ")
        if (last.monthNumber == first.monthNumber) append("${last.dayOfMonth}日") else append("${last.monthNumber}月${last.dayOfMonth}日")
        if (isCurrent) append("，本周")
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 10.dp, top = 10.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                QText("第", modifier = Modifier.padding(bottom = 2.dp), style = QType.pageTitle.copy(fontSize = 26.sp))
                QText("$week", modifier = Modifier.padding(horizontal = 4.dp), style = QType.num(32.sp, FontWeight.Bold))
                QText("周", modifier = Modifier.padding(bottom = 2.dp), style = QType.pageTitle.copy(fontSize = 26.sp))
            }
            QText(range, modifier = Modifier.padding(top = 3.dp), style = QType.sub, color = Q.colors.ink2)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            if (!isCurrent) {
                QText(
                    "回到本周",
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .qClickable(onClick = onBack)
                        .clip(CircleShape)
                        .background(Q.colors.sealSoft)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    style = QType.label.copy(fontWeight = FontWeight.SemiBold),
                    color = Q.colors.seal,
                )
            }
            QIconButton(QIcons.ChevronLeft, "上一周", onPrev, enabled = canPrev)
            QIconButton(QIcons.ChevronRight, "下一周", onNext, enabled = canNext)
            QIconButton(QIcons.More, "更多", onMore)
        }
    }
}

// ── Grid ─────────────────────────────────────────────────────────────

@Composable
private fun WeekGrid(
    schedule: ScheduleData,
    week: Int,
    dates: List<LocalDate>,
    today: LocalDate,
    nowMinutes: Int,
    hues: Map<String, Float>,
    onBlockClick: (GridBlock) -> Unit,
) {
    val slots = remember(schedule) { ScheduleLogic.gridSlots(schedule) }
    val blocks = remember(schedule, week, slots) { ScheduleLogic.gridBlocks(schedule, week, slots) }
    val days = remember(blocks) { ScheduleLogic.visibleDays(blocks) }
    val todayColumn = days.indexOfFirst { dates[it - 1] == today }
    val hasGhost = days.any { day -> blocks[day].orEmpty().any { !it.activeThisWeek } }
    val hiddenLabel = when {
        7 in days -> null
        6 in days -> "周日"
        else -> "周末"
    }

    Column(Modifier.fillMaxSize()) {
        DayHeaderRow(days = days, dates = dates, todayColumn = todayColumn)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            GridBody(
                slots = slots,
                days = days,
                blocks = blocks,
                todayColumn = todayColumn,
                nowMinutes = if (todayColumn >= 0) nowMinutes else null,
                hues = hues,
                onBlockClick = onBlockClick,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp, end = 22.dp, top = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (hasGhost) {
                    val dash = Q.colors.ink3.copy(alpha = 0.6f)
                    Box(
                        Modifier
                            .size(width = 18.dp, height = 12.dp)
                            .drawBehind {
                                drawRoundRect(
                                    dash,
                                    cornerRadius = CornerRadius(4.dp.toPx()),
                                    style = Stroke(1.2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.dp.toPx()))),
                                )
                            },
                    )
                    QText("虚线是本周不上的课", modifier = Modifier.padding(start = 8.dp), style = QType.caption, color = Q.colors.ink3)
                }
                Spacer(Modifier.weight(1f))
                if (hiddenLabel != null) {
                    QText("${hiddenLabel}没课，已收起", style = QType.caption, color = Q.colors.ink3)
                }
            }
            Spacer(Modifier.height(qBottomInset()))
        }
    }
}

@Composable
private fun DayHeaderRow(days: List<Int>, dates: List<LocalDate>, todayColumn: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 6.dp, end = 10.dp, top = 4.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(ColumnGap),
    ) {
        Spacer(Modifier.width(SectionColumn))
        days.forEachIndexed { index, day ->
            val isToday = index == todayColumn
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                QText(
                    WEEKDAY_SHORT[day - 1],
                    style = QType.caption.copy(fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Normal),
                    color = if (isToday) Q.colors.seal else Q.colors.ink3,
                )
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(if (isToday) Q.colors.seal else Color.Transparent),
                    contentAlignment = Alignment.Center,
                ) {
                    QText(
                        "${dates[day - 1].dayOfMonth}",
                        style = QType.num(17.sp),
                        color = if (isToday) Q.colors.onSeal else Q.colors.ink,
                    )
                }
            }
        }
    }
}

@Composable
private fun GridBody(
    slots: List<GridSlot>,
    days: List<Int>,
    blocks: Map<Int, List<GridBlock>>,
    todayColumn: Int,
    nowMinutes: Int?,
    hues: Map<String, Float>,
    onBlockClick: (GridBlock) -> Unit,
) {
    val line = Q.colors.line
    val seal = Q.colors.seal
    val todayTint = if (Q.colors.isDark) Color.White.copy(alpha = 0.025f) else Q.colors.seal.copy(alpha = 0.04f)
    val rows = slots.size.coerceAtLeast(1)
    val gridHeight = RowHeight * rows + RowGap * (rows - 1)
    val nowY: Dp? = nowMinutes?.let { now -> nowLineOffset(slots, now) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 6.dp, end = 10.dp),
    ) {
        val columnWidth = (maxWidth - SectionColumn - ColumnGap * days.size) / days.size
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(gridHeight)
                .drawBehind {
                    for (i in 0 until rows) {
                        val y = (RowHeight + RowGap).toPx() * i
                        drawLine(line, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                    }
                }
                .drawWithContent {
                    drawContent()
                    if (nowY != null && todayColumn >= 0) {
                        val y = nowY.toPx()
                        val startX = (SectionColumn + ColumnGap).toPx() - 4.dp.toPx()
                        drawLine(seal, Offset(startX, y), Offset(size.width, y), strokeWidth = 1.5.dp.toPx())
                        val dotX = (SectionColumn + ColumnGap + (columnWidth + ColumnGap) * todayColumn).toPx()
                        drawCircle(seal, radius = 4.dp.toPx(), center = Offset(dotX, y))
                    }
                },
            horizontalArrangement = Arrangement.spacedBy(ColumnGap),
        ) {
            Box(Modifier.width(SectionColumn).fillMaxHeight()) {
                slots.forEachIndexed { index, slot ->
                    Column(
                        modifier = Modifier
                            .offset(y = (RowHeight + RowGap) * index)
                            .width(SectionColumn)
                            .padding(top = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(if (slot.firstPeriod == slot.lastPeriod) 0.dp else 30.dp),
                    ) {
                        QText(slot.firstPeriod, style = QType.num(13.sp), color = Q.colors.ink3, textAlign = TextAlign.Center)
                        if (slot.firstPeriod != slot.lastPeriod) {
                            QText(slot.lastPeriod, style = QType.num(13.sp), color = Q.colors.ink3, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
            days.forEachIndexed { columnIndex, day ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .then(if (columnIndex == todayColumn) Modifier.background(todayTint) else Modifier),
                ) {
                    blocks[day].orEmpty().forEach { block ->
                        val height = RowHeight * block.rowSpan + RowGap * (block.rowSpan - 1) - 3.dp
                        CourseBlock(
                            block = block,
                            hue = hues[block.course.name] ?: QlitHues.NEUTRAL,
                            modifier = Modifier
                                .offset(y = (RowHeight + RowGap) * block.row + 3.dp)
                                .fillMaxWidth()
                                .height(height),
                            onClick = { onBlockClick(block) },
                        )
                    }
                }
            }
        }
    }
}

/** Y offset of the now-line inside the grid, or null outside teaching hours. */
private fun nowLineOffset(slots: List<GridSlot>, now: Int): Dp? {
    slots.forEachIndexed { index, slot ->
        val top = (RowHeight + RowGap) * index
        if (now >= slot.startMinutes && now < slot.endMinutes) {
            val fraction = (now - slot.startMinutes).toFloat() / (slot.endMinutes - slot.startMinutes).coerceAtLeast(1)
            return top + RowHeight * fraction
        }
        val next = slots.getOrNull(index + 1)
        if (next != null && now >= slot.endMinutes && now < next.startMinutes) {
            return top + RowHeight + RowGap / 2
        }
    }
    return null
}

@Composable
private fun CourseBlock(
    block: GridBlock,
    hue: Float,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val tint = Q.colors.hue(hue)
    val shape = RoundedCornerShape(12.dp)
    if (block.activeThisWeek) {
        Column(
            modifier = modifier
                .qClickable(pressScale = 0.96f, onClick = onClick)
                .clip(shape)
                .background(tint.container)
                .drawBehind {
                    val top = 7.dp.toPx()
                    drawRoundRect(
                        color = tint.accent,
                        topLeft = Offset(0f, top),
                        size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height - top * 2),
                        cornerRadius = CornerRadius(3.dp.toPx()),
                    )
                }
                .padding(start = 9.dp, end = 6.dp, top = 7.dp, bottom = 7.dp),
        ) {
            QText(
                block.course.name,
                style = QType.caption.copy(fontSize = 12.5.sp, lineHeight = 16.5.sp, fontWeight = FontWeight.SemiBold),
                color = tint.content,
                maxLines = if (block.rowSpan > 1) 7 else 4,
            )
            Spacer(Modifier.weight(1f))
            QText(
                block.course.classroom,
                style = QType.num(12.5.sp),
                color = tint.content.copy(alpha = 0.78f),
                maxLines = 1,
            )
        }
    } else {
        val dash = Q.colors.ink3.copy(alpha = 0.45f)
        Column(
            modifier = modifier
                .qClickable(pressScale = 0.96f, onClick = onClick)
                .drawBehind {
                    drawRoundRect(
                        dash,
                        cornerRadius = CornerRadius(12.dp.toPx()),
                        style = Stroke(1.2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))),
                    )
                }
                .padding(start = 9.dp, end = 6.dp, top = 7.dp, bottom = 7.dp),
        ) {
            QText(
                block.course.name,
                style = QType.caption.copy(fontSize = 12.5.sp, lineHeight = 16.5.sp),
                color = Q.colors.ink3,
                maxLines = 3,
            )
        }
    }
}

// ── Empty state ──────────────────────────────────────────────────────

@Composable
private fun EmptySchedule(onOnline: () -> Unit, onLink: () -> Unit, onFile: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = qBottomInset()),
    ) {
        QText("课表", modifier = Modifier.padding(start = 22.dp, top = 14.dp, bottom = 8.dp), style = QType.pageTitle)
        QEmpty(
            text = "还没有课表",
            subtitle = "从教务系统在线读取，或导入导出的 JSON 文件",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                QButton("在线读取教务课表", onOnline, Modifier.fillMaxWidth(), kind = QButtonKind.Primary, icon = QIcons.Cloud)
                QButton("从链接导入", onLink, Modifier.fillMaxWidth(), icon = QIcons.Link)
                QButton("选择本地文件", onFile, Modifier.fillMaxWidth(), icon = QIcons.File)
            }
        }
    }
}

// ── Sheets & dialogs ─────────────────────────────────────────────────

@Composable
private fun ScheduleActionsSheet(
    show: Boolean,
    schedule: ScheduleData?,
    onDismiss: () -> Unit,
    onFile: () -> Unit,
    onLink: () -> Unit,
    onOnline: () -> Unit,
    onAdd: () -> Unit,
    onClear: () -> Unit,
) {
    val importTime = remember(schedule, show) {
        CourseRepository.getImportTime()?.let {
            val dt = Instant.fromEpochMilliseconds(it)
                .toLocalDateTime(TimeZone.currentSystemDefault())
            "${dt.monthNumber}月${dt.dayOfMonth}日 ${dt.hour.toString().padStart(2, '0')}:${dt.minute.toString().padStart(2, '0')}"
        }
    }
    QSheet(show = show, onDismissRequest = onDismiss, title = "课表") {
        schedule?.meta?.let { meta ->
            QText(
                text = buildString {
                    append("${meta.academicYear} 学年 ${meta.termLabel}")
                    if (importTime != null) append(" · 导入于 $importTime")
                },
                modifier = Modifier.padding(bottom = 14.dp),
                style = QType.sub,
                color = Q.colors.ink2,
            )
        }
        QList(color = Q.colors.bg) {
            QRow("在线读取教务", icon = QIcons.Cloud, hue = QlitHues.SETTINGS, chevron = true, onClick = onOnline)
            QDivider()
            QRow("从链接导入", icon = QIcons.Link, hue = QlitHues.UPDATE, chevron = true, onClick = onLink)
            QDivider()
            QRow("从文件导入", icon = QIcons.File, hue = QlitHues.FEEDBACK, chevron = true, onClick = onFile)
        }
        Spacer(Modifier.height(12.dp))
        QList(color = Q.colors.bg) {
            QRow("添加课程", icon = QIcons.Add, hue = QlitHues.CAMPUS_RUN, chevron = true, onClick = onAdd)
            if (schedule != null) {
                QDivider()
                QRow("清除课表", icon = QIcons.Delete, hue = QlitHues.ABOUT, titleColor = Q.colors.seal, onClick = onClear)
            }
        }
    }
}

@Composable
private fun UrlImportDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onDownload: suspend (String) -> ScheduleData?,
) {
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf("") }
    var downloading by remember { mutableStateOf(false) }
    QDialog(
        show = show,
        title = "从链接导入",
        message = "粘贴课表 JSON 的下载链接",
        onDismissRequest = { if (!downloading) onDismiss() },
        confirmText = "导入",
        confirmEnabled = url.isNotBlank() && !downloading,
        loading = downloading,
        onConfirm = {
            scope.launch {
                downloading = true
                val imported = onDownload(url.trim())
                downloading = false
                if (imported != null) {
                    onDismiss()
                } else {
                    showPlatformToast("下载失败")
                }
            }
        },
    ) {
        AdaptiveTextField(
            value = url,
            onValueChange = { url = it },
            label = "https://",
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        )
    }
}

@Composable
private fun OnlineImportSheet(
    show: Boolean,
    onDismiss: () -> Unit,
    onImport: suspend (ScheduleData) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val baseUrl = remember { OnlineScheduleRepository.defaultBaseUrl() }
    var username by remember { mutableStateOf(OnlineScheduleRepository.loadSavedUsername()) }
    var password by remember { mutableStateOf("") }
    var captcha by remember { mutableStateOf("") }
    var captchaBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var needCaptcha by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    fun submit() {
        scope.launch {
            loading = true
            error = ""
            val loginResult: OnlineScheduleLoginResult = OnlineScheduleRepository.login(
                baseUrl = baseUrl,
                username = username,
                password = password,
                captcha = captcha.takeIf { it.isNotBlank() },
            )
            if (loginResult.ok) {
                runCatching {
                    val result = OnlineScheduleRepository.importSchedule(baseUrl = baseUrl, token = loginResult.token)
                    onImport(result.schedule)
                    OnlineScheduleRepository.saveBaseUrlAndUsername(baseUrl = baseUrl, username = username)
                    needCaptcha = false
                    captcha = ""
                    captchaBitmap = null
                    onDismiss()
                }.onFailure {
                    error = it.message ?: "课表读取失败"
                }
            } else {
                error = loginResult.errMsg.ifBlank { "登录失败" }
                needCaptcha = loginResult.needCaptcha
                if (loginResult.needCaptcha && loginResult.token.isNotBlank()) {
                    captchaBitmap = OnlineScheduleRepository.fetchCaptchaBytes(baseUrl = baseUrl, token = loginResult.token)?.let { decodeImageBitmap(it) }
                }
            }
            loading = false
        }
    }

    QSheet(
        show = show,
        onDismissRequest = { if (!loading) { error = ""; onDismiss() } },
        title = "在线读取课表",
    ) {
        val linkColor = Q.colors.ink
        val hint = remember(baseUrl, linkColor) {
            buildAnnotatedString {
                append("通过反代的教务系统 ")
                withLink(LinkAnnotation.Url(baseUrl)) {
                    withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) { append(baseUrl) }
                }
                append(" 登录，与学校教务系统保持同步")
            }
        }
        QText(hint, modifier = Modifier.padding(bottom = 16.dp), style = QType.sub, color = Q.colors.ink2)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AdaptiveTextField(value = username, onValueChange = { username = it }, label = "学号", modifier = Modifier.fillMaxWidth())
            AdaptiveTextField(
                value = password,
                onValueChange = { password = it },
                label = "密码",
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    QIconButton(
                        icon = if (passwordVisible) QIcons.EyeOff else QIcons.Eye,
                        contentDescription = if (passwordVisible) "隐藏密码" else "显示密码",
                        onClick = { passwordVisible = !passwordVisible },
                    )
                },
            )
            if (needCaptcha) {
                AdaptiveTextField(value = captcha, onValueChange = { captcha = it }, label = "验证码", modifier = Modifier.fillMaxWidth())
                val bitmap = captchaBitmap
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = "验证码",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .clip(RoundedCornerShape(12.dp)),
                    )
                } else {
                    QText("正在加载验证码…", style = QType.sub, color = Q.colors.ink3)
                }
            }
            if (loading) QProgressBar(progress = null, modifier = Modifier.fillMaxWidth())
            if (error.isNotBlank()) QText(error, style = QType.sub, color = Q.colors.seal)
        }
        com.schedule.qlit.ui.design.QActionRow(
            confirmText = "登录并读取",
            onConfirm = ::submit,
            onDismiss = onDismiss,
            confirmEnabled = !loading && username.isNotBlank() && password.isNotBlank(),
            loading = loading,
        )
    }
}
