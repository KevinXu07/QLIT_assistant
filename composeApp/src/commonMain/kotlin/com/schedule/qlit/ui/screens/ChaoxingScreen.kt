package com.schedule.qlit.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schedule.qlit.data.model.ChaoxingTask
import com.schedule.qlit.data.model.ChaoxingTaskResult
import com.schedule.qlit.data.repository.ChaoxingRepository
import com.schedule.qlit.ui.components.ExpressiveScaffold
import com.schedule.qlit.ui.components.LocalFloatingBottomBarExtraPadding
import com.schedule.qlit.ui.components.AdaptiveSnackbarHost
import com.schedule.qlit.ui.components.rememberAdaptiveSnackbarHostState
import com.schedule.qlit.ui.components.ExpressiveContentInsets
import com.schedule.qlit.ui.components.AdaptiveTopAppBar
import com.schedule.qlit.ui.components.AdaptiveIconButton
import com.schedule.qlit.ui.components.AdaptiveButton
import com.schedule.qlit.ui.components.AdaptiveIcon
import com.schedule.qlit.ui.components.AdaptiveSurface
import com.schedule.qlit.ui.components.AdaptiveText
import com.schedule.qlit.ui.components.AdaptiveTextField
import com.schedule.qlit.ui.components.AdaptiveLinearProgressIndicator
import com.schedule.qlit.ui.components.AdaptiveProgressIndicator
import com.schedule.qlit.ui.components.SegmentedChoiceRow
import com.schedule.qlit.ui.components.TonalCard
import com.schedule.qlit.ui.theme.QlitTypography
import top.yukonga.miuix.kmp.basic.Badge
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Refresh
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.RoundedCornerShape
import com.schedule.qlit.ui.design.QActionRow
import com.schedule.qlit.ui.design.QButton
import com.schedule.qlit.ui.design.QButtonKind
import com.schedule.qlit.ui.design.QCard
import com.schedule.qlit.ui.design.QChip
import com.schedule.qlit.ui.design.QDialog
import com.schedule.qlit.ui.design.QDivider
import com.schedule.qlit.ui.design.QEmpty
import com.schedule.qlit.ui.design.QGroup
import com.schedule.qlit.ui.design.QIconButton
import com.schedule.qlit.ui.design.QIcons
import com.schedule.qlit.ui.design.QInfoLine
import com.schedule.qlit.ui.design.QList
import com.schedule.qlit.ui.design.QLoading
import com.schedule.qlit.ui.design.QNotice
import com.schedule.qlit.ui.design.QPage
import com.schedule.qlit.ui.design.QProgressBar
import com.schedule.qlit.ui.design.QRow
import com.schedule.qlit.ui.design.QSegmented
import com.schedule.qlit.ui.design.QSheet
import com.schedule.qlit.ui.design.QStat
import com.schedule.qlit.ui.design.QSwitchRow
import com.schedule.qlit.ui.design.QText
import com.schedule.qlit.ui.design.QTintIcon
import com.schedule.qlit.ui.design.qBottomInset
import com.schedule.qlit.ui.design.qClickable
import com.schedule.qlit.ui.theme.Q
import com.schedule.qlit.ui.theme.QType
import com.schedule.qlit.ui.theme.QlitHues

private enum class ChaoxingScreenPhase { LOGIN, CONTENT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChaoxingScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = rememberAdaptiveSnackbarHostState()

    var phase by remember { mutableStateOf(
        if (ChaoxingRepository.isLoggedIn()) ChaoxingScreenPhase.CONTENT
        else ChaoxingScreenPhase.LOGIN
    ) }
    var loading by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<ChaoxingTaskResult?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0=作业 1=考试
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val miuixScrollBehavior = MiuixScrollBehavior()

    // 拉取作业 + 考试；cookie 失效会回到登录态。
    fun loadTasks() {
        scope.launch {
            loading = true
            try {
                result = ChaoxingRepository.fetchTasks()
                com.schedule.qlit.data.repository.refreshWidgets()
            } catch (e: ChaoxingRepository.CookieExpiredException) {
                result = null
                phase = ChaoxingScreenPhase.LOGIN
                snackbarHostState.showSnackbar("登录已失效，请重新登录")
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("加载失败：${e.message ?: e::class.simpleName}")
            } finally {
                loading = false
            }
        }
    }

