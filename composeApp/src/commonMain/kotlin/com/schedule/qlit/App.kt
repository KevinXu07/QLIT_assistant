package com.schedule.qlit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.schedule.qlit.data.checkAppUpdate
import com.schedule.qlit.data.model.ScheduleData
import com.schedule.qlit.data.model.UpdateInfo
import com.schedule.qlit.data.platformScheduleCourseReminders
import com.schedule.qlit.data.platformShowsUpdateCheck
import com.schedule.qlit.data.platformSupportsLiquidBlur
import com.schedule.qlit.data.platformSupportsLivePush
import com.schedule.qlit.data.repository.UsageStatsReporter
import com.schedule.qlit.data.store.kvStore
import com.schedule.qlit.ui.components.AppStartupDialogs
import com.schedule.qlit.ui.components.ExpressiveScaffold
import com.schedule.qlit.ui.components.LiquidMiuixBottomBar
import com.schedule.qlit.ui.components.LiquidNavigationItem
import com.schedule.qlit.ui.components.platformNotifyTabChanged
import com.schedule.qlit.ui.components.platformUsesNativeBarGlass
import com.schedule.qlit.ui.components.LocalFloatingActionButtonClearance
import com.schedule.qlit.ui.components.LocalFloatingBottomBarExtraPadding
import com.schedule.qlit.ui.design.QIcons
import com.schedule.qlit.ui.screens.AboutScreen
import com.schedule.qlit.ui.screens.AllCoursesScreen
import com.schedule.qlit.ui.screens.SettingsScreen
import com.schedule.qlit.ui.screens.TodayScreen
import com.schedule.qlit.ui.screens.ToolsScreen
import com.schedule.qlit.ui.theme.Q
import com.schedule.qlit.ui.theme.QlitTheme
import com.schedule.qlit.ui.theme.QlitUiTokens
import com.schedule.qlit.ui.theme.ThemeMode
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withTimeoutOrNull
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.shader.isRenderEffectSupported

/**
 * 应用壳的可组合状态：平台宿主（Activity / ComposeUIViewController）
 * 持有实例，深链/小组件回调经它驱动 tab 与工具页跳转。
 */
class AppRootState {
    var selectedTab by mutableIntStateOf(0)
    var pendingToolsDestination by mutableStateOf<String?>(null)
}

/** Full-screen pages opened from the profile sheet, drawn above the tab bar. */
private enum class AppOverlay { None, Settings, About }

private val OverlayEasing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

private enum class MainDestination(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    TODAY("今天", QIcons.Today),
    COURSES("课表", QIcons.Week),
    TOOLS("工具", QIcons.Tools),
}

/**
 * 共享应用壳：tab pager + 浮底栏 + 设置/关于覆盖层 + 启动弹窗。
 * Android 在 MainActivity.setContent 调用，iOS 在 ComposeUIViewController 调用；
 * 平台差异（更新检查、权限、通知设置页、Shizuku）走 expect 桥或回调参数。
 */
