package com.schedule.qlit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.schedule.qlit.ui.design.QDivider
import com.schedule.qlit.ui.design.QGroup
import com.schedule.qlit.ui.design.QIcons
import com.schedule.qlit.ui.design.QRow
import com.schedule.qlit.ui.design.QText
import com.schedule.qlit.ui.design.QStatusBarScrim
import com.schedule.qlit.ui.design.qBottomInset
import com.schedule.qlit.ui.theme.Q
import com.schedule.qlit.ui.theme.QType
import com.schedule.qlit.ui.theme.QlitHues
import com.schedule.qlit.data.repository.loadToolStatuses
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

private data class ToolEntry(
    val route: String,
    val title: String,
    val summary: String,
    val onClick: () -> Unit,
)

/** Trailing status of a tool row; [hot] marks something that waits for the user. */
private data class ToolValue(val text: String, val hot: Boolean = false)

private fun toolValues(): Map<String, ToolValue> =
    loadToolStatuses().mapValues { (_, v) -> ToolValue(v.text, v.hot) }

@Composable
internal fun ToolsMiuixScreen(
    onNavigateToTaobaoPickup: () -> Unit,
    onNavigateToChaoxing: () -> Unit,
) {
    val values = toolValues()
    val routes = platformToolRoutes()
    fun available(name: String) = runCatching { ToolsDestination.valueOf(name) in routes }.getOrDefault(false)

    val campus = buildList {
        if (available("TAOBAO_PICKUP")) add(ToolEntry("TAOBAO_PICKUP", "淘宝取件", "驿站取件码与身份码", onNavigateToTaobaoPickup))
    }
    val study = buildList {
        if (available("CHAOXING")) add(ToolEntry("CHAOXING", "超星待办", "作业与考试", onNavigateToChaoxing))
    }

    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(Modifier.fillMaxSize().background(Q.colors.bg)) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .scrollEndHaptic()
            .overScrollVertical(),
        contentPadding = PaddingValues(top = statusTop, bottom = qBottomInset()),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        overscrollEffect = null,
    ) {
        item {
            QText("工具", modifier = Modifier.padding(start = 22.dp, top = 14.dp), style = QType.pageTitle)
        }
        if (campus.isNotEmpty()) item { ToolGroup("校园生活", campus, values) }
        if (study.isNotEmpty()) item { ToolGroup("学习", study, values) }
    }
    QStatusBarScrim()
    }
}

@Composable
private fun ToolGroup(title: String, tools: List<ToolEntry>, values: Map<String, ToolValue>) {
    QGroup(title = title) {
        tools.forEachIndexed { index, tool ->
            if (index > 0) QDivider()
            val value = values[tool.route]
            QRow(
                title = tool.title,
                subtitle = tool.summary,
                icon = QIcons.forTool(tool.route),
                hue = QlitHues.forTool(tool.route),
                value = value?.text,
                valueHot = value?.hot == true,
                chevron = true,
                onClick = tool.onClick,
            )
        }
    }
}
