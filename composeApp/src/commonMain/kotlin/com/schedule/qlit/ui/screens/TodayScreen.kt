package com.schedule.qlit.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxWidth as fillWidthFraction
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.schedule.qlit.ScheduleViewModel
import com.schedule.qlit.data.model.Course
import com.schedule.qlit.data.model.ScheduleData
import com.schedule.qlit.data.repository.CourseRepository
import com.schedule.qlit.data.repository.TaobaoHomeRepository
import com.schedule.qlit.data.net.flushWebCookies
import com.schedule.qlit.data.net.warmupWebSession
import com.schedule.qlit.data.repository.CX_STATE_EXPIRED
import com.schedule.qlit.data.repository.CX_STATE_LOGGED_OUT
import com.schedule.qlit.data.repository.CX_STATE_OK
import com.schedule.qlit.data.repository.loadChaoxingFromWidgetCache
import com.schedule.qlit.data.repository.refreshWidgets
import com.schedule.qlit.data.repository.saveTaobaoToWidgetCache
import com.schedule.qlit.data.store.islandShizukuMissing
import com.schedule.qlit.data.store.kvStore
import com.schedule.qlit.data.store.notificationsPermitted
import com.schedule.qlit.data.store.platformIsXiaomiIslandCapable
import com.schedule.qlit.data.store.postBreakLiveNotification
import com.schedule.qlit.data.store.cancelBreakLiveNotification
import com.schedule.qlit.data.store.promotedNotificationsPermitted
import com.schedule.qlit.data.store.HomeDashboardRefreshSettings
import com.schedule.qlit.data.store.HomeDashboardRefreshSettingsStore
import com.schedule.qlit.ui.design.QActionRow
import com.schedule.qlit.ui.design.QButton
import com.schedule.qlit.ui.design.QButtonKind
import com.schedule.qlit.ui.design.QDialog
import com.schedule.qlit.ui.design.QDivider
import com.schedule.qlit.ui.design.QEmpty
import com.schedule.qlit.ui.design.QGroup
import com.schedule.qlit.ui.design.QIcons
import com.schedule.qlit.ui.design.QList
import com.schedule.qlit.ui.design.QNotice
import com.schedule.qlit.ui.design.QRadioRow
import com.schedule.qlit.ui.design.QRow
import com.schedule.qlit.ui.design.QSectionHeader
import com.schedule.qlit.ui.design.QStatusBarScrim
import com.schedule.qlit.ui.design.QSheet
import com.schedule.qlit.ui.design.QSwitchRow
import com.schedule.qlit.ui.design.QText
import com.schedule.qlit.ui.design.QTintIcon
import com.schedule.qlit.ui.design.qBottomInset
import com.schedule.qlit.ui.design.qClickable
import com.schedule.qlit.ui.design.qRowClickable
import com.schedule.qlit.ui.schedule.CourseDetailSheet
import com.schedule.qlit.ui.schedule.CourseEditorSheet
import com.schedule.qlit.ui.schedule.HeroState
import com.schedule.qlit.ui.schedule.QAvatar
import com.schedule.qlit.ui.schedule.ScheduleLogic
import com.schedule.qlit.ui.schedule.SealWatermark
import com.schedule.qlit.ui.schedule.TimelineState
import com.schedule.qlit.ui.schedule.WEEKDAY_SHORT
import com.schedule.qlit.ui.schedule.rememberCourseHues
import com.schedule.qlit.ui.theme.Q
import com.schedule.qlit.ui.theme.QType
import com.schedule.qlit.ui.theme.QlitHues
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toLocalDateTime

private data class HomeQuickTool(
    val route: String,
    val title: String,
    val shortTitle: String,
    val subtitle: String,
)

private const val HOME_TOOL_TAOBAO_PICKUP = "TAOBAO_PICKUP"
private const val HOME_TOOL_CHAOXING = "CHAOXING"
private const val HOME_QUICK_TOOLS_SCHEMA_VERSION = 3
private const val HOME_QUICK_TOOLS_SCHEMA_VERSION_KEY = "selected_routes_schema_version"

private val REFRESH_TEXTS = listOf("下拉刷新", "松开刷新", "正在刷新", "刷新完成")

private val homeQuickToolRoutesSaver = listSaver<Set<String>, String>(
    save = { it.toList() },
    restore = { it.toSet() },
)

private fun nowDisplayTime(): String =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).let {
        "${it.hour.toString().padStart(2, '0')}:${it.minute.toString().padStart(2, '0')}:${it.second.toString().padStart(2, '0')}"
    }

