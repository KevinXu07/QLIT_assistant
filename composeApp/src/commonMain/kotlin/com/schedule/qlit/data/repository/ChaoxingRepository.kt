package com.schedule.qlit.data.repository

import com.schedule.qlit.data.IODispatcher
import com.schedule.qlit.data.crypto.ChaoxingCrypto
import com.schedule.qlit.data.model.ChaoxingTask
import com.schedule.qlit.data.model.ChaoxingTaskResult
import com.schedule.qlit.data.net.Http
import com.schedule.qlit.data.net.HttpResponse
import com.schedule.qlit.data.store.KeyValueStore
import com.schedule.qlit.data.store.kvStore
import io.ktor.http.Url
import io.ktor.http.decodeURLQueryComponent
import io.ktor.http.encodeURLQueryComponent
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * 超星学习通「待完成作业/考试」的数据层。
 *
 * 链路（参见 docs/05-安卓小助手接入交接.md）：
 *  1. 网页版登录：GET /login 拿种子 → 本地 AES-CBC 加密账号密码 → POST /fanyalogin
 *  2. 持久化 `.chaoxing.com` 域 cookie（明文 prefs）+ 账号密码（加密 prefs，便于自动重登）
 *  3. 拉两个 HTML 入口并跟随 302，正则解析列表
 *
 * `object` 单例 + 薄 Http 层 + 正则解析，
 * 不引入 OkHttp / Jsoup，保持项目一致性。
 *
 * cookie 用内存 [CookieJar] 收集登录响应里的 Set-Cookie，
 * 后续请求把保存的 cookie 字符串塞回 `Cookie` 请求头，避免依赖全局 cookie 容器。
 */
object ChaoxingRepository {

    private const val LOGIN_PAGE = "https://passport2.chaoxing.com/login"
    private const val LOGIN_API = "https://passport2.chaoxing.com/fanyalogin"
    private const val WORK_TRANSFER = "https://mooc1-api.chaoxing.com/mooc-ans/work/transfer/stu-work"
    private const val EXAM_TRANSFER = "https://mooc1-api.chaoxing.com/mooc-ans/exam/test/transfer/examlist"
    private const val COURSE_LIST =
        "https://mooc1-api.chaoxing.com/mycourse/backclazzdata?view=json&getTchClazzType=1&mcode="

    private const val PREFS_COOKIE = "chaoxing_cookie_prefs"
    private const val PREFS_SECURE = "chaoxing_secure_prefs"
    private const val KEY_COOKIE = "cookie"
    private const val KEY_UNAME = "uname"
    private const val KEY_PASSWORD = "password"

    /** 登录成功后必须出现的关键 cookie（用于判断是否真的登录成功）。 */
    private val REQUIRED_COOKIES = setOf("vc3", "_uid", "fid")

    private const val UA =
        "Mozilla/5.0 (Linux; Android 15; RMX5010 Build/BP2A.250605.015) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"

    private val json = Json { ignoreUnknownKeys = true }

    // ── 登录态 ────────────────────────────────────────────────

    /** 是否已保存有效 cookie（不保证未过期）。 */
    fun isLoggedIn(): Boolean =
        cookieStore().getString(KEY_COOKIE).isNullOrBlank().not()

    /** 已保存的账号（用于预填登录页）。 */
    fun savedUname(): String =
        secureStore().getString(KEY_UNAME, "") ?: ""

    /** 已保存的密码（用于预填登录页 + 自动重登）。 */
    fun savedPassword(): String =
        secureStore().getString(KEY_PASSWORD, "") ?: ""

    /** 退出登录：清空 cookie 与凭证。 */
    fun logout() {
        cookieStore().clear()
        secureStore().clear()
        markChaoxingWidgetLoggedOut()
    }