    // 进入内容页时自动拉一次
    LaunchedEffect(phase) {
        if (phase == ChaoxingScreenPhase.CONTENT && result == null) {
            loadTasks()
        }
    }

    QPage(
        title = "超星待办",
        onBack = onBack,
        actions = {
            if (phase == ChaoxingScreenPhase.CONTENT) {
                QIconButton(QIcons.Refresh, "刷新", onClick = { loadTasks() }, enabled = !loading, tint = Q.colors.ink)
                QIconButton(
                    QIcons.Logout,
                    "退出登录",
                    onClick = {
                        ChaoxingRepository.logout()
                        result = null
                        phase = ChaoxingScreenPhase.LOGIN
                    },
                    tint = Q.colors.ink,
                )
            }
        },
        snackbarHost = { AdaptiveSnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize()) {
            when (phase) {
                ChaoxingScreenPhase.LOGIN -> ChaoxingLoginContent(
                    topPadding = innerPadding.calculateTopPadding(),
                    loading = loading,
                    onLoginClick = { uname, pwd ->
                        scope.launch {
                            loading = true
                            val outcome = ChaoxingRepository.login(uname, pwd)
                            loading = false
                            if (outcome.success) {
                                result = null
                                phase = ChaoxingScreenPhase.CONTENT
                            } else {
                                snackbarHostState.showSnackbar(outcome.message)
                            }
                        }
                    }
                )
                ChaoxingScreenPhase.CONTENT -> ChaoxingResultContent(
                    topPadding = innerPadding.calculateTopPadding(),
                    result = result,
                    loading = loading,
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
            }
        }
    }
}

// ── 登录页 ─────────────────────────────────────────────────

@Composable
private fun ChaoxingLoginContent(
    topPadding: androidx.compose.ui.unit.Dp,
    loading: Boolean,
    onLoginClick: (uname: String, password: String) -> Unit
) {
    var uname by remember { mutableStateOf(ChaoxingRepository.savedUname()) }
    var password by remember { mutableStateOf(ChaoxingRepository.savedPassword()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = topPadding + 4.dp, bottom = qBottomInset()),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            QCard(
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                contentPadding = PaddingValues(20.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    QTintIcon(QIcons.Todo, QlitHues.CHAOXING, size = 48.dp, iconSize = 24.dp, shape = RoundedCornerShape(14.dp))
                    Column {
                        QText("学习通登录", style = QType.title)
                        QText("查看作业与考试待办", style = QType.sub, color = Q.colors.ink2)
                    }
                }
                Column(Modifier.padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdaptiveTextField(
                        value = uname,
                        onValueChange = { uname = it },
                        label = "手机号 / 账号",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        modifier = Modifier.fillMaxWidth()
                    )
                    AdaptiveTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "密码",
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                    QButton(
                        text = "登录",
                        onClick = { onLoginClick(uname, password) },
                        enabled = uname.isNotBlank() && password.isNotBlank(),
                        loading = loading,
                        kind = QButtonKind.Primary,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        item {
            QText(
                "账号密码仅保存在本机；登录后使用网页版 cookie 同步列表。",
                modifier = Modifier.padding(horizontal = 24.dp),
                style = QType.caption,
                color = Q.colors.ink3,
            )
        }
    }
}

// ── 结果页（Tab 切换作业/考试） ──────────────────────────────

@Composable
private fun ChaoxingResultContent(
    topPadding: androidx.compose.ui.unit.Dp,
    result: ChaoxingTaskResult?,
    loading: Boolean,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val works = result?.works.orEmpty()
    val exams = result?.exams.orEmpty()
    // 待完成的排前面；待完成组内按剩余时间升序（越紧急越靠前），
    // 已完成的保持接口原顺序，避免列表看起来"乱"。
    val items: List<ChaoxingTask> = remember(works, exams, selectedTab) {
        (if (selectedTab == 0) works else exams).sortedWith(
            compareByDescending<ChaoxingTask> { it.pending }
                .thenBy { chaoxingRemainMinutes(it) }
        )
    }
    val pendingWork = result?.pendingWorkCount ?: 0
    val pendingExam = result?.pendingExamCount ?: 0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = topPadding + 4.dp, bottom = qBottomInset()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            QCard(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    QStat(
                        value = "${pendingWork + pendingExam}",
                        label = "待办",
                        modifier = Modifier.weight(1f),
                        valueColor = if (pendingWork + pendingExam > 0) Q.colors.seal else Q.colors.ink,
                    )
                    QStat(value = "$pendingWork/${works.size}", label = "作业", modifier = Modifier.weight(1f))
                    QStat(value = "$pendingExam/${exams.size}", label = "考试", modifier = Modifier.weight(1f))
                }
                if (loading) QProgressBar(progress = null, modifier = Modifier.padding(top = 14.dp).fillMaxWidth())
            }
        }

        item {
            QSegmented(
                options = listOf("作业", "考试"),
                selectedIndex = selectedTab,
                onSelect = onTabSelected,
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
            )
        }

        when {
            items.isEmpty() && loading -> item { QLoading() }
            items.isEmpty() -> item {
                QEmpty(
                    if (selectedTab == 0) "暂无作业" else "暂无考试",
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            else -> {
                item {
                    QList(modifier = Modifier.padding(horizontal = 16.dp)) {
                        items.forEachIndexed { index, task ->
                            if (index > 0) QDivider(startInset = 14.dp)
                            ChaoxingTaskRow(task)
                        }
                    }
                }
                item {
                    QText(
                        text = "共 ${items.size} 项",
                        modifier = Modifier.fillMaxWidth(),
                        style = QType.caption,
                        color = Q.colors.ink3,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

/**
 * 把"剩余129小时43分钟"/"剩余3天"这类剩余时间文本折算成分钟，
 * 用于待办排序；解析不出来（已完成/无剩余文本）排到最后。
 */
private fun chaoxingRemainMinutes(task: ChaoxingTask): Long {
    if (!task.pending) return Long.MAX_VALUE
    val text = task.remainText
    var total = 0L
    var matched = false
    Regex("(\\d+)天").find(text)?.let { matched = true; total += it.groupValues[1].toLong() * 1440 }
    Regex("(\\d+)小时").find(text)?.let { matched = true; total += it.groupValues[1].toLong() * 60 }
    Regex("(\\d+)分钟").find(text)?.let { matched = true; total += it.groupValues[1].toLong() }
    return if (matched) total else Long.MAX_VALUE
}

@Composable
private fun ChaoxingTaskRow(task: ChaoxingTask) {
    // 24 小时内到期的待办是“此刻”的事，用校徽红；其余保持墨色
    val urgent = task.pending && chaoxingRemainMinutes(task) < 24 * 60
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 14.dp)
            .graphicsLayer { alpha = if (task.statusCode == 2) 0.55f else 1f },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusChip(task)
            if (task.remainText.isNotBlank()) {
                Spacer(Modifier.weight(1f))
                QText(
                    task.remainText,
                    style = QType.label,
                    color = if (urgent) Q.colors.seal else Q.colors.ink2,
                )
            }
        }
        QText(task.title.ifBlank { "（无标题）" }, style = QType.bodyStrong)
        if (task.courseName.isNotBlank()) {
            QText(task.courseName, style = QType.sub, color = Q.colors.ink2)
        }
    }
}

@Composable
private fun StatusChip(task: ChaoxingTask) {
    val label = when (task.statusCode) {
        0 -> "待完成"
        1 -> "待批阅"
        2 -> "已完成"
        else -> task.statusText.ifBlank { "未知" }
    }
    when (task.statusCode) {
        0 -> QChip(label, container = Q.colors.sealSoft, content = Q.colors.seal)
        1 -> QChip(label, hue = QlitHues.CHAOXING)
        else -> QChip(label)
    }
    QChip(if (task.isWork) "作业" else "考试")
}