private suspend inline fun <T> suspendRunCatching(
    crossinline block: suspend () -> T
): Result<T> = try {
    Result.success(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (throwable: Throwable) {
    Result.failure(throwable)
}

/** Shortcut status line + red badge count. */
private data class ToolStatus(val text: String, val badge: Int? = null)

@Composable
fun TodayScreen(
    viewModel: ScheduleViewModel,
    onRequestNotificationPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTool: (String) -> Unit,
    onOpenCourses: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    customizeRequest: Int = 0,
) {
    val isXiaomi = platformIsXiaomiIslandCapable
    val quickToolsPrefs = remember { kvStore("home_quick_tools_prefs") }
    val taobaoHomePrefs = remember { kvStore("taobao_pickup_prefs") }
    val availableQuickTools = remember {
        listOf(
            HomeQuickTool(HOME_TOOL_TAOBAO_PICKUP, "淘宝取件", "取件", "驿站取件码与身份码"),
            HomeQuickTool(HOME_TOOL_CHAOXING, "超星待办", "待办", "待完成作业与考试"),
        )
    }
    val initialQuickToolSchemaVersion = remember {
        quickToolsPrefs.getInt(HOME_QUICK_TOOLS_SCHEMA_VERSION_KEY, 1)
    }
    val initialSelectedQuickToolRoutes = remember {
        val defaultRoutes = availableQuickTools.mapTo(linkedSetOf()) { it.route }
        quickToolsPrefs
            .getStringSet("selected_routes", defaultRoutes)
            .toSet()
    }
    var showQuickToolsDialog by rememberSaveable { mutableStateOf(false) }
    var selectedQuickToolRoutes by rememberSaveable(stateSaver = homeQuickToolRoutesSaver) {
        mutableStateOf(initialSelectedQuickToolRoutes)
    }
    val savedRefreshSettings = remember { HomeDashboardRefreshSettingsStore.load() }
    var homeAutoRefreshEnabled by rememberSaveable {
        mutableStateOf(savedRefreshSettings.autoRefreshEnabled)
    }
    var homeRefreshIntervalMinutes by rememberSaveable {
        mutableIntStateOf(savedRefreshSettings.refreshIntervalMinutes)
    }
    var dialogSelectedQuickToolRoutes by rememberSaveable(stateSaver = homeQuickToolRoutesSaver) {
        mutableStateOf(selectedQuickToolRoutes)
    }
    var dialogHomeAutoRefreshEnabled by rememberSaveable {
        mutableStateOf(homeAutoRefreshEnabled)
    }
    var dialogHomeRefreshIntervalMinutes by rememberSaveable {
        mutableIntStateOf(homeRefreshIntervalMinutes)
    }
    var taobaoSummary by remember { mutableStateOf("未登录") }
    var taobaoRefreshing by remember { mutableStateOf(false) }
    var taobaoInitialRefreshDone by remember { mutableStateOf(false) }
    var taobaoWarmupRequestToken by remember { mutableIntStateOf(0) }
    var taobaoWarmupInProgress by remember { mutableStateOf(false) }
    var chaoxingStatus by remember { mutableStateOf(ToolStatus("查看")) }
    var homeCardRefreshTrigger by remember { mutableIntStateOf(0) }
    var homeCardsRefreshing by remember { mutableStateOf(false) }
    var hasInitializedHomeCards by remember { mutableStateOf(false) }

    val uiState by viewModel.todayUiState.collectAsStateWithLifecycle()
    val schedule by viewModel.schedule.collectAsStateWithLifecycle()
    val clockMillis by viewModel.clock.collectAsStateWithLifecycle()
    var hasNotificationPermission by remember { mutableStateOf(false) }
    var canPostPromoted by remember { mutableStateOf(false) }

    fun applyPersistedTaobaoCard() {
        if (taobaoRefreshing && !taobaoInitialRefreshDone) {
            taobaoSummary = "同步中"
            return
        }
        val taobaoLoggedIn = taobaoHomePrefs.getBoolean("home_login_detected", false)
        val taobaoPendingCount = taobaoHomePrefs.getInt("home_pending_count", -1)
        taobaoSummary = when {
            !taobaoLoggedIn -> "未登录"
            taobaoPendingCount > 0 -> "$taobaoPendingCount 件待取"
            else -> "暂无待取"
        }
    }

    fun refreshChaoxingStatus() {
        val (work, exam, state) = loadChaoxingFromWidgetCache()
        chaoxingStatus = when (state) {
            CX_STATE_OK -> if (work + exam > 0) ToolStatus("${work + exam} 项", work + exam) else ToolStatus("已完成")
            CX_STATE_EXPIRED -> ToolStatus("需重新登录")
            CX_STATE_LOGGED_OUT -> ToolStatus("未登录")
            else -> ToolStatus("查看")
        }
    }

    DisposableEffect(selectedQuickToolRoutes, taobaoWarmupRequestToken) {
        if (HOME_TOOL_TAOBAO_PICKUP !in selectedQuickToolRoutes || taobaoWarmupRequestToken == 0) {
            onDispose { }
        } else {
            val cancel = warmupWebSession(TaobaoHomeRepository.PICKUP_URL) {
                taobaoWarmupInProgress = false
                homeCardRefreshTrigger += 1
            }

            onDispose {
                taobaoWarmupInProgress = false
                cancel()
            }
        }
    }

    suspend fun refreshSelectedHomeCards() {
        if (homeCardsRefreshing) return
        homeCardsRefreshing = true
        try {
            refreshChaoxingStatus()
            if (HOME_TOOL_TAOBAO_PICKUP in selectedQuickToolRoutes) {
                taobaoRefreshing = true
                applyPersistedTaobaoCard()
                suspendRunCatching {
                    flushWebCookies()
                    TaobaoHomeRepository.fetchRefreshResult()
                }.onSuccess { result ->
                    taobaoHomePrefs.putBoolean("home_login_detected", result.loggedIn)
                    taobaoHomePrefs.putInt("home_pending_count", result.pendingCount ?: -1)
                    taobaoHomePrefs.putString("home_station_hint", result.stationHint)
                    taobaoHomePrefs.putString("home_updated_at", nowDisplayTime())
                    saveTaobaoToWidgetCache(
                        loggedIn = result.loggedIn,
                        pendingCount = result.pendingCount ?: -1,
                    )
                    refreshWidgets()
                    taobaoInitialRefreshDone = true
                    taobaoRefreshing = false
                    applyPersistedTaobaoCard()
                }.onFailure {
                    taobaoInitialRefreshDone = true
                    val missingCookieOrToken =
                        it.message?.contains("Cookie", ignoreCase = true) == true ||
                            it.message?.contains("token", ignoreCase = true) == true
                    if (missingCookieOrToken) {
                        val mayHaveExistingSession =
                            taobaoHomePrefs.getBoolean("home_login_detected", false) ||
                                taobaoHomePrefs.getBoolean("home_identity_ready", false)
                        taobaoSummary = if (mayHaveExistingSession) "同步中" else "未登录"
                        if (mayHaveExistingSession && !taobaoWarmupInProgress) {
                            taobaoWarmupInProgress = true
                            taobaoWarmupRequestToken += 1
                        }
                    } else {
                        taobaoRefreshing = false
                        applyPersistedTaobaoCard()
                    }
                }.also {
                    taobaoRefreshing = false
                }
            }
        } finally {
            homeCardsRefreshing = false
        }
    }

    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        hasNotificationPermission = notificationsPermitted()
        canPostPromoted = promotedNotificationsPermitted()
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch {
                    hasNotificationPermission = notificationsPermitted()
                    canPostPromoted = promotedNotificationsPermitted()
                }
                viewModel.refreshClock()
                if (hasInitializedHomeCards) {
                    homeCardRefreshTrigger += 1
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(uiState.upcomingCourse, uiState.minutesUntil) {
        val next = uiState.upcomingCourse
        if (next != null && hasNotificationPermission && canPostPromoted) {
            postBreakLiveNotification(
                courseName = next.name,
                classroom = next.classroom,
                minutesUntil = uiState.minutesUntil,
                progressPercent = uiState.breakProgress,
            )
        } else if (uiState.upcomingCourse == null) {
            cancelBreakLiveNotification()
        }
    }

    LaunchedEffect(selectedQuickToolRoutes, homeCardRefreshTrigger) {
        if (!hasInitializedHomeCards) {
            delay(700)
        }
        refreshSelectedHomeCards()
        hasInitializedHomeCards = true
    }

    LaunchedEffect(Unit) {
        if (initialQuickToolSchemaVersion < HOME_QUICK_TOOLS_SCHEMA_VERSION) {
            // v3 起剔除了浴室/电费/出行，重存一次让旧偏好里的失效路由自然消失
            quickToolsPrefs.putStringSet("selected_routes", initialSelectedQuickToolRoutes)
            quickToolsPrefs.putInt(HOME_QUICK_TOOLS_SCHEMA_VERSION_KEY, HOME_QUICK_TOOLS_SCHEMA_VERSION)
        }
        taobaoRefreshing = HOME_TOOL_TAOBAO_PICKUP in selectedQuickToolRoutes
        applyPersistedTaobaoCard()
        refreshChaoxingStatus()
    }

    LaunchedEffect(homeAutoRefreshEnabled, homeRefreshIntervalMinutes, selectedQuickToolRoutes) {
        if (!homeAutoRefreshEnabled || selectedQuickToolRoutes.isEmpty()) return@LaunchedEffect
        while (true) {
            delay(homeRefreshIntervalMinutes * 60 * 1000L)
            refreshSelectedHomeCards()
        }
    }

    fun openCustomization() {
        dialogSelectedQuickToolRoutes = selectedQuickToolRoutes
        dialogHomeAutoRefreshEnabled = homeAutoRefreshEnabled
        dialogHomeRefreshIntervalMinutes = homeRefreshIntervalMinutes
        showQuickToolsDialog = true
    }

    LaunchedEffect(customizeRequest) {
        if (customizeRequest > 0) openCustomization()
    }

    // ── Derived schedule state ──────────────────────────────────────
    val now = remember(clockMillis) { Instant.fromEpochMilliseconds(clockMillis).toLocalDateTime(TimeZone.currentSystemDefault()) }
    val today = now.date
    val nowMinutes = now.hour * 60 + now.minute
    val data = schedule
    val currentWeek = remember(data, today) { data?.let { CourseRepository.calculateCurrentWeek(it.meta) } ?: 1 }
    val hero = remember(data, today, nowMinutes, currentWeek) {
        data?.let { ScheduleLogic.heroState(it, today, nowMinutes, currentWeek) }
    }
    val hues = rememberCourseHues(data)
    var selectedDay by rememberSaveable { mutableIntStateOf(today.dayOfWeek.isoDayNumber) }
    val weekDates = remember(today) { ScheduleLogic.datesOfWeek(currentWeek, today, currentWeek) }
    var detailCourse by remember { mutableStateOf<Course?>(null) }
    var editorOpen by remember { mutableStateOf(false) }
    var editingCourse by remember { mutableStateOf<Course?>(null) }
    var showProfile by remember { mutableStateOf(false) }

    val shouldShowPermissionCard = if (isXiaomi) {
        islandShizukuMissing()
    } else {
        !(hasNotificationPermission && canPostPromoted)
    }
    val selectedTools = availableQuickTools.filter { it.route in selectedQuickToolRoutes }
    fun statusFor(route: String): ToolStatus = when (route) {
        HOME_TOOL_TAOBAO_PICKUP -> {
            val pending = taobaoHomePrefs.getInt("home_pending_count", -1)
            when {
                taobaoSummary == "同步中" || taobaoSummary == "未登录" -> ToolStatus(taobaoSummary)
                pending > 0 -> ToolStatus("$pending 件", pending)
                else -> ToolStatus("无待取")
            }
        }
        HOME_TOOL_CHAOXING -> chaoxingStatus
        else -> ToolStatus("查看")
    }

    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val pullState = rememberPullToRefreshState()

    Box(Modifier.fillMaxSize().background(Q.colors.bg)) {
        PullToRefresh(
            isRefreshing = homeCardsRefreshing,
            onRefresh = {
                viewModel.refreshClock()
                homeCardRefreshTrigger++
            },
            pullToRefreshState = pullState,
            contentPadding = PaddingValues(top = statusTop),
            color = Q.colors.ink3,
            refreshTexts = REFRESH_TEXTS,
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .scrollEndHaptic()
                    .overScrollVertical(),
                contentPadding = PaddingValues(top = statusTop + 8.dp, bottom = qBottomInset()),
                overscrollEffect = null,
            ) {
                item(key = "top") {
                    HomeTopBar(
                        today = today,
                        week = if (data != null) currentWeek else null,
                        name = data?.meta?.name.orEmpty(),
                        onAvatar = { showProfile = true },
                    )
                }
                item(key = "hero") {
                    HeroCard(
                        schedule = data,
                        hero = hero,
                        today = today,
                        nowMinutes = nowMinutes,
                        currentWeek = currentWeek,
                        onClick = { course -> detailCourse = course },
                        onImport = onOpenCourses,
                    )
                }
                if (shouldShowPermissionCard) {
                    item(key = "permission") {
                        PermissionNotice(
                            isXiaomi = isXiaomi,
                            hasNotificationPermission = hasNotificationPermission,
                            onRequestPermission = onRequestNotificationPermission,
                            onOpenSettings = onOpenSettings,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
                        )
                    }
                }
                if (data != null) {
                    item(key = "strip") {
                        WeekStrip(
                            dates = weekDates,
                            today = today,
                            selectedDay = selectedDay,
                            countFor = { day -> ScheduleLogic.coursesOn(data, currentWeek, day).size },
                            onSelect = { selectedDay = it },
                        )
                    }
                    val isToday = selectedDay == today.dayOfWeek.isoDayNumber
                    val dayList = ScheduleLogic.coursesOn(data, currentWeek, selectedDay)
                    val selDate = weekDates[selectedDay - 1]
                    item(key = "dayhead") {
                        val remaining = ScheduleLogic.remainingToday(dayList, nowMinutes)
                        QSectionHeader(
                            title = if (isToday) "今天" else "周${WEEKDAY_SHORT[selectedDay - 1]} ${selDate.monthNumber}月${selDate.dayOfMonth}日",
                            trailing = when {
                                !isToday -> "${dayList.size} 节"
                                remaining > 0 -> "还有 $remaining 节"
                                dayList.isEmpty() -> null
                                else -> "全部结束"
                            },
                        )
                    }
                    if (dayList.isEmpty()) {
                        item(key = "empty") {
                            QEmpty("这天没有课", modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp))
                        }
                    } else {
                        itemsIndexed(dayList, key = { index, c -> "tl$index${c.name}" }) { index, course ->
                            val gap = ScheduleLogic.gapBefore(dayList, index)
                            if (gap >= 60) TimelineGap(gap)
                            TimelineRow(
                                course = course,
                                hue = hues[course.name] ?: QlitHues.NEUTRAL,
                                state = if (isToday && hero != null) ScheduleLogic.timelineState(course, nowMinutes, hero) else TimelineState.Upcoming,
                                isFirst = index == 0,
                                isLast = index == dayList.lastIndex,
                                onClick = { detailCourse = course },
                            )
                        }
                    }
                }
                item(key = "shortcuts") {
                    ShortcutsCard(
                        tools = selectedTools,
                        statusFor = ::statusFor,
                        onOpenTool = onOpenTool,
                        onCustomize = ::openCustomization,
                    )
                }
            }
        }
        QStatusBarScrim()
    }

    CourseDetailSheet(
        course = detailCourse,
        hue = detailCourse?.let { hues[it.name] } ?: QlitHues.NEUTRAL,
        activeThisWeek = true,
        onDismiss = { detailCourse = null },
        onEdit = { course ->
            detailCourse = null
            editingCourse = course
            editorOpen = true
        },
    )
    CourseEditorSheet(
        show = editorOpen,
        schedule = data,
        editing = editingCourse,
        initialDay = selectedDay,
        onDismiss = { editorOpen = false },
        onSave = { viewModel.saveSchedule(it) },
    )

    ProfileSheet(
        show = showProfile,
        name = data?.meta?.name,
        onDismiss = { showProfile = false },
        onOpenSettings = { showProfile = false; onOpenAppSettings() },
        onOpenAbout = { showProfile = false; onOpenAbout() },
    )

    fun saveHomeCustomization() {
        val normalizedInterval = HomeDashboardRefreshSettingsStore
            .normalizeInterval(dialogHomeRefreshIntervalMinutes)
        selectedQuickToolRoutes = dialogSelectedQuickToolRoutes
        homeAutoRefreshEnabled = dialogHomeAutoRefreshEnabled
        homeRefreshIntervalMinutes = normalizedInterval
        quickToolsPrefs.putStringSet("selected_routes", dialogSelectedQuickToolRoutes)
        quickToolsPrefs.putInt(HOME_QUICK_TOOLS_SCHEMA_VERSION_KEY, HOME_QUICK_TOOLS_SCHEMA_VERSION)
        HomeDashboardRefreshSettingsStore.persist(
            HomeDashboardRefreshSettings(
                autoRefreshEnabled = dialogHomeAutoRefreshEnabled,
                refreshIntervalMinutes = normalizedInterval,
            )
        )
        homeCardRefreshTrigger += 1
        showQuickToolsDialog = false
    }

    val customizationMaxHeight = (LocalWindowInfo.current.containerDpSize.height * 0.62f)
        .coerceAtLeast(240.dp)
        .coerceAtMost(600.dp)
    QSheet(
        show = showQuickToolsDialog,
        onDismissRequest = { showQuickToolsDialog = false },
        title = "主页工具",
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = customizationMaxHeight)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            QText("显示在今天页底部的快捷工具", style = QType.sub, color = Q.colors.ink2)
            QList(color = Q.colors.bg) {
                availableQuickTools.forEachIndexed { index, tool ->
                    if (index > 0) QDivider()
                    QSwitchRow(
                        title = tool.title,
                        subtitle = tool.subtitle,
                        icon = QIcons.forTool(tool.route),
                        hue = QlitHues.forTool(tool.route),
                        checked = tool.route in dialogSelectedQuickToolRoutes,
                        onCheckedChange = { checked ->
                            dialogSelectedQuickToolRoutes = if (checked) {
                                dialogSelectedQuickToolRoutes + tool.route
                            } else {
                                dialogSelectedQuickToolRoutes - tool.route
                            }
                        },
                    )
                }
            }
            QText("自动刷新", modifier = Modifier.padding(start = 4.dp, top = 6.dp), style = QType.label, color = Q.colors.ink2)
            QList(color = Q.colors.bg) {
                QSwitchRow(
                    title = "定时刷新工具状态",
                    subtitle = if (dialogHomeAutoRefreshEnabled) {
                        "每 $dialogHomeRefreshIntervalMinutes 分钟刷新一次"
                    } else {
                        "关闭后仅在进入或返回今天页时刷新"
                    },
                    checked = dialogHomeAutoRefreshEnabled,
                    onCheckedChange = { dialogHomeAutoRefreshEnabled = it },
                )
                if (dialogHomeAutoRefreshEnabled) {
                    HomeDashboardRefreshSettingsStore.INTERVAL_OPTIONS.forEach { minutes ->
                        QDivider(startInset = 14.dp)
                        QRadioRow(
                            title = "每 $minutes 分钟",
                            selected = dialogHomeRefreshIntervalMinutes == minutes,
                            onClick = { dialogHomeRefreshIntervalMinutes = minutes },
                        )
                    }
                }
            }
        }
        QActionRow(
            confirmText = "保存",
            onConfirm = ::saveHomeCustomization,
            onDismiss = { showQuickToolsDialog = false },
        )
    }
}

