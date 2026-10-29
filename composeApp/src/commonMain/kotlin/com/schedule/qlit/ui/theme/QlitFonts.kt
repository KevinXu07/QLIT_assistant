package com.schedule.qlit.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.schedule.qlit.composeapp.generated.resources.Res
import com.schedule.qlit.composeapp.generated.resources.barlow_semi_condensed_bold
import com.schedule.qlit.composeapp.generated.resources.barlow_semi_condensed_medium
import com.schedule.qlit.composeapp.generated.resources.barlow_semi_condensed_semibold
import org.jetbrains.compose.resources.Font

/**
 * Numbers (times, rooms, countdowns, dates) use a narrow grotesque so they read
 * crisply next to the system CJK face; Chinese text stays on the system font.
 *
 * composeResources 的 Font() 只能在组合中调用，真实字族由 [QlitTheme] 在
 * 组合期间写入 [resolvedNumFontFamily]——主题先于一切内容组合，读取方
 * （包括非 composable 的 AnnotatedString 助手）总能拿到已解析的字族。
 */
internal var resolvedNumFontFamily: FontFamily = FontFamily.Default

val NumFontFamily: FontFamily get() = resolvedNumFontFamily

/** 由 [QlitTheme] 组合时调用；Font() 是 composable，必须在这里解析。 */
@Composable
internal fun resolveNumFontFamily() {
    resolvedNumFontFamily = FontFamily(
        Font(Res.font.barlow_semi_condensed_medium, FontWeight.Medium),
        Font(Res.font.barlow_semi_condensed_semibold, FontWeight.SemiBold),
        Font(Res.font.barlow_semi_condensed_bold, FontWeight.Bold),
    )
}

private val TrimmedLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

/** Type ramp of the redesign. Sizes follow the prototype 1:1 (px → sp). */
object QType {
    val display = TextStyle(fontSize = 29.sp, lineHeight = 1.15.em, fontWeight = FontWeight.Bold)
    val pageTitle = TextStyle(fontSize = 28.sp, lineHeight = 1.2.em, fontWeight = FontWeight.Bold)
    val sheetTitle = TextStyle(fontSize = 24.sp, lineHeight = 1.25.em, fontWeight = FontWeight.Bold)
    val title = TextStyle(fontSize = 19.sp, lineHeight = 1.3.em, fontWeight = FontWeight.SemiBold)
    val section = TextStyle(fontSize = 17.sp, lineHeight = 1.35.em, fontWeight = FontWeight.SemiBold)
    val rowTitle = TextStyle(fontSize = 16.sp, lineHeight = 1.35.em, fontWeight = FontWeight.Medium)
    val bodyStrong = TextStyle(fontSize = 16.sp, lineHeight = 1.35.em, fontWeight = FontWeight.SemiBold)
    val body = TextStyle(fontSize = 15.sp, lineHeight = 1.5.em)
    val sub = TextStyle(fontSize = 13.sp, lineHeight = 1.45.em)
    val label = TextStyle(fontSize = 13.sp, lineHeight = 1.3.em, fontWeight = FontWeight.Medium)
    val caption = TextStyle(fontSize = 12.sp, lineHeight = 1.35.em)
    val tiny = TextStyle(fontSize = 11.sp, lineHeight = 1.3.em)

    fun num(size: TextUnit, weight: FontWeight = FontWeight.SemiBold): TextStyle = TextStyle(
        fontFamily = NumFontFamily,
        fontSize = size,
        fontWeight = weight,
        lineHeight = 1.1.em,
        fontFeatureSettings = "tnum",
        lineHeightStyle = TrimmedLineHeight,
    )
}
