package com.schedule.qlit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import com.schedule.qlit.ui.theme.Q
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavKey

@Composable
fun ToolsScreen(
    requestedDestinationName: String? = null,
    onRequestedDestinationHandled: (() -> Unit)? = null
) {
    ToolsMiuixNavHost(
        requestedDestinationName = requestedDestinationName,
        onRequestedDestinationHandled = onRequestedDestinationHandled,
    )
}

/** 工具页专用转场：进场页全宽滑入，覆盖层原地淡出（无视差）；
 * dismissDirection 保持 None——全屏左滑返回太容易误触
 *（地图拖动/列表横向滚动都会被劫持），返回交给系统边缘手势。 */
private val tabSafeNavTransition: top.yukonga.miuix.kmp.nav.transition.NavTransition =
    top.yukonga.miuix.kmp.nav.transition.navGraphicsTransition(
        opaqueDepth = 1f,
    ) { scope ->
        val width = scope.layoutSize.width.toFloat()
        val d = scope.relativeDepth
        if (d <= 0f) {
            translationX = -d.coerceIn(-1f, 0f) * width
        } else {
            alpha = 1f - 0.35f * d.coerceIn(0f, 1f)
        }
    }

private sealed interface ToolsNavDestination : NavKey {
    data object LIST : ToolsNavDestination
    data object TAOBAO_PICKUP : ToolsNavDestination
    data object CHAOXING : ToolsNavDestination

    companion object {
        fun fromName(name: String): ToolsNavDestination? = when (ToolsDestination.fromName(name)) {
            ToolsDestination.LIST -> LIST
            ToolsDestination.TAOBAO_PICKUP -> TAOBAO_PICKUP
            ToolsDestination.CHAOXING -> CHAOXING
            else -> null
        }
    }
}

private val ToolsNavBackStackSaver = listSaver<SnapshotStateList<NavKey>, String>(
    save = { backStack ->
        backStack.map { destination ->
            when (destination) {
                ToolsNavDestination.LIST -> ToolsDestination.LIST.name
                ToolsNavDestination.TAOBAO_PICKUP -> ToolsDestination.TAOBAO_PICKUP.name
                ToolsNavDestination.CHAOXING -> ToolsDestination.CHAOXING.name
                else -> ToolsDestination.LIST.name
            }
        }
    },
    restore = { destinationNames ->
        mutableStateListOf<NavKey>().apply {
            destinationNames.mapNotNullTo(this) { ToolsNavDestination.fromName(it) }
            if (firstOrNull() != ToolsNavDestination.LIST) {
                add(0, ToolsNavDestination.LIST)
            }
        }
    },
)

@Composable
private fun ToolsMiuixNavHost(
    requestedDestinationName: String?,
    onRequestedDestinationHandled: (() -> Unit)?,
) {
    val backStack = rememberSaveable(saver = ToolsNavBackStackSaver) {
        mutableStateListOf<NavKey>(ToolsNavDestination.LIST)
    }

    // 返回统一交给 NavDisplay 内置的 NavigationBackHandler（navigationevent 管线，
    // 自带预测性返回动画）。不要在此叠加旧的 androidx.activity.compose.BackHandler：
    // 两套返回管线并存时，HyperOS 边缘手势会漏掉路由回退、直接退出应用。

    LaunchedEffect(requestedDestinationName) {
        val requested = requestedDestinationName ?: return@LaunchedEffect
        val destination = ToolsNavDestination.fromName(requested) ?: return@LaunchedEffect
        if (backStack.lastOrNull() != destination) {
            backStack.clear()
            backStack += ToolsNavDestination.LIST
            if (destination != ToolsNavDestination.LIST) {
                backStack += destination
            }
        }
        onRequestedDestinationHandled?.invoke()
    }

    // miuix-nav 的堆栈转场会让被覆盖的页面以缩放态露出边缘；不指定
    // backdropColor 时露出区域是透明的，切 tab 时下层内容会透出来形成
    // “动画残留”。指定页面背景色后露出区域读作背景延伸（官方设计意图）。
    val navBackdropColor = Q.colors.bg
    NavDisplay(
        backStack = backStack,
        modifier = Modifier.fillMaxSize(),
        // 默认 MiuixDefault 转场给被覆盖层 1/4 屏宽的向左视差：从工具二级页
        // 切换 tab 时，视差中的旧页左移出 pager 页面边界，pager 的 tab 动画
        // 只覆盖原位区域，左移部分在相邻页上残留（右侧切入时被新页盖住，
        // 故仅左侧可见）。改用覆盖层原地淡出的自定义转场：保留进场页
        // 全宽滑入的 Miuix 手感，覆盖层不再侧移。
        transition = tabSafeNavTransition,
        effects = top.yukonga.miuix.kmp.nav.core.NavDisplayEffects(
            // 关闭进页圆角裁剪：库只有 Leading/All 两种模式，进页前缘的圆角
            // 会连底部一起裁，而页底顶着的是直角实占底栏，圆角处露出下层背景
            enableCornerClip = false,
            backdropColor = navBackdropColor,
        ),
        onBack = {
            if (backStack.size > 1) {
                backStack.removeLastOrNull()
            }
        },
    ) {
        entry<ToolsNavDestination.LIST> {
            ToolsListScreen(
                onNavigateToTaobaoPickup = { backStack += ToolsNavDestination.TAOBAO_PICKUP },
                onNavigateToChaoxing = { backStack += ToolsNavDestination.CHAOXING },
            )
        }
        entry<ToolsNavDestination.TAOBAO_PICKUP> {
            TaobaoPickupScreen(onBack = { backStack.removeLastOrNull() })
        }
        entry<ToolsNavDestination.CHAOXING> {
            ChaoxingScreen(onBack = { backStack.removeLastOrNull() })
        }
    }
}

// ── 工具列表 ────────────────────────────────────────────────

@Composable
private fun ToolsListScreen(
    onNavigateToTaobaoPickup: () -> Unit,
    onNavigateToChaoxing: () -> Unit,
) {
    ToolsMiuixScreen(
        onNavigateToTaobaoPickup = onNavigateToTaobaoPickup,
        onNavigateToChaoxing = onNavigateToChaoxing,
    )
}
