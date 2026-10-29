package com.schedule.qlit.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

/**
 * 纯 Kotlin Code 128 编码器（B/C 码制）——替代 ZXing 出条码模块串，
 * Canvas 直绘比 Bitmap 更锐利，也不依赖 JVM 库，iOS 同用一份。
 */
internal object Code128 {

    private const val CODE_C = 99
    private const val CODE_B = 100
    private const val START_B = 104
    private const val START_C = 105
    private const val STOP = 106

    // 图样表（条/空交替的模块宽，STOP 为 7 段），与 ZXing CODE_PATTERNS 一致
    private val PATTERNS = listOf(
        intArrayOf(2, 1, 2, 2, 2, 2), intArrayOf(2, 2, 2, 1, 2, 2), intArrayOf(2, 2, 2, 2, 2, 1),
        intArrayOf(1, 2, 1, 2, 2, 3), intArrayOf(1, 2, 1, 3, 2, 2), intArrayOf(1, 3, 1, 2, 2, 2),
        intArrayOf(1, 2, 2, 2, 1, 3), intArrayOf(1, 2, 2, 3, 1, 2), intArrayOf(1, 3, 2, 2, 1, 2),
        intArrayOf(2, 2, 1, 2, 1, 3), intArrayOf(2, 2, 1, 3, 1, 2), intArrayOf(2, 3, 1, 2, 1, 2),
        intArrayOf(1, 1, 2, 2, 3, 2), intArrayOf(1, 2, 2, 1, 3, 2), intArrayOf(1, 2, 2, 2, 3, 1),
        intArrayOf(1, 1, 3, 2, 2, 2), intArrayOf(1, 2, 3, 1, 2, 2), intArrayOf(1, 2, 3, 2, 2, 1),
        intArrayOf(2, 2, 3, 2, 1, 1), intArrayOf(2, 2, 1, 1, 3, 2), intArrayOf(2, 2, 1, 2, 3, 1),
        intArrayOf(2, 1, 3, 2, 1, 2), intArrayOf(2, 2, 3, 1, 1, 2), intArrayOf(3, 1, 2, 1, 3, 1),
        intArrayOf(3, 1, 1, 2, 2, 2), intArrayOf(3, 2, 1, 1, 2, 2), intArrayOf(3, 2, 1, 2, 2, 1),
        intArrayOf(3, 1, 2, 2, 1, 2), intArrayOf(3, 2, 2, 1, 1, 2), intArrayOf(3, 2, 2, 2, 1, 1),
        intArrayOf(2, 1, 2, 1, 2, 3), intArrayOf(2, 1, 2, 3, 2, 1), intArrayOf(2, 3, 2, 1, 2, 1),
        intArrayOf(1, 1, 1, 3, 2, 3), intArrayOf(1, 3, 1, 1, 2, 3), intArrayOf(1, 3, 1, 3, 2, 1),
        intArrayOf(1, 1, 2, 3, 1, 3), intArrayOf(1, 3, 2, 1, 1, 3), intArrayOf(1, 3, 2, 3, 1, 1),
        intArrayOf(2, 1, 1, 3, 1, 3), intArrayOf(2, 3, 1, 1, 1, 3), intArrayOf(2, 3, 1, 3, 1, 1),
        intArrayOf(1, 1, 2, 1, 3, 3), intArrayOf(1, 1, 2, 3, 3, 1), intArrayOf(1, 3, 2, 1, 3, 1),
        intArrayOf(1, 1, 3, 1, 2, 3), intArrayOf(1, 1, 3, 3, 2, 1), intArrayOf(1, 3, 3, 1, 2, 1),
        intArrayOf(3, 1, 3, 1, 2, 1), intArrayOf(2, 1, 1, 3, 3, 1), intArrayOf(2, 3, 1, 1, 3, 1),
        intArrayOf(2, 1, 3, 1, 1, 3), intArrayOf(2, 1, 3, 3, 1, 1), intArrayOf(2, 1, 3, 1, 3, 1),
        intArrayOf(3, 1, 1, 1, 2, 3), intArrayOf(3, 1, 1, 3, 2, 1), intArrayOf(3, 3, 1, 1, 2, 1),
        intArrayOf(3, 1, 2, 1, 1, 3), intArrayOf(3, 1, 2, 3, 1, 1), intArrayOf(3, 3, 2, 1, 1, 1),
        intArrayOf(3, 1, 4, 1, 1, 1), intArrayOf(2, 2, 1, 4, 1, 1), intArrayOf(4, 3, 1, 1, 1, 1),
        intArrayOf(1, 1, 1, 2, 2, 4), intArrayOf(1, 1, 1, 4, 2, 2), intArrayOf(1, 2, 1, 1, 2, 4),
        intArrayOf(1, 2, 1, 4, 2, 1), intArrayOf(1, 4, 1, 1, 2, 2), intArrayOf(1, 4, 1, 2, 2, 1),
        intArrayOf(1, 1, 2, 2, 1, 4), intArrayOf(1, 1, 2, 4, 1, 2), intArrayOf(1, 2, 2, 1, 1, 4),
        intArrayOf(1, 2, 2, 4, 1, 1), intArrayOf(1, 4, 2, 1, 1, 2), intArrayOf(1, 4, 2, 2, 1, 1),
        intArrayOf(2, 4, 1, 2, 1, 1), intArrayOf(2, 2, 1, 1, 1, 4), intArrayOf(4, 1, 3, 1, 1, 1),
        intArrayOf(2, 4, 1, 1, 1, 2), intArrayOf(1, 3, 4, 1, 1, 1), intArrayOf(1, 1, 1, 2, 4, 2),
        intArrayOf(1, 2, 1, 1, 4, 2), intArrayOf(1, 2, 1, 2, 4, 1), intArrayOf(1, 1, 4, 2, 1, 2),
        intArrayOf(1, 2, 4, 1, 1, 2), intArrayOf(1, 2, 4, 2, 1, 1), intArrayOf(4, 1, 1, 2, 1, 2),
        intArrayOf(4, 2, 1, 1, 1, 2), intArrayOf(4, 2, 1, 2, 1, 1), intArrayOf(2, 1, 2, 1, 4, 1),
        intArrayOf(2, 1, 4, 1, 2, 1), intArrayOf(4, 1, 2, 1, 2, 1), intArrayOf(1, 1, 1, 1, 4, 3),
        intArrayOf(1, 1, 1, 3, 4, 1), intArrayOf(1, 3, 1, 1, 4, 1), intArrayOf(1, 1, 4, 1, 1, 3),
        intArrayOf(1, 1, 4, 3, 1, 1), intArrayOf(4, 1, 1, 1, 1, 3), intArrayOf(4, 1, 1, 3, 1, 1),
        intArrayOf(2, 1, 1, 4, 1, 2), intArrayOf(2, 1, 1, 2, 1, 4), intArrayOf(2, 1, 1, 2, 3, 2),
        intArrayOf(2, 3, 3, 1, 1, 1, 2),
    )

