package com.schedule.qlit.system
import com.schedule.qlit.IUserService
import androidx.core.net.toUri

import android.content.Context
import android.net.Uri
import android.util.Log

class ShizukuUserService : IUserService.Stub() {

    companion object {
        private const val TAG = "ShizukuUserService"
        private val ISLAND_URI =
            "content://miui.statusbar.notification.public".toUri()
    }

    // 通过反射取得系统 Context
    private val context: Context? by lazy {
        runCatching {
            val clazz = Class.forName("android.app.ActivityThread")
            val thread = clazz.getMethod("systemMain").invoke(null)
            clazz.getMethod("getSystemContext").invoke(thread) as Context
        }.onFailure {
            Log.e(TAG, "获取 SystemContext 失败: ${it.message}")
        }.getOrNull()
    }

    override fun destroy() {}

    override fun exit() {
        System.exit(0)
    }

    override fun ping(): String {
        Log.i(TAG, "ping() called, running as uid=${android.os.Process.myUid()}")
        return "pong from ShizukuUserService v9 uid=${android.os.Process.myUid()}"
    }

    override fun setUidFirewallRule(uid: Int, block: Boolean): Boolean {
        return runCatching {
            // 1. 拿 connectivity 的原始 Binder(ServiceManager 是隐藏 API,走反射)
            val cmBinder = Class.forName("android.os.ServiceManager")
                .getMethod("getService", String::class.java)
                .invoke(null, "connectivity") as? android.os.IBinder
                ?: error("connectivity service 不存在")

            // 2. 转成 IConnectivityManager 接口
            val stubClass = Class.forName("android.net.IConnectivityManager\$Stub")
            val cm = stubClass.getMethod("asInterface", android.os.IBinder::class.java)
                .invoke(null, cmBinder)

            val chain = 9   // FIREWALL_CHAIN_OEM_DENY_3，黑名单模式
            val rule  = if (block) 2 else 0  // 2=DENY, 0=DEFAULT(放行)

            if (block) {
                // 先确保 chain 开启
                cm.javaClass.getMethod("setFirewallChainEnabled", Int::class.java, Boolean::class.java)
                    .invoke(cm, chain, true)
                // 再把 uid 加入黑名单
                cm.javaClass.getMethod("setUidFirewallRule", Int::class.java, Int::class.java, Int::class.java)
                    .invoke(cm, chain, uid, rule)
            } else {
                // 移出黑名单（不关闭整条 chain，避免影响其他 uid）
                cm.javaClass.getMethod("setUidFirewallRule", Int::class.java, Int::class.java, Int::class.java)
                    .invoke(cm, chain, uid, rule)
            }

            Log.i(TAG, "setUidFirewallRule uid=$uid block=$block 成功")
            true

        }.onFailure {
            Log.e(TAG, "setUidFirewallRule 失败: ${it.javaClass.simpleName} ${it.message}")
        }.getOrDefault(false)
    }

    /** execShell 内部实现，带详细日志 */
    private fun execShellInternal(command: String): Boolean {
        return runCatching {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            val stdout = process.inputStream.bufferedReader().readText()
            val stderr = process.errorStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            if (stdout.isNotEmpty()) Log.d(TAG, "stdout: $stdout")
            if (stderr.isNotEmpty()) Log.w(TAG, "stderr: $stderr")
            Log.d(TAG, "exitCode=$exitCode cmd=$command")
            exitCode == 0
        }.onFailure {
            Log.e(TAG, "execShellInternal 异常: ${it.message}")
        }.getOrDefault(false)
    }

    // execShell AIDL 方法复用内部实现
    override fun execShell(command: String): Boolean = execShellInternal(command)
}