    /**
     * 网页版登录。
     *
     * @return 成功与否 + 失败原因（供 UI 直接展示）。
     */
    suspend fun login(uname: String, password: String): LoginResultData =
        withContext(IODispatcher) {
            try {
                // 1. 先 GET 登录页，拿会话种子 cookie（存到内存 jar）
                val seedJar = CookieJar()
                fetchText(LOGIN_PAGE, jar = seedJar, cookieHeader = null, followRedirects = true)

                // 2. 本地 AES-CBC 加密账号密码
                val encUname = ChaoxingCrypto.encrypt(uname)
                val encPwd = ChaoxingCrypto.encrypt(password)

                // 3. POST /fanyalogin（按交接文档 §4.3），Set-Cookie 会 merge 进同一个 jar
                val body = linkedMapOf(
                    "fid" to "-1",
                    "uname" to encUname,
                    "password" to encPwd,
                    "refer" to "https://www.chaoxing.com/",
                    "t" to "true",
                    "forbidotherlogin" to "0",
                    "validate" to "",
                    "doubleFactorLogin" to "0",
                    "independentId" to "0",
                    "independentNameId" to "0"
                ).entries.joinToString("&") { (k, v) ->
                    "${k.encodeURLQueryComponent(spaceToPlus = true)}=${v.encodeURLQueryComponent(spaceToPlus = true)}"
                }

                val respText = postForm(LOGIN_API, body, seedJar, null)

                // 4. 成功判定：JSON status==true，且关键 cookie 已落盘
                val statusOk = respText.contains("\"status\"\\s*:\\s*true".toRegex()) ||
                    respText.contains("\"status\":true")
                val hasCookies = REQUIRED_COOKIES.any { seedJar.containsKey(it) }
                if (!statusOk || !hasCookies) {
                    val msg = parseLoginError(respText) ?: "登录失败，请检查账号密码或稍后重试"
                    return@withContext LoginResultData(false, msg)
                }

                // 5. 持久化全量 cookie（明文，便于后续请求直接复用）+ 账号密码（加密）
                cookieStore().putString(KEY_COOKIE, seedJar.toCookieHeader())
                val secure = secureStore()
                secure.putString(KEY_UNAME, uname)
                secure.putString(KEY_PASSWORD, password)

                LoginResultData(true, "")
            } catch (e: Exception) {
                LoginResultData(false, "登录请求失败：${e.message ?: "未知错误"}")
            }
        }

    /**
     * 拉取作业 + 考试待办。若 cookie 失效会抛 [CookieExpiredException]。
     */
    suspend fun fetchTasks(): ChaoxingTaskResult = withContext(IODispatcher) {
        val cookie = cookieStore().getString(KEY_COOKIE, "") ?: ""
        if (cookie.isBlank()) {
            markChaoxingWidgetExpired()
            throw CookieExpiredException()
        }

        val workHtml: String
        val examHtml: String
        try {
            workHtml = fetchText(WORK_TRANSFER, jar = CookieJar(), cookieHeader = cookie, followRedirects = true)
            throwIfLoginRedirected(workHtml)
            examHtml = fetchText(EXAM_TRANSFER, jar = CookieJar(), cookieHeader = cookie, followRedirects = true)
            throwIfLoginRedirected(examHtml)
        } catch (e: CookieExpiredException) {
            markChaoxingWidgetExpired()
            throw e
        }
        val courseNameMap = runCatching {
            parseCourseNameMap(fetchText(COURSE_LIST, jar = CookieJar(), cookieHeader = cookie, followRedirects = true))
        }.getOrDefault(emptyMap())
        val works = parseWorkItems(workHtml)
        val exams = parseExamItems(examHtml)
        val enriched = enrichCourseNames(works, exams, courseNameMap)

        ChaoxingTaskResult(
            works = enriched.first,
            exams = enriched.second
        ).also { result ->
            saveChaoxingToWidgetCache(
                pendingWork = result.pendingWorkCount,
                pendingExam = result.pendingExamCount,
            )
        }
    }

    // ── 网络层 ────────────────────────────────────────────────

    /** 登录失效异常，UI 据此回到登录态。 */
    class CookieExpiredException : RuntimeException("超星登录已失效，请重新登录")

