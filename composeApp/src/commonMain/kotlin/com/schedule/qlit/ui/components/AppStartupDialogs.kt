package com.schedule.qlit.ui.components

import androidx.compose.runtime.Composable
import com.schedule.qlit.data.model.UpdateInfo
import com.schedule.qlit.ui.design.QDialog

/**
 * 应用级启动弹窗的组合入口。
 *
 * 弹窗的状态和持久化动作仍由应用壳管理，这里只负责组合顺序和互斥关系。
 */
@Composable
fun AppStartupDialogs(
    showAnalyticsConsent: Boolean,
    onAnalyticsDismiss: () -> Unit,
    onAnalyticsConfirm: () -> Unit,
    showLivePush: Boolean,
    onLivePushDismiss: () -> Unit,
    updateInfo: UpdateInfo?,
    onUpdateDismiss: () -> Unit,
) {
    QDialog(
        show = showAnalyticsConsent,
        title = "帮助开发者改进APP",
        message = "您可以选择是否将您的手机品牌、系统版本匿名共享给开发者，这会帮助开发者更好的改进APP，选项不会影响您正常的使用APP",
        onDismissRequest = onAnalyticsDismiss,
        confirmText = "同意",
        onConfirm = onAnalyticsConfirm,
        dismissText = "拒绝",
    )

    QDialog(
        show = showLivePush && !showAnalyticsConsent,
        title = "课程实时推送暂不支持",
        message = "当前系统或安卓版本暂不支持课程实时推送，课程提醒等其他功能不受影响",
        onDismissRequest = onLivePushDismiss,
        confirmText = "我知道了",
        onConfirm = onLivePushDismiss,
        dismissText = null,
    )

    if (!showAnalyticsConsent && !showLivePush) {
        updateInfo?.let { info ->
            PlatformUpdateDialog(updateInfo = info, onDismiss = onUpdateDismiss)
        }
    }
}

/** 应用更新对话框宿主：Android 渲染 APK 更新流程；iOS 空实现。 */
@Composable
internal expect fun PlatformUpdateDialog(updateInfo: UpdateInfo, onDismiss: () -> Unit)