@OptIn(FlowPreview::class)
@Composable
fun QlitApp(
    state: AppRootState,
    viewModel: ScheduleViewModel,
    onRequestNotificationPermission: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
) {
    val schedule by viewModel.schedule.collectAsStateWithLifecycle()
    val clockMillis by viewModel.clock.collectAsStateWithLifecycle()
    val uiPreferences = remember { kvStore("ui_preferences") }
    var themeMode by remember {
        mutableStateOf(ThemeMode.fromValue(uiPreferences.getString("theme_mode", null)))
    }
    var barBlurEnabled by remember {
        mutableStateOf(
            uiPreferences.getBoolean(
                "miuix_floating_bottom_bar_blur",
                uiPreferences.getBoolean("liquid_miuix_bottom_bar", true),
            ),
        )
    }
    var overlay by rememberSaveable { mutableStateOf(AppOverlay.None) }
    var homeCustomizeRequest by remember { mutableIntStateOf(0) }
    val saveableStateHolder = rememberSaveableStateHolder()
    // A requested tab must not be overwritten by an intermediate pager
    // settle event while a programmatic page animation is still running.
    var programmaticPageTarget by remember { mutableIntStateOf(-1) }
    val pagerState = rememberPagerState(
        initialPage = state.selectedTab.coerceIn(0, MainDestination.entries.lastIndex),
        pageCount = { MainDestination.entries.size }
    )
    fun selectTab(index: Int) {
        programmaticPageTarget = index
        state.selectedTab = index
    }

    val showUpdateDialog = remember { mutableStateOf<UpdateInfo?>(null) }
    // 匿名统计首启同意弹窗（新装与升级用户都会见到一次）
    var showAnalyticsConsentDialog by remember { mutableStateOf(UsageStatsReporter.needsConsent()) }
    // 课程实时推送不支持提示：每台设备只提示一次
    var showLivePushDialog by remember {
        mutableStateOf(!platformSupportsLivePush &&
            !uiPreferences.getBoolean("live_push_unsupported_shown", false))
    }

    // 1. 开屏自动检测更新
    if (platformShowsUpdateCheck) {
        LaunchedEffect(Unit) {
            // 冷启动阶段先让首页稳定下来，再做网络更新检查，避免和首屏渲染抢资源。
            delay(2_500L)
            val info = checkAppUpdate()
            if (info != null) {
                showUpdateDialog.value = info
            }
        }
    }

    // 2. 课表变动时重新安排提醒
    var scheduledRemindersFor by remember { mutableStateOf<ScheduleData?>(null) }
    LaunchedEffect(schedule) {
        platformScheduleCourseReminders(scheduledRemindersFor, schedule)
        scheduledRemindersFor = schedule
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { state.selectedTab }
            .distinctUntilChanged()
            // Coalesce a rapid tab burst into one pager request. This
            // prevents several cancellable pager springs from leaving
            // the content between two pages.
            .debounce(80L)
            .collectLatest { targetPage ->
                if (pagerState.currentPage != targetPage ||
                    pagerState.settledPage != targetPage
                ) {
                    withTimeoutOrNull(900L) {
                        pagerState.animateScrollToPage(targetPage)
                    }
                    // Always close a cancelled/slow spring at an exact
                    // page boundary while the request is still current.
                    if (state.selectedTab == targetPage) {
                        pagerState.scrollToPage(targetPage)
                    }
                }
                if (programmaticPageTarget == targetPage) {
                    programmaticPageTarget = -1
                }
            }
    }
    LaunchedEffect(pagerState.settledPage) {
        if (programmaticPageTarget == -1 && state.selectedTab != pagerState.settledPage) {
            state.selectedTab = pagerState.settledPage
        }
    }
    // 应用内选页变化同步给 iOS 原生 tab bar（Android 为空实现）
    LaunchedEffect(Unit) {
        snapshotFlow { state.selectedTab }.collect { platformNotifyTabChanged(it) }
    }

    QlitTheme(themeMode = themeMode) {
        // Match SukiSU's layer topology: the backdrop samples only the
        // pager content, while the floating bar stays in a sibling layer.
        val barBackdrop = if (barBlurEnabled) rememberBarBackdrop() else null
        val navigationBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val floatingBarClearance = QlitUiTokens.floatingBarHeight + 24.dp + navigationBarInset

        Box(Modifier.fillMaxSize().background(Q.colors.bg)) {
            ExpressiveScaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                miuixFloatingToolbar = {
                    MainNavigationBar(
                        selectedPage = state.selectedTab,
                        onPageSelected = ::selectTab,
                        blurEnabled = barBlurEnabled,
                        blurBackdrop = barBackdrop,
                    )
                },
            ) { _ ->
                CompositionLocalProvider(
                    LocalFloatingBottomBarExtraPadding provides floatingBarClearance,
                    LocalFloatingActionButtonClearance provides floatingBarClearance,
                ) {
                    HorizontalPager(
                        state = pagerState,
                        // Main pages are switched from the navigation bar only;
                        // prevent accidental horizontal page drags from stealing
                        // gestures from the page content (the week grid swipes).
                        userScrollEnabled = false,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(if (barBackdrop != null) Modifier.layerBackdrop(barBackdrop) else Modifier),
                        // Keep every root page composed so screen-local network
                        // state and in-progress forms survive tab switches. Tool
                        // sub-pages are still created only after the user opens them.
                        beyondViewportPageCount = MainDestination.entries.size - 1,
                    ) { page ->
                        saveableStateHolder.SaveableStateProvider(key = "main_page_$page") {
                            when (MainDestination.entries[page]) {
                                MainDestination.TODAY -> TodayScreen(
                                    viewModel = viewModel,
                                    onRequestNotificationPermission = onRequestNotificationPermission,
                                    onOpenSettings = onOpenNotificationSettings,
                                    onOpenTool = { route ->
                                        state.pendingToolsDestination = route
                                        selectTab(MainDestination.TOOLS.ordinal)
                                    },
                                    onOpenCourses = { selectTab(MainDestination.COURSES.ordinal) },
                                    onOpenAppSettings = { overlay = AppOverlay.Settings },
                                    onOpenAbout = { overlay = AppOverlay.About },
                                    customizeRequest = homeCustomizeRequest,
                                )

                                MainDestination.COURSES -> AllCoursesScreen(
                                    schedule = schedule,
                                    clockMillis = clockMillis,
                                    onImportSchedule = viewModel::importSchedule,
                                    onSaveSchedule = viewModel::saveSchedule,
                                    onDownloadSchedule = viewModel::downloadSchedule,
                                    onClearSchedule = viewModel::clearSchedule,
                                )

                                MainDestination.TOOLS -> ToolsScreen(
                                    requestedDestinationName = state.pendingToolsDestination,
                                    onRequestedDestinationHandled = { state.pendingToolsDestination = null },
                                )
                            }
                        }
                    }
                }
            }

            val overlayBackState = rememberNavigationEventState(NavigationEventInfo.None)
            NavigationBackHandler(
                state = overlayBackState,
                isBackEnabled = overlay != AppOverlay.None,
                onBackCompleted = { overlay = AppOverlay.None },
            )
            var lastOverlay by remember { mutableStateOf(AppOverlay.Settings) }
            if (overlay != AppOverlay.None) lastOverlay = overlay
            AnimatedVisibility(
                visible = overlay != AppOverlay.None,
                enter = slideInHorizontally(tween(420, easing = OverlayEasing)) { it },
                exit = slideOutHorizontally(tween(320, easing = OverlayEasing)) { it } + fadeOut(tween(320)),
            ) {
                Box(Modifier.fillMaxSize().background(Q.colors.bg)) {
                    CompositionLocalProvider(LocalFloatingBottomBarExtraPadding provides 0.dp) {
                        when (lastOverlay) {
                            AppOverlay.About -> AboutScreen(onBack = { overlay = AppOverlay.None })
                            else -> SettingsScreen(
                                onBack = { overlay = AppOverlay.None },
                                themeMode = themeMode,
                                onThemeModeChange = {
                                    themeMode = it
                                    uiPreferences.putString("theme_mode", it.value)
                                },
                                barBlurEnabled = barBlurEnabled,
                                onBarBlurChange = {
                                    barBlurEnabled = it
                                    uiPreferences.putBoolean("miuix_floating_bottom_bar_blur", it)
                                },
                                onCustomizeHome = {
                                    overlay = AppOverlay.None
                                    selectTab(MainDestination.TODAY.ordinal)
                                    homeCustomizeRequest++
                                },
                                onOpenNotificationSettings = onOpenNotificationSettings,
                            )
                        }
                    }
                }
            }
        }

        // 应用级弹窗由独立组合入口统一管理显示顺序和互斥关系。
        AppStartupDialogs(
            showAnalyticsConsent = showAnalyticsConsentDialog,
            onAnalyticsDismiss = {
                UsageStatsReporter.setConsent(false)
                showAnalyticsConsentDialog = false
            },
            onAnalyticsConfirm = {
                UsageStatsReporter.setConsent(true)
                showAnalyticsConsentDialog = false
            },
            showLivePush = showLivePushDialog,
            onLivePushDismiss = {
                uiPreferences.putBoolean("live_push_unsupported_shown", true)
                showLivePushDialog = false
            },
            updateInfo = showUpdateDialog.value,
            onUpdateDismiss = { showUpdateDialog.value = null },
        )
    }
}