    /** 轻量 cookie 容器：仅记录本次请求链路里所有 Set-Cookie 的 name=value。 */
    private class CookieJar {
        private val map = LinkedHashMap<String, String>()
        fun merge(setCookieHeader: String?) {
            if (setCookieHeader.isNullOrBlank()) return
            // Set-Cookie 可能多行，每行形如 "name=value; Path=/; Domain=.chaoxing.com; ..."
            for (raw in setCookieHeader.split("\n", ";")) {
                val item = raw.trim()
                if (item.isEmpty() || '=' !in item) continue
                val idx = item.indexOf('=')
                val name = item.substring(0, idx).trim()
                val value = item.substring(idx + 1).trim()
                if (name.isEmpty()) continue
                // 跳过属性名
                if (name.equals("Path", true) || name.equals("Domain", true) ||
                    name.equals("Expires", true) || name.equals("Max-Age", true) ||
                    name.equals("Secure", true) || name.equals("HttpOnly", true) ||
                    name.equals("SameSite", true)
                ) continue
                map[name] = value
            }
        }
        fun toCookieHeader(): String =
            map.entries.joinToString("; ") { "${it.key}=${it.value}" }
        fun containsKey(name: String): Boolean = map.containsKey(name)
    }

    private suspend fun fetchText(
        url: String,
        jar: CookieJar,
        cookieHeader: String?,
        followRedirects: Boolean
    ): String {
        val resp = Http.get(
            url,
            headers = buildMap {
                put("User-Agent", UA)
                put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                put("Accept-Language", "zh-CN,zh;q=0.9")
                if (!cookieHeader.isNullOrBlank()) put("Cookie", cookieHeader)
            },
            followRedirects = followRedirects,
        )
        jar.merge(concatSetCookie(resp))
        return resp.text()
    }

    /** POST 表单，Set-Cookie 会 merge 进 [jar]，返回响应文本。 */
    private suspend fun postForm(
        url: String,
        body: String,
        jar: CookieJar,
        cookieHeader: String?
    ): String {
        val resp = Http.post(
            url,
            body.encodeToByteArray(),
            headers = buildMap {
                put("User-Agent", UA)
                put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                put("Accept", "application/json, text/plain, */*")
                put("Accept-Language", "zh-CN,zh;q=0.9")
                put("Referer", LOGIN_PAGE)
                if (!cookieHeader.isNullOrBlank()) put("Cookie", cookieHeader)
            },
            followRedirects = false, // 登录响应通常是 200 + JSON，不跟跳转
        )
        jar.merge(concatSetCookie(resp))
        return resp.text()
    }

    /** 合并所有 Set-Cookie / Set-Cookie2 头（Ktor 不自动收集跨请求 cookie）。 */
    private fun concatSetCookie(resp: HttpResponse): String =
        (resp.headerValues("Set-Cookie") + resp.headerValues("Set-Cookie2"))
            .joinToString("") { "$it\n" }

    private fun parseLoginError(respText: String): String? {
        // 常见失败文案兜底
        val text = respText.trim()
        return when {
            text.contains("密码错误", true) || text.contains("用户名或密码错误") -> "账号或密码错误"
            text.contains("验证码") -> "需要验证码，请稍后重试或在网页端登录一次"
            text.contains("\"status\":false", true) -> "登录被拒绝，可能触发了风控"
            else -> null
        }
    }

    /** 检测最终页面是否被重定向回了登录页。 */
    private fun throwIfLoginRedirected(html: String) {
        if (html.contains("passport2.chaoxing.com/login") ||
            html.contains("id=\"pwdLogin\"") ||
            (html.contains("请输入手机号") && html.contains("请输入密码"))
        ) {
            throw CookieExpiredException()
        }
    }

    // ── HTML 解析 ─────────────────────────────────────────────