// ── Top bar ──────────────────────────────────────────────────────────

@Composable
private fun HomeTopBar(today: LocalDate, week: Int?, name: String, onAvatar: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            QText(
                "${today.monthNumber}月${today.dayOfMonth}日 周${WEEKDAY_SHORT[today.dayOfWeek.isoDayNumber - 1]}",
                style = QType.title,
            )
            QText(
                if (week != null) "第 $week 周" else "还没有导入课表",
                modifier = Modifier.padding(top = 2.dp),
                style = QType.sub,
                color = Q.colors.ink2,
            )
        }
        QAvatar(name = name, onClick = onAvatar)
    }
}

// ── Hero ─────────────────────────────────────────────────────────────

private val HeroShape = RoundedCornerShape(28.dp)

@Composable
private fun HeroCard(
    schedule: ScheduleData?,
    hero: HeroState?,
    today: LocalDate,
    nowMinutes: Int,
    currentWeek: Int,
    onClick: (Course) -> Unit,
    onImport: () -> Unit,
) {
    val seal = Q.colors.seal
    val white = Color.White
    val course = when (hero) {
        is HeroState.Next -> hero.course
        is HeroState.Now -> hero.course
        is HeroState.Later -> hero.course
        else -> null
    }
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .then(
                if (!Q.colors.isDark) {
                    Modifier.shadow(18.dp, HeroShape, ambientColor = seal.copy(alpha = 0.3f), spotColor = seal.copy(alpha = 0.55f))
                } else {
                    Modifier
                },
            )
            .then(
                when {
                    course != null -> Modifier.qClickable(pressScale = 0.985f) { onClick(course) }
                    schedule == null -> Modifier.qClickable(pressScale = 0.985f, onClick = onImport)
                    else -> Modifier
                },
            )
            .clip(HeroShape)
            .background(seal),
    ) {
        // The watermark must not drive the card's height: it lives in a box that
        // only matches the content size and is allowed to overflow (clipped).
        Box(Modifier.matchParentSize()) {
            SealWatermark(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .wrapContentSize(Alignment.TopStart, unbounded = true)
                    .offset(x = 78.dp, y = (-34).dp)
                    .requiredSize(230.dp)
                    .graphicsLayer { alpha = 0.13f },
                color = white,
            )
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp)) {
            when {
                schedule == null -> HeroMessage(
                    top = "还没有课表",
                    title = "导入课表后，下一节课会出现在这里",
                    tail = "去课表页导入 →",
                )

                hero == null || hero is HeroState.Free -> HeroMessage(
                    top = "接下来两周",
                    title = "没有安排课程",
                    tail = "好好休息",
                )

                course != null -> {
                    val top = when (hero) {
                        is HeroState.Next -> "下一节"
                        is HeroState.Now -> "正在上课"
                        is HeroState.Later -> when {
                            hero.dayOffset == 1 && hero.todayHadClasses -> "今天的课上完了，明天第一节"
                            hero.dayOffset == 1 -> "今天没课，明天第一节"
                            else -> "下一节在 ${hero.date.monthNumber}月${hero.date.dayOfMonth}日"
                        }
                        else -> ""
                    }
                    val topRight = if (hero is HeroState.Later) {
                        "周${WEEKDAY_SHORT[hero.date.dayOfWeek.isoDayNumber - 1]}"
                    } else {
                        "${course.startTime} – ${course.endTime}"
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        QText(top, modifier = Modifier.weight(1f), style = QType.sub, color = white.copy(alpha = 0.82f), maxLines = 1)
                        QText(topRight, style = QType.num(15.sp), color = white)
                    }
                    QText(
                        course.name,
                        modifier = Modifier.fillWidthFraction(0.92f).padding(top = 10.dp, bottom = 6.dp),
                        style = QType.display,
                        color = white,
                        maxLines = 2,
                    )
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (course.classroom.isNotBlank()) QText(course.classroom, style = QType.num(22.sp), color = white)
                        if (course.teacher.isNotBlank()) {
                            QText(course.teacher, modifier = Modifier.padding(bottom = 2.dp), style = QType.body.copy(fontSize = 14.sp), color = white.copy(alpha = 0.82f))
                        }
                    }
                    Row(Modifier.padding(top = 22.dp), verticalAlignment = Alignment.Bottom) {
                        when (hero) {
                            is HeroState.Next -> {
                                Duration(hero.minutesUntil)
                                HeroTail("后上课")
                            }
                            is HeroState.Now -> {
                                HeroTail("还剩", end = 6.dp)
                                Duration(hero.minutesLeft)
                            }
                            is HeroState.Later -> {
                                QText(course.startTime, style = QType.num(46.sp), color = white)
                                HeroTail("开始")
                            }
                            else -> Unit
                        }
                    }
                    if (hero is HeroState.Next || hero is HeroState.Now) {
                        DayTrack(
                            courses = ScheduleLogic.coursesOn(schedule, currentWeek, today.dayOfWeek.isoDayNumber),
                            nowMinutes = nowMinutes,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroMessage(top: String, title: String, tail: String) {
    QText(top, style = QType.sub, color = Color.White.copy(alpha = 0.82f))
    QText(title, modifier = Modifier.fillWidthFraction(0.86f).padding(top = 10.dp), style = QType.display.copy(fontSize = 24.sp), color = Color.White)
    QText(tail, modifier = Modifier.padding(top = 22.dp), style = QType.body.copy(fontSize = 14.sp), color = Color.White.copy(alpha = 0.75f))
}

@Composable
private fun RowScope.HeroTail(text: String, end: androidx.compose.ui.unit.Dp = 0.dp) {
    QText(
        text,
        modifier = Modifier.padding(start = if (end == 0.dp) 8.dp else 0.dp, end = end, bottom = 3.dp),
        style = QType.body.copy(fontSize = 14.sp),
        color = Color.White.copy(alpha = 0.75f),
    )
}

/** "1 小时 20 分" with big numbers and small units. */
@Composable
private fun Duration(minutes: Int) {
    val h = minutes / 60
    val m = minutes % 60
    val unit = QType.body.copy(fontSize = 15.sp)
    Row(verticalAlignment = Alignment.Bottom) {
        if (h > 0) {
            QText("$h", style = QType.num(46.sp), color = Color.White)
            QText("小时", modifier = Modifier.padding(start = 3.dp, end = 6.dp, bottom = 4.dp), style = unit, color = Color.White.copy(alpha = 0.9f))
            if (m > 0) {
                QText("$m", style = QType.num(46.sp), color = Color.White)
                QText("分", modifier = Modifier.padding(start = 3.dp, bottom = 4.dp), style = unit, color = Color.White.copy(alpha = 0.9f))
            }
        } else {
            QText("$m", style = QType.num(46.sp), color = Color.White)
            QText("分钟", modifier = Modifier.padding(start = 3.dp, bottom = 4.dp), style = unit, color = Color.White.copy(alpha = 0.9f))
        }
    }
}

/** Thin track marking each class of the day and "now". */
@Composable
private fun DayTrack(courses: List<Course>, nowMinutes: Int) {
    if (courses.isEmpty()) return
    val dayStart = ScheduleLogic.startMinutes(courses.first()) - 30
    val dayEnd = ScheduleLogic.endMinutes(courses.last()) + 30
    val span = (dayEnd - dayStart).coerceAtLeast(1).toFloat()
    fun pct(t: Int) = ((t - dayStart) / span).coerceIn(0f, 1f)
    val ringColor = Q.colors.seal
    Column(Modifier.padding(top = 16.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(12.dp)) {
            val w = maxWidth
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.16f)),
            )
            courses.forEach { c ->
                val a = pct(ScheduleLogic.startMinutes(c))
                val b = pct(ScheduleLogic.endMinutes(c))
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = w * a)
                        .width(w * (b - a))
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.34f)),
                )
            }
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = w * pct(nowMinutes) - 6.dp)
                    .size(12.dp)
                    .drawBehind {
                        drawCircle(Color.White.copy(alpha = 0.5f), radius = size.minDimension / 2 + 4.dp.toPx())
                        drawCircle(ringColor, radius = size.minDimension / 2 + 3.dp.toPx())
                        drawCircle(Color.White, radius = size.minDimension / 2)
                    },
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = 7.dp)) {
            QText(courses.first().startTime, modifier = Modifier.weight(1f), style = QType.num(12.sp, FontWeight.Medium), color = Color.White.copy(alpha = 0.7f))
            QText(courses.last().endTime, style = QType.num(12.sp, FontWeight.Medium), color = Color.White.copy(alpha = 0.7f))
        }
    }
}

