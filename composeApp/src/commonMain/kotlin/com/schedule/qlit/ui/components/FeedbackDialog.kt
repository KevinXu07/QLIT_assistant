package com.schedule.qlit.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.schedule.qlit.data.IODispatcher
import com.schedule.qlit.data.deviceInfo
import com.schedule.qlit.data.net.Http
import com.schedule.qlit.ui.design.QDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * 意见反馈弹窗（Formspree）。
 *
 * 组件保持常驻组合、由 [show] 驱动显隐：窗口对话框的退场动画依赖 show 的
 * 状态跳变，若条件组合，点关闭的瞬间窗口会被直接移出组合，退场动画没有机会播放。
 * 提交为 JSON，正文自动附一行匿名设备信息（机型/系统/App 版本），联系方式形如
 * 邮箱时作为 Formspree 回信地址。
 */
@Composable
fun FeedbackDialog(
    show: Boolean,
    appVersionName: String,
    onDismiss: () -> Unit,
) {
    var content by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val canSend = !sending && content.trim().length >= 5

    LaunchedEffect(show) {
        if (!show && !sending) {
            content = ""
            contact = ""
        }
    }

    fun submit() {
        if (!canSend) return
        sending = true
        scope.launch {
            val ok = submitFeedback(appVersionName, content.trim(), contact.trim())
            sending = false
            if (ok) {
                content = ""
                contact = ""
                onDismiss()
            }
        }
    }

    QDialog(
        show = show,
        title = "意见反馈",
        message = "功能建议或遇到的问题都可以告诉我们",
        onDismissRequest = { if (!sending) onDismiss() },
        confirmText = if (sending) "发送中" else "发送",
        onConfirm = ::submit,
        confirmEnabled = canSend,
        loading = sending,
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AdaptiveTextField(
                value = content,
                onValueChange = { if (it.length <= 500) content = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                label = "说说你的想法或遇到的问题",
                singleLine = false,
            )
            AdaptiveTextField(
                value = contact,
                onValueChange = { contact = it },
                modifier = Modifier.fillMaxWidth(),
                label = "联系方式，选填（邮箱可直接收到回复）",
            )
        }
    }
}

/** 返回是否提交成功（结果以轻提示提示） */
private suspend fun submitFeedback(
    appVersionName: String,
    content: String,
    contact: String,
): Boolean = withContext(IODispatcher) {
    val device = deviceInfo()
    val ok = try {
        val body = buildJsonObject {
            put("反馈内容", content)
            put("联系方式", contact.ifBlank { "未填写" })
            put(
                "设备信息",
                "${device.brand} ${device.model} · ${device.osName} ${device.osVersion} · V$appVersionName",
            )
            // Formspree 约定字段：邮箱形式的联系方式可直接作为回信地址
            if (contact.contains("@")) put("_replyto", contact)
        }
        val resp = Http.post(
            "https://formspree.io/f/xdekbzjp",
            body.toString().encodeToByteArray(),
            headers = mapOf(
                "Content-Type" to "application/json",
                "Accept" to "application/json",
            ),
            timeoutMs = 8_000,
        )
        resp.code in 200..299
    } catch (_: Exception) {
        false
    }
    withContext(Dispatchers.Main) {
        showPlatformToast(if (ok) "感谢反馈！" else "发送失败，请稍后重试")
    }
    ok
}
