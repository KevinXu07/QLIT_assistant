package com.schedule.qlit.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schedule.qlit.data.deviceInfo
import com.schedule.qlit.data.platformShowsUpdateCheck
import com.schedule.qlit.data.repository.CourseRepository
import com.schedule.qlit.ui.components.FeedbackDialog
import com.schedule.qlit.ui.components.PlatformUpdateEntry
import com.schedule.qlit.ui.design.QDivider
import com.schedule.qlit.ui.design.QIcons
import com.schedule.qlit.ui.design.QList
import com.schedule.qlit.ui.design.QRow
import com.schedule.qlit.ui.design.QSheet
import com.schedule.qlit.ui.design.QText
import com.schedule.qlit.ui.schedule.QAvatar
import com.schedule.qlit.ui.theme.Q
import com.schedule.qlit.ui.theme.QType
import com.schedule.qlit.ui.theme.QlitHues
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** "Me" sheet behind the avatar: settings, update check, feedback, about. */
@OptIn(ExperimentalTime::class)
@Composable
fun ProfileSheet(
    show: Boolean,
    name: String?,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    val versionName = remember { deviceInfo().versionName.ifBlank { "?" } }
    val importTime = remember(show) {
        CourseRepository.getImportTime()?.let {
            val dt = Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.currentSystemDefault())
            "${dt.monthNumber}月${dt.day}日 ${dt.hour.toString().padStart(2, '0')}:${dt.minute.toString().padStart(2, '0')}"
        }
    }
    var showFeedback by remember { mutableStateOf(false) }

    QSheet(show = show, onDismissRequest = onDismiss) {
        Row(
            modifier = Modifier.padding(bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            QAvatar(name = name.orEmpty(), size = 52.dp)
            Column {
                QText(name?.ifBlank { null } ?: "QLIT 同学", style = QType.title.copy(fontSize = 19.sp))
                QText(
                    if (importTime != null) "课表导入于 $importTime" else "还没有导入课表",
                    modifier = Modifier.padding(top = 2.dp),
                    style = QType.sub,
                    color = Q.colors.ink2,
                )
            }
        }
        QList(color = Q.colors.bg) {
            QRow("设置", icon = QIcons.Settings, hue = QlitHues.SETTINGS, chevron = true, onClick = onOpenSettings)
            QDivider()
            if (platformShowsUpdateCheck) {
                PlatformUpdateEntry()
                QDivider()
            }
            QRow("意见反馈", icon = QIcons.Feedback, hue = QlitHues.FEEDBACK, chevron = true, onClick = { showFeedback = true })
            QDivider()
            QRow("关于 QLIT 小助手", icon = QIcons.Info, hue = QlitHues.ABOUT, chevron = true, onClick = onOpenAbout)
        }
    }

    FeedbackDialog(show = showFeedback, appVersionName = versionName, onDismiss = { showFeedback = false })
}
