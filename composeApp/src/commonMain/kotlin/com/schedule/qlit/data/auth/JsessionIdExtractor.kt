package com.schedule.qlit.data.auth

import com.schedule.qlit.data.repository.lenientJson
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * 解析 CDP Network.getAllCookies 响应，过滤出 student 域 JSESSIONID。
 *
 * Cookie 按域名、Path 和有效期判断是否会随 `/student/...` 请求发送；Tomcat
 * sticky-route 后缀只作为多个候选项之间的排序偏好，避免服务端节点名变化后失效。
 *
 * 例：`<session-id>-nX.tomcatX`
 *
 * 移植自 cdp-capture 程序的 JsessionIdExtractor.kt。
 */
object JsessionIdExtractor {

    private const val STUDENT_HOST = "pass.qlit.edu.cn"
    private const val STUDENT_REQUEST_PATH = "/student/mobile/admin.jsp"
    private val stickyRouteSuffix = Regex("""-n\d+\.tomcat\d+$""", RegexOption.IGNORE_CASE)

    data class Result(val value: String, val domain: String, val path: String)
    internal data class CookieCandidate(
        val value: String,
        val domain: String,
        val path: String,
        val expires: Double,
    )

    private fun JsonObject.optString(key: String): String =
        this[key]?.jsonPrimitive?.content.orEmpty()

    /**
     * @param rawJson CDP getAllCookies 的完整响应 JSON
     * @return 匹配到的 student 域 JSESSIONID，或 null
     */
    @OptIn(ExperimentalTime::class)
    fun extractStudentJsession(
        rawJson: String,
        ignoredValues: Set<String> = emptySet(),
        nowEpochSeconds: Double = Clock.System.now().toEpochMilliseconds() / 1000.0,
    ): Result? {
        return try {
            val root = lenientJson.parseToJsonElement(rawJson).jsonObject
            val cookies = (root["result"] as? JsonObject)
                ?.get("cookies") as? JsonArray ?: return null
            val candidates = mutableListOf<CookieCandidate>()
            cookies.forEach { element ->
                val c = element as? JsonObject ?: return@forEach
                if (c.optString("name") != "JSESSIONID") return@forEach
                candidates += CookieCandidate(
                    value = c.optString("value"),
                    domain = c.optString("domain"),
                    path = c.optString("path").ifBlank { "/" },
                    expires = c["expires"]?.jsonPrimitive?.doubleOrNull ?: -1.0,
                )
            }
            selectStudentJsession(candidates, ignoredValues, nowEpochSeconds)
        } catch (t: Throwable) {
            null
        }
    }

    internal fun selectStudentJsession(
        candidates: List<CookieCandidate>,
        ignoredValues: Set<String> = emptySet(),
        nowEpochSeconds: Double,
    ): Result? = candidates.asSequence()
        .filter { it.value.isNotBlank() && it.value !in ignoredValues }
        .filter { domainMatches(it.domain) && pathMatches(it.path) }
        .filter { it.expires <= 0.0 || it.expires > nowEpochSeconds }
        .sortedWith(
            compareByDescending<CookieCandidate> { it.path.length }
                .thenByDescending { stickyRouteSuffix.containsMatchIn(it.value) }
        )
        .map { Result(it.value, it.domain, it.path) }
        .firstOrNull()

    private fun domainMatches(rawDomain: String): Boolean {
        val domain = rawDomain.trim().removePrefix(".").lowercase()
        return domain.isNotEmpty() &&
            (STUDENT_HOST == domain || STUDENT_HOST.endsWith(".$domain"))
    }

    private fun pathMatches(cookiePath: String): Boolean {
        if (cookiePath == "/") return true
        if (!STUDENT_REQUEST_PATH.startsWith(cookiePath)) return false
        return cookiePath.endsWith("/") ||
            STUDENT_REQUEST_PATH.length == cookiePath.length ||
            STUDENT_REQUEST_PATH.getOrNull(cookiePath.length) == '/'
    }

    /** 仅用于调试：返回所有 JSESSIONID 的域和路径，不包含凭据值。 */
    fun debugListAllJsession(rawJson: String): List<String> {
        val out = mutableListOf<String>()
        try {
            val root = lenientJson.parseToJsonElement(rawJson).jsonObject
            val cookies = (root["result"] as? JsonObject)
                ?.get("cookies") as? JsonArray ?: return out
            cookies.forEach { element ->
                val c = element as? JsonObject ?: return@forEach
                if (c.optString("name") == "JSESSIONID") {
                    out.add("${c.optString("domain")}|${c.optString("path")}")
                }
            }
        } catch (_: Throwable) {}
        return out
    }
}
