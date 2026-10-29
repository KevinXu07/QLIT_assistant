package com.schedule.qlit.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.schedule.qlit.data.deviceInfo
import com.schedule.qlit.data.model.UpdateInfo
import com.schedule.qlit.data.repository.UpdateManager
import com.schedule.qlit.ui.design.QIcons
import com.schedule.qlit.ui.design.QRow
import com.schedule.qlit.ui.theme.Q
import com.schedule.qlit.ui.theme.QlitHues
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator

@Composable
internal actual fun PlatformUpdateEntry() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val versionName = remember { deviceInfo().versionName.ifBlank { "?" } }
    var checking by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }

    QRow(
        "检查更新",
        icon = QIcons.Refresh,
        hue = QlitHues.UPDATE,
        value = "V$versionName",
        chevron = !checking,
        trailing = if (checking) {
            { InfiniteProgressIndicator(color = Q.colors.ink3) }
        } else {
            null
        },
        onClick = {
            if (checking) return@QRow
            if (!UpdateManager.isUpdateEnabled(context)) {
                showPlatformToast("已关闭接收更新，可在设置中开启")
                return@QRow
            }
            checking = true
            scope.launch {
                val info = UpdateManager.checkUpdate(context)
                checking = false
                if (info != null) updateInfo = info
                else showPlatformToast("当前已是最新版本")
            }
        },
    )
    updateInfo?.let { UpdateDialog(updateInfo = it, onDismiss = { updateInfo = null }) }
}