// ── Permission ───────────────────────────────────────────────────────

@Composable
private fun PermissionNotice(
    isXiaomi: Boolean,
    hasNotificationPermission: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isXiaomi) {
        QNotice(
            title = "Shizuku 未连接",
            text = "超级岛课前提醒等功能需要 Shizuku 服务",
            attention = false,
            modifier = modifier,
        )
    } else {
        // Kept in ink: the hero card above is the only red block on the page.
        QNotice(
            title = if (hasNotificationPermission) "开启实时更新" else "开启课前提醒",
            text = "授权后，上课前会在通知栏显示倒计时",
            modifier = modifier,
            attention = false,
            icon = QIcons.Bell,
            action = {
                QButton(
                    text = if (hasNotificationPermission) "去设置" else "授予",
                    onClick = if (hasNotificationPermission) onOpenSettings else onRequestPermission,
                    kind = QButtonKind.Primary,
                    height = 36.dp,
                )
            },
        )
    }
}

// ── Week strip ───────────────────────────────────────────────────────

@Composable
private fun WeekStrip(
    dates: List<LocalDate>,
    today: LocalDate,
    selectedDay: Int,
    countFor: (Int) -> Int,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 22.dp, bottom = 4.dp),
    ) {
        dates.forEachIndexed { index, date ->
            val day = index + 1
            val isToday = date == today
            val isSelected = day == selectedDay
            val count = countFor(day)
            val ink = Q.colors.ink
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .qClickable(pressScale = 0.94f) { onSelect(day) }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                QText(
                    WEEKDAY_SHORT[index],
                    style = QType.caption.copy(fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Normal),
                    color = if (isToday) Q.colors.seal else Q.colors.ink3,
                )
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isToday) Q.colors.seal else Color.Transparent)
                        .then(
                            if (isSelected && !isToday) {
                                Modifier.drawBehind {
                                    drawCircle(ink, radius = size.minDimension / 2 - 0.75.dp.toPx(), style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()))
                                }
                            } else {
                                Modifier
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    QText("${date.dayOfMonth}", style = QType.num(19.sp), color = if (isToday) Q.colors.onSeal else Q.colors.ink)
                }
                Row(Modifier.height(5.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(count.coerceAtMost(4)) {
                        Box(Modifier.size(4.dp).clip(CircleShape).background(Q.colors.ink3.copy(alpha = 0.7f)))
                    }
                }
            }
        }
    }
}

