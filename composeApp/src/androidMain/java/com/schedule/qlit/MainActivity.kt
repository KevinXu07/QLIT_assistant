package com.schedule.qlit

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.net.toUri
import com.schedule.qlit.system.DeviceUtils
import com.schedule.qlit.system.ShizukuManager
import com.schedule.qlit.ui.screens.ToolsDestination

/**
 * Android 宿主：平台初始化（Shizuku/权限/深链）在这里，
 * 界面本体是 commonMain 的 [QlitApp]。
 */
class MainActivity : ComponentActivity() {

    private val viewModel: ScheduleViewModel by viewModels()
    private val appState = AppRootState()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        android.util.Log.d("MainActivity", "通知权限: $isGranted")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleNavigationIntent(intent)

        // Shizuku 为小米灵动岛等系统级通知能力提供 UserService；未运行时降级为普通通知
        initShizuku(requestPermissionOnLaunch = DeviceUtils.supportsXiaomiIsland)

        setContent {
            QlitApp(
                state = appState,
                viewModel = viewModel,
                onRequestNotificationPermission = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                onOpenNotificationSettings = { openNotificationSettings() },
            )
        }
    }

    private fun openNotificationSettings() {
        val intents = listOf(
            Intent("android.settings.MANAGE_APP_PROMOTED_NOTIFICATIONS").apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            },
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            },
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = "package:$packageName".toUri()
            }
        )

        val launched = intents.any { intent ->
            val resolvable = intent.resolveActivity(packageManager) != null
            if (resolvable) {
                startActivity(intent)
                true
            } else {
                false
            }
        }

        if (!launched) {
            android.widget.Toast.makeText(this, "当前设备不支持打开通知设置页", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun initShizuku(requestPermissionOnLaunch: Boolean) {
        ShizukuManager.registerPermissionListener { granted ->
            if (granted) {
                android.util.Log.d("MainActivity", "Shizuku 权限已获取，绑定 UserService")
                bindShizukuService()
            }
        }
        when {
            !ShizukuManager.isShizukuAvailable ->
                android.util.Log.w("MainActivity", "Shizuku 未运行，降级为普通通知")
            ShizukuManager.hasPermission -> bindShizukuService()
            requestPermissionOnLaunch -> ShizukuManager.requestPermission()
            else -> android.util.Log.d("MainActivity", "等待功能页按需请求 Shizuku 权限")
        }
    }

    private fun bindShizukuService() {
        ShizukuManager.bindUserService(
            onConnected = { android.util.Log.i("MainActivity", "UserService 就绪，可发送系统级通知") },
            onDisconnected = { android.util.Log.w("MainActivity", "UserService 断开") }
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNavigationIntent(intent)
    }

    private fun handleNavigationIntent(intent: Intent?) {
        when (intent?.getStringExtra("navigate_to")) {
            "today" -> appState.selectedTab = 0
            "tools" -> {
                appState.selectedTab = 2
                intent.getStringExtra("tool_destination")
                    ?.takeIf { it in TOOL_DESTINATIONS }
                    ?.let { appState.pendingToolsDestination = it }
            }
        }
    }

    override fun onDestroy() {
        ShizukuManager.unregisterPermissionListener()
        if (!isChangingConfigurations) {
            ShizukuManager.unbindUserService()
        }
        super.onDestroy()
    }

    private companion object {
        /** 小组件深链允许直达的工具页（与 ToolsDestination 枚举同名） */
        val TOOL_DESTINATIONS = ToolsDestination.deepLinkNames
    }
}
