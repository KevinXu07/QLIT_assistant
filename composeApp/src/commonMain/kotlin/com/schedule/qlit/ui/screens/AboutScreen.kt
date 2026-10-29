package com.schedule.qlit.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.schedule.qlit.composeapp.generated.resources.Res
import com.schedule.qlit.composeapp.generated.resources.app_icon_foreground
import com.schedule.qlit.composeapp.generated.resources.avatar_kevin
import com.schedule.qlit.data.deviceInfo
import com.schedule.qlit.data.platformOpenUrl
import com.schedule.qlit.ui.design.QDivider
import com.schedule.qlit.ui.design.QGroup
import com.schedule.qlit.ui.design.QIcons
import com.schedule.qlit.ui.design.QPage
import com.schedule.qlit.ui.design.QRow
import com.schedule.qlit.ui.design.QSheet
import com.schedule.qlit.ui.design.QText
import com.schedule.qlit.ui.design.qBottomInset
import com.schedule.qlit.ui.theme.Q
import com.schedule.qlit.ui.theme.QType
import com.schedule.qlit.ui.theme.QlitHues
import org.jetbrains.compose.resources.painterResource
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/** About page: app identity, developer and device info, licenses. */
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val device = remember { deviceInfo() }
    var showLicense by remember { mutableStateOf(false) }
    val license by produceState(initialValue = "") {
        value = runCatching {
            Res.readBytes("files/BarlowSemiCondensed-OFL.txt").decodeToString()
        }.getOrDefault("")
    }

    QPage(title = "关于", onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .scrollEndHaptic()
                .overScrollVertical(),
            contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = qBottomInset()),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            overscrollEffect = null,
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    AppIcon(Modifier.size(88.dp).clip(RoundedCornerShape(22.dp)))
                    Spacer(Modifier.height(6.dp))
                    QText("QLIT 小助手", style = QType.pageTitle)
                    QText("V${device.versionName} · 课程表与校园工具箱", style = QType.sub, color = Q.colors.ink2)
                }
            }
            item {
                QGroup(title = "开发者") {
                    QRow(
                        title = "Rana",
                        subtitle = "项目开发者 · 用爱制作",
                        leading = {
                            Image(
                                painter = painterResource(Res.drawable.avatar_kevin),
                                contentDescription = "开发者头像",
                                modifier = Modifier.size(44.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop,
                            )
                        },
                    )
                    QDivider()
                    QRow(
                        title = "开源地址",
                        subtitle = "github.com/KevinXu07/QLIT_assistant",
                        icon = QIcons.Link,
                        hue = QlitHues.ABOUT,
                        chevron = true,
                        onClick = { platformOpenUrl("https://github.com/KevinXu07/QLIT_assistant") },
                    )
                }
            }
            item {
                QGroup(title = "设备信息") {
                    val infoList = buildList {
                        add("应用版本" to device.versionName)
                        add("内部版本" to device.versionCode)
                        add("设备" to "${device.brand} ${device.model}")
                        add("${device.osName} 版本" to device.osVersion)
                        if (device.sdk > 0) add("API 级别" to device.sdk.toString())
                    }
                    infoList.forEachIndexed { index, (label, value) ->
                        if (index > 0) QDivider(startInset = 14.dp)
                        QRow(title = label, value = value)
                    }
                }
            }
            item {
                QGroup(title = "开源许可") {
                    QRow(
                        title = "Barlow Semi Condensed",
                        subtitle = "数字字体 · SIL Open Font License 1.1",
                        chevron = true,
                        onClick = { showLicense = true },
                    )
                }
            }
        }
    }

    QSheet(show = showLicense, onDismissRequest = { showLicense = false }, title = "SIL Open Font License") {
        QText(
            license,
            modifier = Modifier
                .heightIn(max = 460.dp)
                .verticalScroll(rememberScrollState()),
            style = QType.caption,
            color = Q.colors.ink2,
        )
    }
}

/**
 * 应用图标：Android/iOS 通用 —— 自适应图标 = 白底 + 前景图层，
 * 这里用 composeResources 里的前景图还原该外观。
 */
@Composable
internal fun AppIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(Res.drawable.app_icon_foreground),
            contentDescription = "QLIT 小助手应用图标",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )
    }
}