    /**
     * 作业列表解析。结构（交接文档 §3.1）：
     * ```
     * <li onclick="goTask(this);" data="https://...?taskrefId=..&courseId=..&clazzId=..&type=work&enc_task=..">
     *   <p>作业标题</p>
     *   <span class="status">未提交</span>
     *   <span>《课程名》</span>
     *   <span class="fr">剩余129小时43分钟</span>
     * </li>
     * ```
     */
    private val WORK_ITEM = Regex(
        """<li([^>]*)onclick="goTask\(this\);"([^>]*)>(.*?)</li>""",
        RegexOption.DOT_MATCHES_ALL
    )

    /** 考试列表解析。结构（交接文档 §3.2）。 */
    private val EXAM_ITEM = Regex(
        """<li([^>]*)onclick="goTask\(this\);"([^>]*)>(.*?)</li>""",
        RegexOption.DOT_MATCHES_ALL
    )

    private val ATTR_DATA = Regex("\\bdata=\"([^\"]+)\"")
    private val ATTR_TITLE = Regex("\\btitle=\"([^\"]*)\"")
    private val P_TAG = Regex("""<p[^>]*>(.*?)</p>""", RegexOption.DOT_MATCHES_ALL)
    private val DT_TAG = Regex("""<dt[^>]*>(.*?)</dt>""", RegexOption.DOT_MATCHES_ALL)
    private val SPAN_STATUS = Regex("""<span[^>]*class="status"[^>]*>(.*?)</span>""", RegexOption.DOT_MATCHES_ALL)
    private val SPAN_ANY = Regex("""<span([^>]*)>(.*?)</span>""", RegexOption.DOT_MATCHES_ALL)
    private val SPAN_COURSE = Regex("""<span[^>]*aria-hidden="true"[^>]*>(.*?)</span>""", RegexOption.DOT_MATCHES_ALL)
    private val SPAN_FR = Regex("""<span[^>]*class="fr"[^>]*>(.*?)</span>""", RegexOption.DOT_MATCHES_ALL)
    private val DD_TAG = Regex("""<dd[^>]*>(.*?)</dd>""", RegexOption.DOT_MATCHES_ALL)
    private val SPAN_KS_STATE = Regex("""<span[^>]*class="ks_state"[^>]*>(.*?)</span>""", RegexOption.DOT_MATCHES_ALL)

    internal fun parseWorkItems(html: String): List<ChaoxingTask> {
        val out = ArrayList<ChaoxingTask>()
        for (m in WORK_ITEM.findAll(html)) {
            val attrs = m.groupValues[1] + m.groupValues[2]
            val data = firstGroup(ATTR_DATA, attrs) ?: continue
            val inner = m.groupValues[3]
            val title = firstGroup(P_TAG, inner) ?: ""
            val status = extractWorkStatus(inner)
            val courseRaw = extractWorkCourseName(inner)
            val remain = firstGroup(SPAN_FR, inner).orEmpty()

            val qs = parseQuery(data)
            val (code, pending) = normalizeStatus(status)
            out += ChaoxingTask(
                type = ChaoxingTask.TYPE_WORK,
                title = cleanText(title),
                courseName = cleanCourseName(courseRaw),
                courseId = qs["courseId"] ?: qs["courseid"].orEmpty(),
                classId = qs["clazzId"] ?: qs["classId"] ?: qs["classid"].orEmpty(),
                taskrefId = qs["taskrefId"] ?: qs["taskRefId"].orEmpty(),
                statusText = cleanText(status),
                pending = pending,
                statusCode = code,
                remainText = cleanText(remain),
                detailUrl = normalizeDetailUrl(data, isExam = false)
            )
        }
        return out
    }

    internal fun enrichCourseNames(
        works: List<ChaoxingTask>,
        exams: List<ChaoxingTask>,
        courseNameMap: Map<String, String> = emptyMap()
    ): Pair<List<ChaoxingTask>, List<ChaoxingTask>> {
        val merged = LinkedHashMap<String, String>()
        works.forEach { task ->
            putCourseName(merged, task.courseId, task.classId, task.courseName)
        }
        courseNameMap.forEach { (key, value) ->
            if (value.isNotBlank()) merged[key] = value
        }

        fun enrich(task: ChaoxingTask): ChaoxingTask {
            if (!shouldReplaceCourseName(task.courseName)) return task
            val resolved = merged[courseKey(task.courseId, task.classId)]
                ?: merged[courseOnlyKey(task.courseId)]
                ?: return task
            return task.copy(courseName = resolved)
        }

        return works.map(::enrich) to exams.map(::enrich)
    }

