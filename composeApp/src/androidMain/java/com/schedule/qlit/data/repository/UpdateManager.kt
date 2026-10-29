package com.schedule.qlit.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.gson.Gson
import androidx.core.net.toUri
import com.schedule.qlit.data.model.UpdateInfo
import com.schedule.qlit.data.net.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object UpdateManager {
    private const val UPDATE_URL = "https://api.kevinxu.cn/update.json"
    private const val TAG = "UpdateManager"
    private const val PREFS_NAME = "ui_preferences"
    private const val KEY_ENABLED = "update_check_enabled"

    fun isUpdateEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, true)

    fun setUpdateEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    suspend fun checkUpdate(context: Context): UpdateInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val resp = Http.get(
                    UPDATE_URL,
                    headers = mapOf("User-Agent" to "Mozilla/5.0 (Linux; Android 10)"),
                    timeoutMs = 8_000,
                )
                if (resp.code != 200) return@withContext null

                val json = resp.text()
                // 关键点：明确指定 Gson 解析的类型
                val updateInfo = Gson().fromJson(json, UpdateInfo::class.java)

                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                val currentVersion = packageInfo.longVersionCode

                if (updateInfo.versionCode > currentVersion) updateInfo else null
            } catch (e: Exception) {
                Log.e(TAG, "检查失败: ${e.message}")
                null
            }
        }
    }

    suspend fun downloadAndInstall(
        context: Context,
        apkUrl: String,
        onProgress: (Float) -> Unit,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                val file = File(context.externalCacheDir, "update.apk")
                val connection = java.net.URL(apkUrl).openConnection() as java.net.HttpURLConnection
                val totalSize = connection.contentLength

                connection.inputStream.use { input ->
                    file.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var downloaded = 0
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            withContext(Dispatchers.Main) {
                                onProgress(downloaded.toFloat() / totalSize)
                            }
                        }
                    }
                }
                withContext(Dispatchers.Main) {
                    onComplete()
                    installApk(context, file)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError(e.message ?: "下载失败") }
            }
        }
    }

    private fun installApk(context: Context, file: File) {
        if (!file.exists()) {
            Log.e(TAG, "安装失败：文件不存在")
            return
        }

        // 1. 处理“安装未知应用”权限
        if (!context.packageManager.canRequestPackageInstalls()) {
            // 引导用户去设置页面开启权限
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = "package:${context.packageName}".toUri()
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            // 提示用户：由于系统限制，开启权限后请重新点击安装
            Toast.makeText(context, "请开启安装权限后重试", Toast.LENGTH_LONG).show()
            return
        }

        // 2. 正常拉起安装程序
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // 必须：授权给系统安装器读取 APK
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)        // 必须：从非 Activity 上下文启动
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "拉起安装程序失败: ${e.message}")
            e.printStackTrace()
            // 如果报错，检查 authority 是否拼写错误
        }
    }
}