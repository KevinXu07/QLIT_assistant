package com.schedule.qlit.system
import com.schedule.qlit.IUserService

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import rikka.shizuku.Shizuku

object ShizukuManager {

    private const val TAG = "ShizukuManager"
    private const val REQUEST_CODE = 100

    @Volatile
    var userService: IUserService? = null
        private set

    private var isBound = false
    private var isBinding = false
    private var savedConnection: ServiceConnection? = null
    private var permissionListener: Shizuku.OnRequestPermissionResultListener? = null

    // Service 配置
    private val serviceArgs = Shizuku.UserServiceArgs(
        ComponentName("com.schedule.qlit", ShizukuUserService::class.java.name)
    )
        .daemon(false)
        .processNameSuffix("user_service")
        .debuggable(false)
        .version(22)

    // ── 权限相关 ────────────────────────────────────────────

    val isShizukuAvailable: Boolean
        get() = runCatching { Shizuku.pingBinder() }.getOrDefault(false)

    val hasPermission: Boolean
        get() = runCatching {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)

    fun requestPermission() {
        runCatching {
            if (Shizuku.isPreV11()) {
                Log.w(TAG, "Shizuku 版本过低，不支持运行时权限申请")
                return
            }
            Shizuku.requestPermission(REQUEST_CODE)
        }
    }

    // 注册权限回调（在 Activity onCreate 里调用）
    fun registerPermissionListener(onResult: (Boolean) -> Unit) {
        unregisterPermissionListener()
        val listener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == REQUEST_CODE) {
                onResult(grantResult == PackageManager.PERMISSION_GRANTED)
            }
        }
        permissionListener = listener
        runCatching { Shizuku.addRequestPermissionResultListener(listener) }
    }

    fun unregisterPermissionListener() {
        permissionListener?.let { listener ->
            runCatching { Shizuku.removeRequestPermissionResultListener(listener) }
        }
        permissionListener = null
    }

    // ── Service 绑定 ────────────────────────────────────────

    fun bindUserService(
        onConnected: (IUserService) -> Unit,
        onDisconnected: () -> Unit = {}
    ) {
        if (!isShizukuAvailable || !hasPermission) {
            Log.w(TAG, "Shizuku 不可用或没有权限")
            return
        }
        userService?.let {
            onConnected(it)
            return
        }
        if (isBound || isBinding) return
        isBinding = true

        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                if (savedConnection !== this) {
                    Log.d(TAG, "忽略过期 UserService 连接回调")
                    return
                }

                val service = IUserService.Stub.asInterface(binder)
                if (service == null) {
                    Log.w(TAG, "UserService 返回空 Binder")
                    isBound = false
                    isBinding = false
                    savedConnection = null
                    onDisconnected()
                    return
                }

                userService = service
                isBound = true
                isBinding = false

                // 立即 ping，确认是新版本
                val pong = runCatching { service.ping() }.getOrNull()
                Log.i(TAG, "UserService ping 结果: $pong")

                Log.i(TAG, "UserService 已连接")
                onConnected(service)
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                if (savedConnection !== this) {
                    Log.d(TAG, "忽略过期 UserService 断开回调")
                    return
                }

                userService = null
                isBound = false
                isBinding = false
                savedConnection = null
                Log.w(TAG, "UserService 已断开")
                onDisconnected()
            }
        }

        savedConnection = connection

        runCatching {
            Shizuku.bindUserService(serviceArgs, connection)
        }.onFailure {
            Log.e(TAG, "绑定 UserService 失败：${it.message}")
            if (savedConnection === connection) {
                isBinding = false
                savedConnection = null
            }
        }
    }

    fun unbindUserService() {
        val connection = savedConnection
        val shouldUnbind = isBound || isBinding

        // 先使当前代连接失效，避免异步断开回调覆盖紧随其后的新绑定。
        savedConnection = null
        userService = null
        isBound = false
        isBinding = false

        runCatching {
            if (shouldUnbind && connection != null) {
                Shizuku.unbindUserService(serviceArgs, connection, true)
            }
        }
    }
}
