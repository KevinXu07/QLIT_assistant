package com.schedule.qlit.widget

import android.content.Context
import org.json.JSONObject
import java.io.File

/**
 * 小组件的数据兜底缓存（JSON 文件存储）。
 *
 * 小米小部件规范要求 Widget 跑在独立的 ":widgetProvider" 进程；
 * SharedPreferences 不做跨进程刷新，另一进程内的实例会读到过期内存值。
 * 这里统一用文件存，读侧每次打开文件，天然跨进程一致——当前虽跑在
 * 主进程，但保持文件存储让将来恢复小米小部件进程隔离时无需改动。
 *
 * 写入方在各数据产生处：超星待办、淘宝取件拿到新数据时落盘；
 * 读取方在 Widget 刷新路径：优先网络、失败回退本文件。
 */
object QlitWidgetCache {

    private const val FILE_NAME = "widget_cache.json"
    private val fileLock = Any()

    // ── 写入（主进程调用）──────────────────────────────────

    fun saveChaoxing(context: Context, pendingWork: Int, pendingExam: Int) = write(context) {
        put("cx_work", pendingWork)
        put("cx_exam", pendingExam)
        put("cx_state", CX_STATE_OK)
        put("cx_at", System.currentTimeMillis())
    }

    fun saveChaoxingExpired(context: Context) = write(context) {
        put("cx_state", CX_STATE_EXPIRED)
        put("cx_at", System.currentTimeMillis())
    }

    fun saveChaoxingLoggedOut(context: Context) = write(context) {
        put("cx_state", CX_STATE_LOGGED_OUT)
        put("cx_at", System.currentTimeMillis())
    }

    fun saveTaobao(context: Context, loggedIn: Boolean, pendingCount: Int) = write(context) {
        put("tb_logged_in", loggedIn)
        put("tb_pending", pendingCount)
        put("tb_at", System.currentTimeMillis())
    }

    fun markTriggered(context: Context) = write(context) {
        put("last_trigger", System.currentTimeMillis())
    }

    // ── 读取（Widget 进程调用）─────────────────────────────

    const val CX_STATE_OK = 0
    const val CX_STATE_EXPIRED = 1
    const val CX_STATE_LOGGED_OUT = 2
    const val CX_STATE_UNKNOWN = -1

    /** @return Triple(待完成作业数, 待办考试数, 状态) */
    fun loadChaoxing(context: Context): Triple<Int, Int, Int> {
        val json = read(context) ?: return Triple(0, 0, CX_STATE_UNKNOWN)
        return Triple(
            json.optInt("cx_work", 0),
            json.optInt("cx_exam", 0),
            json.optInt("cx_state", CX_STATE_UNKNOWN),
        )
    }

    /** @return Pair(已登录, 待取件数)；文件没写过返回 null 让调用方回退 SharedPreferences。 */
    fun loadTaobao(context: Context): Pair<Boolean, Int>? {
        val json = read(context) ?: return null
        if (!json.has("tb_logged_in")) return null
        return json.optBoolean("tb_logged_in") to json.optInt("tb_pending", -1)
    }

    fun lastTriggerAt(context: Context): Long =
        read(context)?.optLong("last_trigger", 0L) ?: 0L

    // ── 文件读写 ─────────────────────────────────────────

    private fun read(context: Context): JSONObject? = synchronized(fileLock) {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return@synchronized null
        runCatching { JSONObject(file.readText()) }.getOrNull()
    }

    private fun write(context: Context, block: JSONObject.() -> Unit) = synchronized(fileLock) {
        val json = read(context) ?: JSONObject()
        json.block()
        val target = File(context.filesDir, FILE_NAME)
        val tmp = File(context.filesDir, "$FILE_NAME.tmp")
        tmp.writeText(json.toString())
        if (!tmp.renameTo(target)) {
            java.nio.file.Files.move(
                tmp.toPath(), target.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING,
            )
        }
    }
}
