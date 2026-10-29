package com.schedule.qlit.system

import android.os.Build

object DeviceUtils {

    /** 是否是小米 / Redmi / POCO 设备 */
    val isXiaomi: Boolean
        get() = Build.MANUFACTURER.lowercase() == "xiaomi"

    /** 是否运行 HyperOS（小米灵动岛需要 HyperOS） */
    val isHyperOS: Boolean
        get() = isXiaomi && runCatching {
            Class.forName("android.os.SystemProperties")
                .getMethod("get", String::class.java, String::class.java)
                .invoke(null, "ro.mi.os.version.name", "") as String
        }.getOrDefault("").isNotEmpty()

    /** 是否支持小米灵动岛 */
    val supportsXiaomiIsland: Boolean
        get() = isHyperOS

    private fun systemProp(key: String): String = runCatching {
        Class.forName("android.os.SystemProperties")
            .getMethod("get", String::class.java, String::class.java)
            .invoke(null, key, "") as String
    }.getOrDefault("")

    /** ROM 显示名（HyperOS / ColorOS / 其他→Android），用于匿名统计 */
    val romName: String
        get() = when {
            isHyperOS -> "HyperOS"
            systemProp("ro.build.version.oplusrom").isNotEmpty() -> "ColorOS"
            else -> "Android"
        }

    /** ROM 版本号（HyperOS 如 3.0.3.0；ColorOS 如 15；其他→空） */
    val romVersion: String
        get() = when {
            isHyperOS -> systemProp("ro.mi.os.version.name")
            else -> systemProp("ro.build.version.oplusrom")
        }

    /**
     * 课程实时推送（Live Updates / 灵动通知）是否受支持：
     * Android 15+ 且 ROM 为 HyperOS 3.0.300+ 或 ColorOS 16+。
     *
     * HyperOS 完整版本号在 ro.mi.os.version.incremental（如
     * "OS3.0.307.0.WKWCNXM"，前三段 3.0.307 与 3.0.300 阈值对应）；
     * ro.mi.os.version.name 只有两段（"OS3.0"）且带 OS 前缀，作回退。
     * ColorOS 的 oplusrom 形如 "15.0.1"，按主版本 16 判定。
     */
    val supportsLivePush: Boolean
        get() {
            if (Build.VERSION.SDK_INT < 35) return false

            fun numericSegments(raw: String): List<Int> =
                Regex("\\d+").findAll(raw).mapNotNull { it.value.toIntOrNull() }.toList()

            fun versionAtLeast(raw: String, threshold: List<Int>): Boolean {
                val segments = numericSegments(raw)
                if (segments.isEmpty()) return false
                for (i in threshold.indices) {
                    val actual = segments.getOrElse(i) { 0 }
                    val want = threshold[i]
                    if (actual != want) return actual > want
                }
                return true
            }

            return when (romName) {
                "HyperOS" -> versionAtLeast(
                    systemProp("ro.mi.os.version.incremental").ifBlank {
                        systemProp("ro.mi.os.version.name")
                    },
                    listOf(3, 0, 300),
                )
                "ColorOS" -> versionAtLeast(
                    systemProp("ro.build.version.oplusrom"),
                    listOf(16),
                )
                else -> false
            }
        }

    /**
     * 液态玻璃模糊是否可用。
     *
     * miuix-blur 0.9.4 构件按 minSdk 33 发布（内部依赖 API 33 的
     * RuntimeShader/AGSL），而 miuix 自带的 isRenderEffectSupported 只校验
     * RenderEffect（API 31）。Android 12/12L 上必须整体走无模糊降级路径。
     */
    val supportsLiquidBlur: Boolean
        get() = Build.VERSION.SDK_INT >= 33
}