// ── Timeline ─────────────────────────────────────────────────────────

@Composable
private fun TimelineRow(
    course: Course,
    hue: Float,
    state: TimelineState,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
) {
    val colors = Q.colors
    val tint = colors.hue(hue)
    val hot = state == TimelineState.Now || state == TimelineState.Next
    val pulse = if (state == TimelineState.Now) {
        val transition = rememberInfiniteTransition(label = "nowPulse")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(2400), RepeatMode.Restart),
            label = "nowPulseValue",
        )
    } else {
        null
    }
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(16.dp))
            .qRowClickable(onClick = onClick)
            .graphicsLayer { alpha = if (state == TimelineState.Done) 0.42f else 1f },
    ) {
        Column(Modifier.width(48.dp).padding(top = 13.dp), horizontalAlignment = Alignment.End) {
            QText(course.startTime, style = QType.num(18.sp))
            QText(course.endTime, style = QType.num(14.sp, FontWeight.Medium), color = colors.ink3)
        }
        Box(
            modifier = Modifier
                .width(22.dp)
                .fillMaxHeight()
                .drawBehind {
                    val x = size.width / 2
                    val top = if (isFirst) 20.dp.toPx() else 0f
                    val bottom = if (isLast) 20.dp.toPx() else size.height
                    if (bottom > top) drawLine(colors.line, Offset(x, top), Offset(x, bottom), strokeWidth = 1.5.dp.toPx())
                    val center = Offset(x, 22.5.dp.toPx())
                    val r = 5.5.dp.toPx()
                    if (hot) {
                        val haloExtra = pulse?.value?.let { 2.5.dp.toPx() + 6.dp.toPx() * it } ?: 5.dp.toPx()
                        val haloAlpha = pulse?.value?.let { 1f - it } ?: 1f
                        drawCircle(colors.sealSoft.copy(alpha = colors.sealSoft.alpha * haloAlpha * 1.6f), radius = r + haloExtra, center = center)
                    }
                    drawCircle(colors.bg, radius = r + 3.dp.toPx(), center = center)
                    drawCircle(if (hot) colors.seal else tint.accent, radius = r, center = center)
                },
        )
        Column(Modifier.weight(1f).padding(start = 4.dp, end = 6.dp, top = 11.dp, bottom = 14.dp)) {
            QText(course.name, style = QType.bodyStrong, color = if (hot) colors.seal else colors.ink, maxLines = 2)
            if (course.teacher.isNotBlank()) QText(course.teacher, style = QType.sub, color = colors.ink2, maxLines = 1)
        }
        QText(
            course.classroom,
            modifier = Modifier.padding(top = 13.dp, end = 10.dp),
            style = QType.num(17.sp),
            color = colors.ink2,
            maxLines = 1,
        )
    }
}

