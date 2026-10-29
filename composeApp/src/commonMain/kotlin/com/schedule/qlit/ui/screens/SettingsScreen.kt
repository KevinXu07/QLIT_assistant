package com.schedule.qlit.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.schedule.qlit.data.repository.UsageStatsReporter
import com.schedule.qlit.data.platformSupportsLiquidBlur
import com.schedule.qlit.data.store.platformIsUpdateCheckEnabled
import com.schedule.qlit.data.store.platformSetUpdateCheckEnabled
import com.schedule.qlit.data.platformShowsUpdateCheck
import com.schedule.qlit.ui.design.QDivider
import com.schedule.qlit.ui.design.QGroup
import com.schedule.qlit.ui.design.QIcons
import com.schedule.qlit.ui.design.QPage
import com.schedule.qlit.ui.design.QRow
import com.schedule.qlit.ui.design.QSegmented
import com.schedule.qlit.ui.design.QSwitchRow
import com.schedule.qlit.ui.design.qBottomInset
import com.schedule.qlit.ui.theme.QlitHues
import com.schedule.qlit.ui.theme.ThemeMode
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    barBlurEnabled: Boolean,
    onBarBlurChange: (Boolean) -> Unit,
    onCustomizeHome: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
) {
    var analytics by remember { mutableStateOf(UsageStatsReporter.getConsent() == true) }
    var updateEnabled by remember { mutableStateOf(platformIsUpdateCheckEnabled()) }

    QPage(title = "设置", onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .scrollEndHaptic()
                .overScrollVertical(),
            contentPadding = PaddingValues(top = padding.calculateTopPadding() + 4.dp, bottom = qBottomInset()),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            overscrollEffect = null,
        ) {
            item {
                QGroup(title = "外观") {
                    QRow(title = "深浅色", icon = QIcons.Moon, hue = QlitHues.SETTINGS)
                    QSegmented(
                        options = ThemeMode.entries.map { it.label },
                        selectedIndex = themeMode.ordinal,
                        onSelect = { onThemeModeChange(ThemeMode.entries[it]) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                    )
                    // 液态模糊依赖 miuix-blur 的 RuntimeShader 管线（API 33+），
                    // 低版本运行时已强制降级为纯色，这里直接不显示开关以免误导
                    if (platformSupportsLiquidBlur) {
                        QDivider()
                        QSwitchRow(
                            title = "底栏液态玻璃",
                            subtitle = "模糊与折射背后的内容；关闭后为纯色底栏",
                            icon = QIcons.Tools,
                            hue = QlitHues.SHOWER,
                            checked = barBlurEnabled,
                            onCheckedChange = onBarBlurChange,
                        )
                    }
                }
            }
            item {
                QGroup(title = "今天页") {
                    QRow(
                        title = "主页快捷工具",
                        subtitle = "选择显示的工具、自动刷新和订阅浴室",
                        icon = QIcons.Today,
                        hue = QlitHues.PICKUP,
                        chevron = true,
                        onClick = onCustomizeHome,
                    )
                    QDivider()
                    QRow(
                        title = "通知与实时更新",
                        subtitle = "上课前在通知栏倒计时",
                        icon = QIcons.Bell,
                        hue = QlitHues.ELECTRICITY,
                        chevron = true,
                        onClick = onOpenNotificationSettings,
                    )
                }
            }
            item {
                QGroup(
                    title = "隐私",
                    footer = "仅共享手机品牌与系统版本，用于改进兼容性，不影响任何功能。",
                ) {
                    QSwitchRow(
                        title = "匿名使用统计",
                        icon = QIcons.Shield,
                        hue = QlitHues.CAMPUS_RUN,
                        checked = analytics,
                        onCheckedChange = {
                            analytics = it
                            UsageStatsReporter.setConsent(it)
                        },
                    )
                }
            }
            if (platformShowsUpdateCheck) {
                item {
                    QGroup(title = "更新") {
                        QSwitchRow(
                            title = "接收更新",
                            subtitle = "关闭后不再检查和提醒新版本",
                            icon = QIcons.Refresh,
                            hue = QlitHues.UPDATE,
                            checked = updateEnabled,
                            onCheckedChange = {
                                updateEnabled = it
                                platformSetUpdateCheckEnabled(it)
                            },
                        )
                    }
                }
            }
        }
    }
}
