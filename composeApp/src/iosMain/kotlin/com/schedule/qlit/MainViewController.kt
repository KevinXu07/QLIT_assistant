package com.schedule.qlit

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import androidx.lifecycle.viewmodel.compose.viewModel
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UIKit.UIViewController
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNUserNotificationCenter

/**
 * iOS 壳持有应用状态：url-scheme / 小组件深链经这里驱动 tab 与工具页跳转，
 * Swift 侧通过 `IosAppHost.state` 访问。
 */
object IosAppHost {
    val state = AppRootState()
}

/** iOS 应用入口，与 Android MainActivity 共享同一个 [QlitApp] 组合树。 */
fun MainViewController(): UIViewController = ComposeUIViewController {
    // QA 钩子：QLIT_OPEN_TOOL=TAOBAO_PICKUP 等环境变量直达工具页，便于模拟器验证
    (NSProcessInfo.processInfo.environment["QLIT_OPEN_TOOL"] as? String)?.let { toolName ->
        IosAppHost.state.selectedTab = 2
        IosAppHost.state.pendingToolsDestination = toolName
    }
    QlitApp(
        state = IosAppHost.state,
        viewModel = viewModel { ScheduleViewModel() },
        onRequestNotificationPermission = {
            UNUserNotificationCenter.currentNotificationCenter()
                .requestAuthorizationWithOptions(
                    UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge,
                ) { _, _ -> }
        },
        onOpenNotificationSettings = {
            NSURL.URLWithString(UIApplicationOpenSettingsURLString)?.let {
                UIApplication.sharedApplication.openURL(it, mapOf<Any?, Any>(), null)
            }
        },
    )
}