@Composable
private fun TimelineGap(minutes: Int) {
    val line = Q.colors.line
    Row(
        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(34.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(48.dp))
        Box(
            Modifier
                .width(22.dp)
                .fillMaxHeight()
                .drawBehind {
                    drawLine(
                        line,
                        Offset(size.width / 2, 0f),
                        Offset(size.width / 2, size.height),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx())),
                    )
                },
        )
        QText("空 ${ScheduleLogic.formatDuration(minutes).replace(" 分钟", " 分钟")}", modifier = Modifier.padding(start = 4.dp), style = QType.caption, color = Q.colors.ink3)
    }
}

// ── Shortcuts ────────────────────────────────────────────────────────

@Composable
private fun ShortcutsCard(
    tools: List<HomeQuickTool>,
    statusFor: (String) -> ToolStatus,
    onOpenTool: (String) -> Unit,
    onCustomize: () -> Unit,
) {
    if (tools.isEmpty()) {
        QEmpty(
            text = "添加常用工具",
            subtitle = "取件码、电费、浴室空位会显示在这里",
            modifier = Modifier
                .padding(start = 12.dp, end = 12.dp, top = 26.dp)
                .qClickable(onClick = onCustomize),
        )
        return
    }
    Column(
        modifier = Modifier
            .padding(start = 12.dp, end = 12.dp, top = 26.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Q.colors.surface)
            .padding(horizontal = 4.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        tools.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { tool ->
                    val status = statusFor(tool.route)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .qClickable(pressScale = 0.94f, onLongClick = onCustomize) { onOpenTool(tool.route) }
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        QTintIcon(
                            icon = QIcons.forTool(tool.route),
                            hue = QlitHues.forTool(tool.route),
                            size = 48.dp,
                            iconSize = 22.dp,
                            shape = CircleShape,
                            badge = status.badge,
                        )
                        QText(tool.shortTitle, style = QType.label.copy(fontWeight = FontWeight.Normal))
                        QText(
                            status.text,
                            modifier = Modifier.padding(horizontal = 4.dp).offset(y = (-4).dp),
                            style = QType.tiny.copy(fontSize = 11.5.sp),
                            color = Q.colors.ink3,
                            maxLines = 1,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

