package com.schedule.qlit.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

/**
 * 单文件文本选择器（课表 JSON 导入用）。返回启动函数；
 * 取消/读取失败回调 null，成功回调文件全文。
 */
@Composable
internal expect fun rememberTextFilePicker(onText: (String?) -> Unit): () -> Unit

/** 平台位图解码（验证码图片）。解码失败返回 null。 */
internal expect fun decodeImageBitmap(bytes: ByteArray): ImageBitmap?
