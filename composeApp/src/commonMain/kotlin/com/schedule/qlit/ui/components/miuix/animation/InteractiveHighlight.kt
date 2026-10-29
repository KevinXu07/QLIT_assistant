package com.schedule.qlit.ui.components.miuix.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.schedule.qlit.ui.components.miuix.modifier.inspectDragGestures

/**
 * 按压辉光：手势/位移动画是共享逻辑，发光绘制走平台
 * [glowModifier]——Android 用 AGSL smoothstep 径向衰减，
 * iOS 用 radialGradient 近似。
 */
class InteractiveHighlight(
    val animationScope: CoroutineScope,
    val position: (size: Size, offset: Offset) -> Offset = { _, offset -> offset }
) {

    internal val pressProgressAnimationSpec =
        spring(0.5f, 300f, 0.001f)
    internal val positionAnimationSpec =
        spring(0.5f, 300f, Offset.VisibilityThreshold)

    internal val pressProgressAnimation =
        Animatable(0f, 0.001f)
    internal val positionAnimation =
        Animatable(Offset.Zero, Offset.VectorConverter, Offset.VisibilityThreshold)

    internal var startPosition = Offset.Zero
    val offset: Offset get() = positionAnimation.value - startPosition

    val modifier: Modifier get() = glowModifier()

    val gestureModifier: Modifier =
        Modifier.pointerInput(animationScope) {
            inspectDragGestures(
                onDragStart = { down ->
                    startPosition = down.position
                    animationScope.launch {
                        launch { pressProgressAnimation.animateTo(1f, pressProgressAnimationSpec) }
                        launch { positionAnimation.snapTo(startPosition) }
                    }
                },
                onDragEnd = {
                    animationScope.launch {
                        launch { pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec) }
                        launch { positionAnimation.animateTo(startPosition, positionAnimationSpec) }
                    }
                },
                onDragCancel = {
                    animationScope.launch {
                        launch { pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec) }
                        launch { positionAnimation.animateTo(startPosition, positionAnimationSpec) }
                    }
                }
            ) { change, _ ->
                animationScope.launch { positionAnimation.snapTo(change.position) }
            }
        }
}

/** 发光层绘制修饰符；实现里可以缓存 shader 实例按帧更新 uniform。 */
internal expect fun InteractiveHighlight.glowModifier(): Modifier
