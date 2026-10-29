package com.schedule.qlit.ui.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Line icons of the redesign: 24-unit grid, 1.8 stroke, round caps and joins.
 * They are tinted at draw time, so the stroke color here is irrelevant.
 */
object QIcons {
    private fun circle(cx: Float, cy: Float, r: Float) =
        "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0z"

    private fun rrect(x: Float, y: Float, w: Float, h: Float, r: Float) =
        "M${x + r} ${y}h${w - 2 * r}a$r $r 0 0 1 $r ${r}v${h - 2 * r}a$r $r 0 0 1 ${-r} ${r}" +
            "h${-(w - 2 * r)}a$r $r 0 0 1 ${-r} ${-r}v${-(h - 2 * r)}a$r $r 0 0 1 $r ${-r}z"

    private fun icon(name: String, vararg paths: String, stroke: Float = 1.8f): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            paths.forEach { data ->
                addPath(
                    pathData = PathParser().parsePathString(data).toNodes(),
                    fill = null,
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = stroke,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()

    val Today by lazy { icon("today", "M3.5 10.5 12 3.5l8.5 7V19a1.5 1.5 0 0 1-1.5 1.5h-4.5V15h-5v5.5H5A1.5 1.5 0 0 1 3.5 19z") }
    val Week by lazy {
        icon("week", rrect(3.5f, 4.5f, 17f, 16f, 3.5f), "M3.5 9.5h17M8.5 2.5v4M15.5 2.5v4M8 13.5h2M14 13.5h2M8 17h2")
    }
    val Tools by lazy {
        icon(
            "tools",
            rrect(3.5f, 3.5f, 7f, 7f, 2.2f), rrect(13.5f, 3.5f, 7f, 7f, 2.2f),
            rrect(3.5f, 13.5f, 7f, 7f, 2.2f), circle(17f, 17f, 3.6f),
        )
    }
    val ChevronRight by lazy { icon("chevron_right", "m9.5 6 6 6-6 6") }
    val ChevronLeft by lazy { icon("chevron_left", "m14.5 6-6 6 6 6") }
    val ChevronDown by lazy { icon("chevron_down", "m6 9.5 6 6 6-6") }
    val Back get() = ChevronLeft

    val Pickup by lazy { icon("pickup", "M3.5 7.5 12 3.5l8.5 4v9L12 20.5l-8.5-4z", "M3.5 7.5 12 11.5l8.5-4M12 11.5v9") }
    val Power by lazy { icon("power", "M13 2.5 5 13.5h6l-1 8 8-11h-6z") }
    val Shower by lazy {
        icon("shower", "M5 12.5a7 7 0 0 1 14 0z", "M12 5.5V3.5M8 16.5v1M12 16.5v1.5M16 16.5v1M10 20.5v.5M14 20.5v.5")
    }
    val Todo by lazy {
        icon(
            "todo",
            "M4.5 5A1.5 1.5 0 0 1 6 3.5h13.5v14H6a1.5 1.5 0 0 0-1.5 1.5z",
            "M4.5 19A1.5 1.5 0 0 0 6 20.5h13.5v-3",
            "m9 10.5 2 2 4-4",
        )
    }
    val Pin by lazy { icon("pin", "M12 21s-6.5-5.6-6.5-11a6.5 6.5 0 0 1 13 0c0 5.4-6.5 11-6.5 11z", circle(12f, 10f, 2.4f)) }
    val Nfc by lazy { icon("nfc", "M7.5 8a5.5 5.5 0 0 1 0 8M11 5.5a9 9 0 0 1 0 13M14.5 3a12.5 12.5 0 0 1 0 18") }
    val Luggage by lazy {
        icon(
            "luggage",
            rrect(5f, 7.5f, 14f, 12f, 2.5f),
            "M9 7.5V5a1.5 1.5 0 0 1 1.5-1.5h3A1.5 1.5 0 0 1 15 5v2.5",
            "M9 11v5M15 11v5",
        )
    }
    val Wallet by lazy { icon("wallet", rrect(3.5f, 6f, 17f, 13f, 3f), "M3.5 10h17", "M15.5 14.5h1.5") }

    val Settings by lazy {
        icon(
            "settings",
            circle(12f, 12f, 3f),
            "M12 3v2.5M12 18.5V21M3 12h2.5M18.5 12H21M5.6 5.6l1.8 1.8M16.6 16.6l1.8 1.8M5.6 18.4l1.8-1.8M16.6 7.4l1.8-1.8",
        )
    }
    val Refresh by lazy {
        icon(
            "refresh",
            "M19.5 11a7.5 7.5 0 0 0-13.4-4.6L4.5 8.5",
            "M4.5 4v4.5H9",
            "M4.5 13a7.5 7.5 0 0 0 13.4 4.6l1.6-2.1",
            "M19.5 20v-4.5H15",
        )
    }
    val Feedback by lazy { icon("feedback", "M4.5 6A1.5 1.5 0 0 1 6 4.5h12A1.5 1.5 0 0 1 19.5 6v9a1.5 1.5 0 0 1-1.5 1.5H9.5l-5 4z") }
    val Info by lazy { icon("info", circle(12f, 12f, 8.5f), "M12 11v5.5M12 7.6v.2") }
    val Warning by lazy { icon("warning", "M12 4 2.8 19.5h18.4z", "M12 10v4.5M12 17v.2") }
    val More by lazy { icon("more", "M6 12h.01M12 12h.01M18 12h.01", stroke = 2.6f) }
    val Add by lazy { icon("add", "M12 5v14M5 12h14") }
    val Close by lazy { icon("close", "M6.5 6.5l11 11M17.5 6.5l-11 11") }
    val Check by lazy { icon("check", "m5 12.5 4.5 4.5L19 7.5") }
    val CheckCircle by lazy { icon("check_circle", circle(12f, 12f, 8.5f), "m8.3 12.2 2.6 2.6 4.9-5.2") }
    val Edit by lazy { icon("edit", "M14.5 5.5l4 4L8 20H4v-4z", "M12.5 7.5l4 4") }
    val Delete by lazy {
        icon("delete", "M4.5 7h15", "M9.5 7V4.5h5V7", "M6.5 7l1 13h9l1-13", "M10 11v5.5M14 11v5.5")
    }
    val Download by lazy { icon("download", "M12 4v11", "m7.5 10.5 4.5 4.5 4.5-4.5", "M5 19.5h14") }
    val Upload by lazy { icon("upload", "M12 15V4", "m7.5 8.5 4.5-4.5 4.5 4.5", "M5 19.5h14") }
    val Link by lazy {
        icon(
            "link",
            "M10 14a4 4 0 0 0 5.7 0l3-3a4 4 0 0 0-5.7-5.7l-1 1",
            "M14 10a4 4 0 0 0-5.7 0l-3 3a4 4 0 0 0 5.7 5.7l1-1",
        )
    }
    val File by lazy { icon("file", "M6.5 3.5h7l4 4v13h-11z", "M13.5 3.5v4h4") }
    val Cloud by lazy { icon("cloud", "M7 18.5a4.5 4.5 0 0 1-.6-9 6 6 0 0 1 11.4 1.6A3.8 3.8 0 0 1 17.5 18.5z") }
    val Search by lazy { icon("search", circle(11f, 11f, 6.5f), "m16 16 4 4") }
    val Eye by lazy {
        icon("eye", "M2.5 12s3.5-6.5 9.5-6.5 9.5 6.5 9.5 6.5-3.5 6.5-9.5 6.5S2.5 12 2.5 12z", circle(12f, 12f, 2.8f))
    }
    val EyeOff by lazy {
        icon(
            "eye_off",
            "M2.5 12s3.5-6.5 9.5-6.5 9.5 6.5 9.5 6.5-3.5 6.5-9.5 6.5S2.5 12 2.5 12z",
            circle(12f, 12f, 2.8f),
            "M4 4l16 16",
        )
    }
    val Lock by lazy { icon("lock", rrect(5f, 10.5f, 14f, 10f, 2.5f), "M8 10.5V8a4 4 0 0 1 8 0v2.5") }
    val Logout by lazy {
        icon(
            "logout",
            "M14.5 4.5h3a1.5 1.5 0 0 1 1.5 1.5v12a1.5 1.5 0 0 1-1.5 1.5h-3",
            "M10 16.5 5.5 12 10 7.5",
            "M5.5 12h9",
        )
    }
    val Clock by lazy { icon("clock", circle(12f, 12f, 8.5f), "M12 7.5V12l3 2") }
    val User by lazy { icon("user", circle(12f, 8.5f, 3.5f), "M5 20a7 7 0 0 1 14 0") }
    val Play by lazy { icon("play", "M8 5.5v13l10-6.5z") }
    val Stop by lazy { icon("stop", rrect(6.5f, 6.5f, 11f, 11f, 2f)) }
    val Map by lazy { icon("map", "M3.5 6.5 9 4.5l6 2 5.5-2v13l-5.5 2-6-2-5.5 2z", "M9 4.5v13M15 6.5v13") }
    val Copy by lazy {
        icon(
            "copy",
            rrect(8.5f, 8.5f, 11f, 11f, 2.5f),
            "M15.5 8.5V6A1.5 1.5 0 0 0 14 4.5H6A1.5 1.5 0 0 0 4.5 6v8A1.5 1.5 0 0 0 6 15.5h2.5",
        )
    }
    val External by lazy {
        icon(
            "external",
            "M14 4.5h5.5V10",
            "M19.5 4.5 11 13",
            "M18 14v4.5a1.5 1.5 0 0 1-1.5 1.5h-11A1.5 1.5 0 0 1 4 18.5v-11A1.5 1.5 0 0 1 5.5 6H10",
        )
    }
    val Moon by lazy { icon("moon", "M19.5 14.5A8 8 0 0 1 9.5 4.5a8 8 0 1 0 10 10z") }
    val Bell by lazy { icon("bell", "M6 16.5V11a6 6 0 0 1 12 0v5.5l1.5 2h-15z", "M10 20.5h4") }
    val Phone by lazy { icon("phone", rrect(7f, 3f, 10f, 18f, 2.5f), "M11 17.5h2") }
    val Swap by lazy { icon("swap", "M7 7.5h11l-3-3", "M17 16.5H6l3 3") }
    val History by lazy {
        icon("history", "M4.5 12a7.5 7.5 0 1 0 2.2-5.3L4.5 9", "M4.5 4.5V9H9", "M12 8v4l3 2")
    }
    val Shield by lazy { icon("shield", "M12 3.5 5 6.5v5c0 4.5 3 7.6 7 9 4-1.4 7-4.5 7-9v-5z") }
    val Qr by lazy {
        icon(
            "qr",
            rrect(4f, 4f, 6f, 6f, 1.2f), rrect(14f, 4f, 6f, 6f, 1.2f), rrect(4f, 14f, 6f, 6f, 1.2f),
            "M14 14h2.5v2.5H14zM17.5 17.5H20V20h-2.5z",
        )
    }
    val Route by lazy {
        icon("route", circle(6f, 18f, 2.2f), circle(18f, 6f, 2.2f), "M8 18h7.5a3 3 0 0 0 0-6h-7a3 3 0 0 1 0-6H16")
    }

    /** Icon for a tool route name (see `ToolsDestination`). */
    fun forTool(route: String): ImageVector = when (route) {
        "TAOBAO_PICKUP" -> Pickup
        "CHAOXING" -> Todo
        else -> Info
    }
}