    private fun extractWorkStatus(inner: String): String {
        firstGroup(SPAN_STATUS, inner)?.let { explicit ->
            if (cleanText(explicit).isNotBlank()) return explicit
        }

        for (m in SPAN_ANY.findAll(inner)) {
            val attrs = m.groupValues[1]
            val text = m.groupValues[2]
            if (attrs.contains("class=\"fr\"", ignoreCase = true)) continue
            if (normalizeStatus(text).first != -1) return text
        }

        return ""
    }

    private fun extractWorkCourseName(inner: String): String {
        for (m in SPAN_COURSE.findAll(inner)) {
            val text = m.groupValues[1]
            if (normalizeStatus(text).first != -1) continue
            val cleaned = cleanCourseName(text)
            if (cleaned.isNotBlank()) return cleaned
        }
        return ""
    }

    private fun shouldReplaceCourseName(name: String): Boolean =
        name.isBlank() || name.contains("...")

    private fun putCourseName(
        target: MutableMap<String, String>,
        courseId: String,
        classId: String,
        courseName: String
    ) {
        val cleanName = cleanCourseName(courseName)
        if (courseId.isBlank() || cleanName.isBlank()) return
        if (classId.isNotBlank()) target[courseKey(courseId, classId)] = cleanName
        val onlyKey = courseOnlyKey(courseId)
        if (onlyKey !in target) target[onlyKey] = cleanName
    }

    internal fun parseExamItems(html: String): List<ChaoxingTask> {
        val out = ArrayList<ChaoxingTask>()
        for (m in EXAM_ITEM.findAll(html)) {
            val attrs = m.groupValues[1] + m.groupValues[2]
            val data = firstGroup(ATTR_DATA, attrs) ?: continue
            val titleAttr = firstGroup(ATTR_TITLE, attrs).orEmpty()
            val inner = m.groupValues[3]
            val remain = firstGroup(DD_TAG, inner).orEmpty()
            val status = firstGroup(SPAN_KS_STATE, inner) ?: ""

            val qs = parseQuery(data)
            val (code, pending) = normalizeStatus(status)
            out += ChaoxingTask(
                type = ChaoxingTask.TYPE_EXAM,
                title = cleanText(titleAttr)
                    .ifBlank { cleanText(firstGroup(DT_TAG, inner).orEmpty()) }
                    .ifBlank { cleanText(firstGroup(P_TAG, inner).orEmpty()) },
                courseName = "",
                courseId = qs["courseId"] ?: qs["courseid"].orEmpty(),
                classId = qs["classId"] ?: qs["classid"].orEmpty(),
                taskrefId = qs["taskrefId"] ?: qs["taskRefId"].orEmpty(),
                statusText = cleanText(status),
                pending = pending,
                statusCode = code,
                remainText = cleanText(remain),
                detailUrl = normalizeDetailUrl(data, isExam = true)
            )
        }
        return out
    }

    private fun firstGroup(pattern: Regex, input: String): String? =
        pattern.find(input)?.groupValues?.get(1)

    private fun parseQuery(urlOrPath: String): Map<String, String> {
        val q = urlOrPath.substringAfter('?', "")
        if (q.isEmpty()) return emptyMap()
        val out = LinkedHashMap<String, String>()
        for (pair in q.split('&')) {
            val idx = pair.indexOf('=')
            if (idx < 0) continue
            val k = pair.substring(0, idx)
            val v = pair.substring(idx + 1)
            out[k] = runCatching { v.decodeURLQueryComponent(plusIsSpace = true) }
                .getOrDefault(v)
        }
        return out
    }

