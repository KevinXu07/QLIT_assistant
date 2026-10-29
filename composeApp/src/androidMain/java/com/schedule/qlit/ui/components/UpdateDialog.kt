package com.schedule.qlit.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schedule.qlit.data.model.UpdateInfo
import com.schedule.qlit.data.repository.UpdateManager
import com.schedule.qlit.ui.design.QDialog
import com.schedule.qlit.ui.design.QProgressBar
import com.schedule.qlit.ui.design.QText
import com.schedule.qlit.ui.theme.Q
import com.schedule.qlit.ui.theme.QType
import kotlinx.coroutines.launch

@Composable
fun UpdateDialog(
    updateInfo: UpdateInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    fun startDownload() {
        downloading = true
        errorMsg = null
        scope.launch {
            UpdateManager.downloadAndInstall(
                context = context,
                apkUrl = updateInfo.apkUrl,
                onProgress = { p -> progress = p },
                onComplete = {
                    downloading = false
                    onDismiss()
                },
                onError = { e ->
                    errorMsg = e
                    downloading = false
                }
            )
        }
    }

    QDialog(
        show = true,
        title = "发现新版本 ${updateInfo.versionName}",
        onDismissRequest = { if (!downloading) onDismiss() },
        confirmText = if (downloading) "正在下载" else "立即更新",
        onConfirm = { startDownload() },
        dismissText = if (downloading) null else "以后再说",
        loading = downloading,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            QText("更新内容", style = QType.label, color = Q.colors.ink2)
            QText(
                text = updateInfo.changelog,
                modifier = Modifier
                    .heightIn(max = 280.dp)
                    .verticalScroll(rememberScrollState()),
                style = QType.body,
            )
            if (downloading) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    QProgressBar(progress = progress, modifier = Modifier.fillMaxWidth())
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        QText("正在下载", style = QType.caption, color = Q.colors.ink2)
                        QText("${(progress * 100).toInt()}%", style = QType.num(13.sp), color = Q.colors.ink2)
                    }
                }
            }
            errorMsg?.let {
                QText(text = it, style = QType.sub, color = Q.colors.seal)
            }
        }
    }
}
