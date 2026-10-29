package com.schedule.qlit.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Lightness/chroma pair shared by every hue of one tint role. */
@Immutable
data class TintLc(val l: Float, val c: Float)

/** One hue rendered for every role it is used in. */
@Immutable
data class HueTint(
    /** Course block / chip background. */
    val container: Color,
    /** Course accent bar and timeline dot. */
    val accent: Color,
    /** Text drawn on [container]. */
    val content: Color,
    /** Tool icon tile background. */
    val iconContainer: Color,
    /** Tool icon glyph. */
    val icon: Color,
)

/**
 * The seal-red palette. Red is reserved for "right now" (next class, today,
 * the now-line, things waiting for the user); everything else is warm paper
 * and ink so the red stays loud.
 */
@Immutable
class QlitPalette(
    val isDark: Boolean,
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val ink: Color,
    val ink2: Color,
    val ink3: Color,
    val line: Color,
    val seal: Color,
    val sealSoft: Color,
    val glass: Color,
    val glassEdge: Color,
    val scrim: Color,
    private val containerLc: TintLc,
    private val accentLc: TintLc,
    private val contentLc: TintLc,
    private val iconContainerLc: TintLc,
    private val iconLc: TintLc,
) {
    val onSeal: Color = Color.White

    private val cache = HashMap<Int, HueTint>()

    fun hue(degrees: Float): HueTint {
        val key = (degrees * 10).toInt()
        return cache.getOrPut(key) {
            HueTint(
                container = oklch(containerLc.l, containerLc.c, degrees),
                accent = oklch(accentLc.l, accentLc.c, degrees),
                content = oklch(contentLc.l, contentLc.c, degrees),
                iconContainer = oklch(iconContainerLc.l, iconContainerLc.c, degrees),
                icon = oklch(iconLc.l, iconLc.c, degrees),
            )
        }
    }
}

val LightQlitPalette = QlitPalette(
    isDark = false,
    bg = Color(0xFFF3F1EC),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFEBE8E2),
    ink = Color(0xFF1D1B18),
    ink2 = Color(0xFF6B665E),
    ink3 = Color(0xFFA39D93),
    line = Color(0x141D1B18),
    seal = Color(0xFFC8161D),
    sealSoft = Color(0x1AC8161D),
    glass = Color(0x94FFFFFF),
    glassEdge = Color(0xD9FFFFFF),
    scrim = Color(0x5214100C),
    containerLc = TintLc(.935f, .042f),
    accentLc = TintLc(.64f, .13f),
    contentLc = TintLc(.36f, .085f),
    iconContainerLc = TintLc(.93f, .05f),
    iconLc = TintLc(.52f, .15f),
)

val DarkQlitPalette = QlitPalette(
    isDark = true,
    bg = Color(0xFF141312),
    surface = Color(0xFF1F1D1B),
    surface2 = Color(0xFF2B2825),
    ink = Color(0xFFF2EFEA),
    ink2 = Color(0xFFA8A29A),
    ink3 = Color(0xFF6E6962),
    line = Color(0x12FFFFFF),
    seal = Color(0xFFCC2A31),
    sealSoft = Color(0x29E6464C),
    glass = Color(0x942C2926),
    glassEdge = Color(0x1AFFFFFF),
    scrim = Color(0x80000000),
    containerLc = TintLc(.31f, .05f),
    accentLc = TintLc(.72f, .12f),
    contentLc = TintLc(.9f, .05f),
    iconContainerLc = TintLc(.3f, .055f),
    iconLc = TintLc(.8f, .12f),
)

val LocalQlitPalette = staticCompositionLocalOf { LightQlitPalette }

/** Short accessor for the active palette: `Q.colors.ink`. */
object Q {
    val colors: QlitPalette
        @Composable @ReadOnlyComposable
        get() = LocalQlitPalette.current
}

/** Fixed hues for tools and settings entries, chosen to stay clear of seal red. */
object QlitHues {
    const val PICKUP = 50f
    const val ELECTRICITY = 95f
    const val SHOWER = 220f
    const val CHAOXING = 255f
    const val CAMPUS_RUN = 150f
    const val NFC = 285f
    const val TRAVEL = 185f
    const val WALLET = 125f
    const val SETTINGS = 250f
    const val UPDATE = 150f
    const val FEEDBACK = 60f
    const val ABOUT = 320f
    const val NEUTRAL = 60f

    fun forTool(route: String): Float = when (route) {
        "TAOBAO_PICKUP" -> PICKUP
        "CHAOXING" -> CHAOXING
        else -> NEUTRAL
    }
}

/**
 * Assigns each distinct course name a hue. Hues walk the wheel by the golden
 * ratio so neighbours always land far apart, and skip the 0°–50° red–orange
 * band entirely.
 */
object CoursePalette {
    private const val START = 52f
    private const val SPAN = 308f
    private const val GOLDEN = 0.618034f

    fun hueForIndex(index: Int): Float {
        val fraction = (index * GOLDEN) % 1f
        return (START + fraction * SPAN) % 360f
    }

    fun assign(namesInOrder: List<String>): Map<String, Float> {
        val result = LinkedHashMap<String, Float>()
        namesInOrder.forEach { name ->
            if (name !in result) result[name] = hueForIndex(result.size)
        }
        return result
    }

    fun isSealBand(hue: Float): Boolean = hue < 50f
}