    private fun normalizeDetailUrl(data: String, isExam: Boolean): String {
        val absolute = if (data.startsWith("http")) data
        else if (data.startsWith("//")) "https:$data"
        else "https://mooc1-api.chaoxing.com${if (data.startsWith("/")) "" else "/"}$data"

        return when {
            isExam && !absolute.contains("ut=") && absolute.contains("#") ->
                absolute.substringBefore('#') + "&ut=s#INNER"
            isExam && !absolute.contains("ut=") ->
                "$absolute&ut=s#INNER"
            !absolute.contains("#INNER") -> "$absolute#INNER"
            else -> absolute
        }
    }

    private val PENDING_WORDS = listOf("未交", "未提交", "待完成", "未完成", "未做")
    private val REVIEW_WORDS = listOf("待批阅", "待阅", "批阅中")
    private val DONE_WORDS = listOf("已完成", "已交", "已提交")

    internal fun parseCourseNameMap(json: String): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        val root = runCatching { this.json.parseToJsonElement(json) }.getOrNull() ?: return out

        fun walk(node: JsonElement?) {
            when {
                node == null || node is JsonNull -> Unit
                node is JsonObject -> {
                    val courseId = firstJsonString(node, "courseId", "courseid", "id", "key", "mappingcourseid")
                    val classId = firstJsonString(node, "classId", "classid", "clazzId", "clazzid")
                    val courseName = firstJsonString(node, "courseName", "coursename", "name", "title")
                    if (!courseId.isNullOrBlank() && !courseName.isNullOrBlank()) {
                        putCourseName(out, courseId, classId.orEmpty(), courseName)
                    }
                    node.values.forEach { walk(it) }
                }
                node is JsonArray -> node.forEach { walk(it) }
            }
        }

        walk(root)
        return out
    }

    private fun firstJsonString(obj: JsonObject, vararg keys: String): String? {
        for (key in keys) {
            val value = obj[key]
            if (value is JsonPrimitive) {
                val text = value.contentOrNull
                if (!text.isNullOrBlank()) return text
            }
        }
        return null
    }

    private fun normalizeStatus(state: String): Pair<Int, Boolean> {
        val s = cleanText(state)
        return when {
            PENDING_WORDS.any { it in s } -> 0 to true
            REVIEW_WORDS.any { it in s } -> 1 to false
            DONE_WORDS.any { it in s } -> 2 to false
            else -> -1 to false
        }
    }

    private fun cleanText(text: String): String =
        collapseWs(unescapeHtml(text)).trim()

    private fun cleanCourseName(text: String): String =
        cleanText(text).trim('《', '》').trim()

    private fun courseKey(courseId: String, classId: String): String = "$courseId|$classId"

    private fun courseOnlyKey(courseId: String): String = "$courseId|"

    private fun collapseWs(text: String): String =
        text.replace("\\s+".toRegex(), " ")

    private fun unescapeHtml(text: String): String =
        text.replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
            // 数字字符引用 &#xx; 或 &#x;
            .replace("&#x([0-9a-fA-F]+);".toRegex()) { mr ->
                mr.groupValues[1].toIntOrNull(16)?.toChar()?.toString() ?: mr.value
            }
            .replace("&#([0-9]+);".toRegex()) { mr ->
                mr.groupValues[1].toIntOrNull()?.toChar()?.toString() ?: mr.value
            }

    // ── 偏好（cookie 明文 / 凭证加密） ─────────────────────────

    private fun cookieStore(): KeyValueStore = kvStore(PREFS_COOKIE)

    /** 加密偏好；Keystore/Keychain 异常时降级明文，保证 App 不崩。 */
    private fun secureStore(): KeyValueStore =
        runCatching { kvStore(PREFS_SECURE, secure = true) }
            .getOrElse { kvStore(PREFS_SECURE) }

    @Suppress("unused")
    private fun uriHost(url: String): String =
        runCatching { Url(url).host }.getOrDefault("")

    /** 静态登录结果类型，供 UI 与单测使用。 */
    data class LoginResultData(val success: Boolean, val message: String)
}