/** Liquid glass tab bar adapted from SukiSU's floating navigation, seal red for the current tab. */
@Composable
private fun MainNavigationBar(
    selectedPage: Int,
    onPageSelected: (Int) -> Unit,
    blurEnabled: Boolean,
    blurBackdrop: LayerBackdrop?,
) {
    // iOS：原生 UITabBarController 在 SwiftUI 层承载，Compose 不画底栏
    if (platformUsesNativeBarGlass) return
    LiquidMiuixBottomBar(
        items = MainDestination.entries.map { destination ->
            LiquidNavigationItem(
                label = destination.label,
                icon = destination.icon,
            )
        },
        selectedIndex = selectedPage,
        onSelected = onPageSelected,
        backdrop = blurBackdrop,
        isBlurEnabled = blurEnabled,
    )
}

@Composable
private fun rememberBarBackdrop(): LayerBackdrop? {
    // API < 33 时 miuix-blur 的 RuntimeShader 管线不可用，走纯色降级；
    // iOS 用原生 UIVisualEffectView 玻璃（NativeGlassPill），不用采样管线
    if (platformUsesNativeBarGlass || !platformSupportsLiquidBlur || !isRenderEffectSupported()) return null
    val background = Q.colors.bg
    return rememberLayerBackdrop {
        drawRect(background)
        drawContent()
    }
}
