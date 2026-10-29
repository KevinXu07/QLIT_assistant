package com.schedule.qlit.ui.components

import com.schedule.qlit.IosAppHost
import platform.UIKit.UIColor
import platform.UIKit.UIImage
import platform.UIKit.UITabBarController
import platform.UIKit.UITabBarControllerDelegateProtocol
import platform.UIKit.UITabBarItem
import platform.UIKit.UIViewController
import platform.UIKit.tabBarItem
import platform.darwin.NSObject

internal actual val platformUsesNativeBarGlass: Boolean = true

internal actual fun platformNotifyTabChanged(index: Int) = NativeTabBridge.notifyTab(index)

/**
 * Compose ↔ SwiftUI 选中桥。
 * SwiftUI 侧 TabView host 设 [listener] 接收应用内选页变化；
 * 原生 tab 点击经 [onNativeSelect] 写回共享 [IosAppHost.state]。
 */
object NativeTabBridge {
    var listener: ((Int) -> Unit)? = null

    fun notifyTab(index: Int) {
        listener?.invoke(index)
    }

    fun onNativeSelect(index: Int) {
        IosAppHost.state.selectedTab = index
    }

    fun currentIndex(): Int = IosAppHost.state.selectedTab

    internal var retainedDelegate: NSObject? = null
}

private class TabDelegate : NSObject(), UITabBarControllerDelegateProtocol {
    override fun tabBarController(
        tabBarController: UITabBarController,
        didSelectViewController: UIViewController,
    ) {
        val index = tabBarController.viewControllers
            ?.indexOf(didSelectViewController)
            ?.takeIf { it >= 0 }
            ?: return
        NativeTabBridge.onNativeSelect(index)
    }
}

// MainDestination: TODAY=0 / COURSES=1 / TOOLS=2
private val tabEntries = listOf(
    "今天" to "house",
    "课表" to "calendar",
    "工具" to "square.grid.2x2",
)

private val sealRed: UIColor
    get() = UIColor.colorWithRed(0xC8 / 255.0, 0x16 / 255.0, 0x1D / 255.0, 1.0)

/**
 * 系统 UITabBarController：SwiftUI 层作为 overlay 叠在 Compose 画布上，
 * iOS 26 液态玻璃材质、液态选中动画、按压物理反馈全部由系统提供。
 * 子控制器仅借 tabBar，页面内容由 Compose pager 渲染。
 */
fun NativeTabBarViewController(): UITabBarController {
    val delegate = TabDelegate()
    val controller = UITabBarController()
    controller.delegate = delegate
    NativeTabBridge.retainedDelegate = delegate // delegate 被弱持有，Kotlin 侧留强引用
    controller.view.backgroundColor = UIColor.clearColor
    controller.viewControllers = tabEntries.mapIndexed { index, (title, symbol) ->
        UIViewController().apply {
            view.backgroundColor = UIColor.clearColor
            view.userInteractionEnabled = false
            tabBarItem = UITabBarItem().apply {
                this.title = title
                image = UIImage.systemImageNamed(symbol)
                tag = index.toLong()
            }
        }
    }
    controller.tabBar.tintColor = sealRed
    controller.selectedIndex = IosAppHost.state.selectedTab.toULong()
    return controller
}