    /**
     * 编码为模块布尔串（true=条）。纯数字串走 C 码制压缩，
     * 奇数长度先把首数字用 B 编再切 C；其余一律 B（ASCII 32–127）。
     */
    fun encode(text: String): BooleanArray {
        require(text.isNotEmpty())
        val codes = mutableListOf<Int>()
        var i = 0
        if (text.all { it in '0'..'9' } && text.length >= 4) {
            if (text.length % 2 == 1) {
                codes += START_B
                codes += text[0].code - 32
                codes += CODE_C
                i = 1
            } else {
                codes += START_C
            }
            while (i + 1 < text.length) {
                codes += (text[i] - '0') * 10 + (text[i + 1] - '0')
                i += 2
            }
        } else {
            codes += START_B
            for (ch in text) {
                require(ch.code in 32..127) { "Code128-B 不支持字符 U+${ch.code.toString(16)}" }
                codes += ch.code - 32
            }
        }

        var sum = codes[0]
        for (idx in 1 until codes.size) sum += codes[idx] * idx
        codes += sum % 103
        codes += STOP

        val bits = ArrayList<Boolean>((codes.size - 1) * 11 + 13)
        for (code in codes) {
            var bar = true
            for (w in PATTERNS[code]) {
                repeat(w) { bits += bar }
                bar = !bar
            }
        }
        return bits.toBooleanArray()
    }
}

/** Code 128 条码直绘：模块按可用宽度均分，连续条合并为一次 drawRect。 */
@Composable
internal fun Code128Barcode(
    code: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Black,
) {
    val modules = remember(code) { runCatching { Code128.encode(code) }.getOrNull() } ?: return
    Canvas(modifier = modifier) {
        val moduleWidth = size.width / modules.size
        var x = 0f
        var i = 0
        while (i < modules.size) {
            var j = i
            while (j < modules.size && modules[j] == modules[i]) j++
            if (modules[i]) {
                drawRect(
                    color = color,
                    topLeft = Offset(x, 0f),
                    size = Size((j - i) * moduleWidth, size.height),
                )
            }
            x += (j - i) * moduleWidth
            i = j
        }
    }
}
