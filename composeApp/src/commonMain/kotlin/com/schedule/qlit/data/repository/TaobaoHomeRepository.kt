package com.schedule.qlit.data.repository

import com.schedule.qlit.data.IODispatcher
import com.schedule.qlit.data.crypto.md5Hex
import com.schedule.qlit.data.net.Http
import com.schedule.qlit.data.store.flushWebViewCookies
import com.schedule.qlit.data.store.setWebViewCookie
import com.schedule.qlit.data.store.webViewCookie
import kotlinx.coroutines.withContext
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * 淘宝菜鸟取件首页数据的获取逻辑。
 *
 * 纯网络层：MTop 签名、jsonp 解析、preCheck + home 查询、token 自举。
 * 调用方（TaobaoPickupScreen / TodayScreen）负责偏好读写和 UI 状态。
 *
 * 设计目标是“无头优先”：正常情况下只走 mtop JSONP 接口，不依赖 WebView；
 * 只有 cookie 缺失/登录失效或 mtop 风控（返回 punish 链接、ILL/SYS 失败码）
 * 时才需要调用方拉起 WebView 让用户登录或过人机校验。
 */
internal object TaobaoHomeRepository {

    private const val MTOP_HOST = "https://h5api.m.taobao.com"
    private const val MTOP_JSV = "2.6.1"
    private const val MTOP_APP_KEY = "12574478"
    const val PICKUP_URL =
        "https://pages-fast.m.taobao.com/wow/z/uniapp/1100333/last-mile-fe/m-end-school-tab/home"
    /** 身份码页面（取件码）入口，与取件首页 PICKUP_URL 不同。 */
    const val IDENTITY_URL =
        "https://pages-fast.m.taobao.com/wow/z/uniapp/1100410/last-mile-fe/m-end-identity-code/home?x-ssr=true&tab=school&skeleton=true&forceThemis=true"

    /** mtop ret 码分类：调用方据此决定要不要拉起 WebView。 */
    enum class MtopStatus {
        /** ret 含 SUCCESS，或响应里没有 ret 字段（部分接口不返回）。 */
        SUCCESS,

        /** 未登录 / sid 失效 / cookie 缺失——需要 WebView 让用户重新登录。 */
        SESSION_EXPIRED,

        /**
         * token 过期（FAIL_SYS_TOKEN_EXOIRED / TOKEN_EMPTY）。发生时会先从
         * 响应 Set-Cookie 里自举新的 `_m_h5_tk` 并重试一次；仍失败才报出。
         */
        TOKEN_EXPIRED,

        /** 触发风控（ret 里带 punish/x5sec 链接，或 ILL/SYS 失败）。需要 WebView 过人机校验。 */
        CHALLENGED,

        /** 其它失败（网络错误、签名错误、未知 ret）。 */
        FAILED,
    }

    data class MtopResult(
        val status: MtopStatus,
        val body: String,
        /** 解析出的 JSON 主体（去掉 `mtopjsonp_xxx(...)` 包装）；jsonp 外层缺失时为 null。 */
        val json: String?,
        /** 原始 ret 列表文本，便于 UI 显示或记录日志。 */
        val retText: String,
    ) {
        val ok: Boolean get() = status == MtopStatus.SUCCESS
    }

    data class RefreshResult(
        val loggedIn: Boolean,
        val pendingCount: Int?,
        val stationHint: String
    )

    /** 一个待取包裹：取件码、快递单号、商品图等成组信息。 */
    data class TaobaoPackage(
        val pickupCode: String = "",
        val trackingNumber: String = "",
        val title: String = "",
        val carrier: String = "",
        val imageURL: String = "",
        val station: String = "",
    )

    /** MTOP ret 行里切出的错误码（如 FAIL_SYS_TOKEN_EXOIRED::令牌过期）。 */
    private fun retHas(ret: String, vararg needles: String): Boolean =
        needles.any { ret.contains(it, ignoreCase = true) }

    /** 从 `mtopjsonp_xxx({...})` 包装里抽出 JSON 主体。 */
    internal fun parseJsonpPayload(text: String): String? {
        val start = text.indexOf('(')
        val end = text.lastIndexOf(')')
        if (start < 0 || end <= start) return null
        return text.substring(start + 1, end)
    }

    @OptIn(ExperimentalTime::class)
    internal fun buildTraceId(): String {
        val random = Random.nextBytes(7).joinToString("") {
            (it.toInt() and 0xFF).toString(16).padStart(2, '0')
        }
        return "${Clock.System.now().toEpochMilliseconds()}-${random.take(13)}"
    }

    private fun md5(text: String): String = md5Hex(text.encodeToByteArray())

    private fun extractCookieValue(cookies: String, key: String): String? =
        cookies.split(";")
            .map { it.trim() }
            .firstNotNullOfOrNull { item ->
                if (item.startsWith("$key=")) item.substringAfter("=") else null
            }

    /** 当前 `_m_h5_tk` token（不含 `_{expire}` 后缀）；没有则 null。 */
    private suspend fun currentMtopToken(): String? {
        val cookies = webViewCookie(MTOP_HOST)
            ?: webViewCookie(PICKUP_URL)
            ?: return null
        return extractCookieValue(cookies, "_m_h5_tk")
            ?.substringBefore("_")
            ?.takeIf { it.isNotBlank() }
    }

    /**
     * 把 mtop 响应的 Set-Cookie 写回 WebView cookie 仓库。
     *
     * token 过期时 mtop 会在 Set-Cookie 里下发新的 `_m_h5_tk`（带 `_{expire}` 后缀），
     * 必须落库后重试一次，否则所有无头请求会持续 TOKEN_EMPTY。
     */
    private suspend fun persistResponseCookies(
        resp: com.schedule.qlit.data.net.HttpResponse,
        referer: String
    ) {
        val setCookies = resp.headerValues("Set-Cookie")
        setCookies.forEach { cookie ->
            runCatching { setWebViewCookie(MTOP_HOST, cookie) }
            // referer 域（pages-fast.m.taobao.com）也写一份，保证页面域 cookie 同样刷新
            runCatching { setWebViewCookie(referer, cookie) }
        }
        if (setCookies.isNotEmpty()) {
            flushWebViewCookies()
        }
    }

    /**
     * 发起一次 MTop JSONP 请求，返回 [MtopResult]。
     *
     * 流程：签名 → GET → 写回 Set-Cookie → 解析 ret 分类。
     * 若分类为 TOKEN_EXPIRED 且本次是首次尝试（[retryOnTokenExpired]），
     * 用刚写入的新 token 自动重试一次。
     */
    @OptIn(ExperimentalTime::class)
    internal suspend fun requestMtop(
        api: String,
        dataJson: String,
        referer: String,
        userAgent: String,
        callback: String = "mtopjsonp_home",
        retryOnTokenExpired: Boolean = true
    ): MtopResult = withContext(IODispatcher) {
        val cookies = webViewCookie(MTOP_HOST)
            ?: webViewCookie(referer)
            ?: return@withContext MtopResult(
                status = MtopStatus.SESSION_EXPIRED,
                body = "",
                json = null,
                retText = "missing taobao cookie"
            )

        val token = extractCookieValue(cookies, "_m_h5_tk")
            ?.substringBefore("_")
            ?.takeIf { it.isNotBlank() }
            ?: return@withContext MtopResult(
                status = MtopStatus.SESSION_EXPIRED,
                body = "",
                json = null,
                retText = "missing _m_h5_tk token"
            )

        val timestamp = Clock.System.now().toEpochMilliseconds().toString()
        val sign = md5("$token&$timestamp&$MTOP_APP_KEY&$dataJson")

        val query = linkedMapOf<String, String>(
            "jsv" to MTOP_JSV,
            "appKey" to MTOP_APP_KEY,
            "t" to timestamp,
            "sign" to sign,
            "api" to api,
            "v" to "1.0",
            "needLogin" to "true",
            "SessionOption" to "AUTO_LOGIN_AND_MANUAL_LOGIN",
            "mpHost" to "acs.m.taobao.com",
            "valueType" to "original",
            "extHeaders" to "[object Object]",
            "responseType" to "ORIGINAL_JSON",
            "H5Request" to "true",
            "LoginRequest" to "true",
            "type" to "originaljsonp",
            "dataType" to "jsonp",
            "callback" to callback,
            "data" to dataJson
        ).entries.joinToString("&") { (key, value) ->
            "${urlEncodeForm(key)}=${urlEncodeForm(value)}"
        }

        val resp = Http.get(
            "$MTOP_HOST/h5/${api.lowercase()}/1.0/?$query",
            headers = mapOf(
                "Cookie" to cookies,
                "User-Agent" to userAgent,
                "Accept" to "*/*",
                "Referer" to referer,
                "Origin" to "https://pages-fast.m.taobao.com",
            ),
        )
        val body = resp.text()
        persistResponseCookies(resp, referer)

        // 正常是 jsonp 包装；偶尔 mtop 直接回纯 JSON，此时用 body 本体兜底
        val json = parseJsonpPayload(body)
            ?: body.trim().takeIf { it.startsWith("{") }
        val retText = runCatching {
            val root = json?.let { parseJsonMap(it) }
            (root?.get("ret") as? List<*>)?.joinToString(" ") { it.toString() }.orEmpty()
        }.getOrDefault("")

        val status = classifyRet(retText, body)
        if (status == MtopStatus.TOKEN_EXPIRED && retryOnTokenExpired && currentMtopToken() != token) {
            // token 已换新，用新 token 重试一次（递归不会再重试，因为 retryOnTokenExpired=false）
            return@withContext requestMtop(
                api = api,
                dataJson = dataJson,
                referer = referer,
                userAgent = userAgent,
                callback = callback,
                retryOnTokenExpired = false
            )
        }

        MtopResult(status = status, body = body, json = json, retText = retText)
    }

    private fun classifyRet(retText: String, rawBody: String): MtopStatus {
        if (retText.isBlank()) {
            // 没有 ret 字段：只要外层 jsonp 在、body 非空，先按成功处理（老逻辑如此）。
            return if (rawBody.isNotBlank()) MtopStatus.SUCCESS else MtopStatus.FAILED
        }
        return when {
            retText.contains("SUCCESS", ignoreCase = true) -> MtopStatus.SUCCESS

            retText.contains("TOKEN_EXOIRED", ignoreCase = true) ||
                retText.contains("TOKEN_EMPTY", ignoreCase = true) ||
                retText.contains("ILLEGAL_ACCESS", ignoreCase = true) ||
                retText.contains("令牌为空", ignoreCase = true) ||
                retText.contains("令牌过期", ignoreCase = true) -> MtopStatus.TOKEN_EXPIRED

            retText.contains("SESSION_EXPIRED", ignoreCase = true) ||
                retText.contains("NOT_LOGIN", ignoreCase = true) ||
                retText.contains("SID_INVALID", ignoreCase = true) ||
                retText.contains("FAIL_BIZ_NOT_LOGIN", ignoreCase = true) -> MtopStatus.SESSION_EXPIRED

            retText.contains("punish", ignoreCase = true) ||
                retText.contains("x5sec", ignoreCase = true) ||
                retText.contains("FAIL_SYS_ILLEGAL", ignoreCase = true) ||
                retText.contains("FAIL_SYS_", ignoreCase = true) ||
                retText.contains("验证码", ignoreCase = true) ||
                retText.contains("滑块", ignoreCase = true) -> MtopStatus.CHALLENGED

            else -> MtopStatus.FAILED
        }
    }

    /**
     * 无头刷新取件首页数据：preCheck 判登录态，成功后再查包裹列表。
     *
     * 被 TodayScreen 快捷卡片和桌面小组件共用——这些路径没有 WebView，
     * 依赖这里的 token 自举才能长期免登录运行。
     */
    suspend fun fetchRefreshResult(
        userAgent: String = "Mozilla/5.0 (Linux; Android 15; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
    ): RefreshResult = withContext(IODispatcher) {
        val traceId = buildTraceId()
        val preCheckData = linkedMapOf(
            "stationType" to "XY",
            "source" to "",
            "traceId" to traceId,
            "accessSource" to "",
            "subSource" to "",
            "productType" to "XY"
        ).toJsonElement().toString()
        val preCheck = requestMtop(
            api = "mtop.cainiao.pickup.productType.preCheck",
            dataJson = preCheckData,
            referer = PICKUP_URL,
            userAgent = userAgent
        )
        if (!preCheck.ok) {
            return@withContext RefreshResult(
                loggedIn = false,
                pendingCount = null,
                stationHint = preCheck.retText.ifBlank { "preCheck ${preCheck.status}" }
            )
        }
        val preCheckJson = preCheck.json ?: error("淘宝登录校验返回为空")
        val preCheckRoot = parseJsonMap(preCheckJson)
        @Suppress("UNCHECKED_CAST")
        val preCheckDataMap = preCheckRoot["data"] as? Map<String, Any?> ?: emptyMap()
        @Suppress("UNCHECKED_CAST")
        val preCheckInner = preCheckDataMap["data"] as? Map<String, Any?> ?: emptyMap()
        val userId = preCheckInner["userId"]?.toString().orEmpty()
        if (userId.isBlank()) {
            return@withContext RefreshResult(
                loggedIn = false,
                pendingCount = null,
                stationHint = ""
            )
        }

        val homeData = linkedMapOf(
            "stationType" to "XY",
            "source" to "",
            "traceId" to traceId
        ).toJsonElement().toString()
        val home = requestMtop(
            api = "mtop.cainiao.pickup.plus.queryMultiStaPackages4Xy",
            dataJson = homeData,
            referer = PICKUP_URL,
            userAgent = userAgent
        )
        if (!home.ok) {
            return@withContext RefreshResult(
                loggedIn = true,
                pendingCount = null,
                stationHint = home.retText.ifBlank { "home ${home.status}" }
            )
        }
        val homeJson = home.json ?: error("淘宝首页数据返回为空")
        val homeRoot = parseJsonMap(homeJson)
        @Suppress("UNCHECKED_CAST")
        val homeDataMap = homeRoot["data"] as? Map<String, Any?> ?: emptyMap()
        @Suppress("UNCHECKED_CAST")
        val homeInner = homeDataMap["data"] as? Map<String, Any?> ?: emptyMap()
        @Suppress("UNCHECKED_CAST")
        val homeBars = homeInner["homeBarList"] as? List<Map<String, Any?>> ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val stations = homeInner["staPkgAssembles"] as? List<Map<String, Any?>> ?: emptyList()

        var pendingCount: Int? = null
        val hintCandidates = mutableListOf<String>()

        homeBars.forEach { item ->
            item.values.forEach { value ->
                val text = value?.toString()?.trim().orEmpty()
                if (pendingCount == null) {
                    pendingCount = text.toIntOrNull()
                }
                if (text.contains("驿站") || text.contains("菜鸟") || text.contains("站点")) {
                    hintCandidates += text
                }
            }
        }

        if (pendingCount == null) {
            pendingCount = stations.size
        }

        stations.forEach { item ->
            val stationName = item["stationName"]?.toString()?.trim().orEmpty()
            if (stationName.isNotBlank()) {
                hintCandidates += stationName
            }
        }

        RefreshResult(
            loggedIn = true,
            pendingCount = pendingCount,
            stationHint = hintCandidates.firstOrNull().orEmpty()
        )
    }

    // MARK: - 结构化包裹解析（对标 iOS TaobaoHomeRepository.parsePackage）

    /**
     * 把一个节点解析成包裹列表：staPkgAssembles 的元素可能是按驿站分组、
     * 内部再嵌套包裹数组（staPkgVOList/pkgList/items 等）。递归找包裹数组，
     * 取解析出包裹最多的一份；没有数组就当作单个包裹 dict 解析。
     */
    fun parsePackagesFromNode(node: Any?): List<TaobaoPackage> {
        @Suppress("UNCHECKED_CAST")
        val item = node as? Map<String, Any?> ?: return emptyList()
        var best: List<TaobaoPackage> = emptyList()
        // 两遍探测：先只看 key 名像包裹列表的数组，再看任意 dict 数组
        for (pass in 0 until 2) {
            for ((key, value) in item) {
                val lk = key.lowercase()
                val pkgish = lk.contains("pkg") || lk.contains("package") ||
                    lk.contains("list") || lk.contains("item") || lk.contains("vo")
                if (pass == 0 && !pkgish) continue
                @Suppress("UNCHECKED_CAST")
                val arr = value as? List<Any?> ?: continue
                if (arr.isEmpty()) continue
                val pkgs = arr.flatMap { parsePackagesFromNode(it) }
                if (pkgs.size > best.size) best = pkgs
            }
            if (best.isNotEmpty()) return best
        }
        return parsePackage(item)?.let { listOf(it) } ?: emptyList()
    }

    /**
     * 把一个包裹 dict（staPkgAssembles/homeBarList 元素）解析为 TaobaoPackage。
     * 字段名按多种候选做深度搜索，兼容服务端结构漂移。
     */
    private fun parsePackage(item: Map<String, Any?>): TaobaoPackage? {
        // 取件码：专用字段 → 长文本提取 → 包裹内深度字段
        var pickupCode = (item["pickupCode"] as? String)?.trim().orEmpty()
        if (pickupCode.isEmpty()) {
            pickupCode = (item["pickupText"] as? String)
                ?.let { normalizePickupCodes(collectPickupCodeCandidates(it)).firstOrNull() }
                .orEmpty()
        }
        if (pickupCode.isEmpty()) {
            val codes = mutableListOf<String>()
            collectPickupCodeFields(item, codes)
            pickupCode = normalizePickupCodes(codes).firstOrNull().orEmpty()
        }

        // 快递单号：优先 mail/bill/tracking/express 系字段，再退到 order 系
        val trackingNumber = deepFirstString(item,
            keyMatches = {
                it.contains("mailno") || it.contains("billno") || it.contains("tracking") ||
                    it.contains("expressno") || it == "wbn" || it == "cpmailno"
            },
            valueOk = ::isPlausibleTrackingNumber
        ) ?: deepFirstString(item,
            keyMatches = {
                it.contains("mail") || it.contains("bill") ||
                    it.contains("orderno") || it.contains("orderid")
            },
            valueOk = ::isPlausibleTrackingNumber
        ).orEmpty()

        // 包裹图：img/pic/image/logo 系字段里的 URL（兼容 // 开头）
        val imageURL = deepFirstString(item,
            keyMatches = {
                it.contains("img") || it.contains("pic") ||
                    it.contains("image") || it.contains("logo")
            },
            valueOk = { it.startsWith("http") || it.startsWith("//") }
        ).orEmpty()

        // 商品/包裹标题
        val title = deepFirstString(item,
            keyMatches = {
                it.contains("title") || it.contains("goodsname") ||
                    it.contains("itemname") || it.contains("pkgname")
            }
        ).orEmpty()

        // 快递公司
        val carrier = deepFirstString(item,
            keyMatches = {
                it.contains("cpname") || it.contains("carrier") ||
                    it.contains("expressname") || it.contains("company")
            }
        ).orEmpty()

        // 驿站名：station.text 或 stationName 字段
        val station = (item["station"] as? Map<*, *>)?.get("text") as? String
            ?: (item["stationName"] as? String).orEmpty()

        // 至少要有取件码或单号，否则不是包裹 dict
        if (pickupCode.isEmpty() && trackingNumber.isEmpty()) return null
        return TaobaoPackage(
            pickupCode = pickupCode,
            trackingNumber = trackingNumber,
            title = title,
            carrier = carrier,
            imageURL = imageURL,
            station = station
        )
    }

    /**
     * 在任意嵌套 dict/array 里，找第一个「key 命中谓词 且 值通过校验」的字符串。
     * 先查本层命中的 key，再递归子节点。
     */
    private fun deepFirstString(
        node: Any?,
        keyMatches: (String) -> Boolean,
        valueOk: (String) -> Boolean = { true }
    ): String? {
        when (node) {
            is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                val dict = node as Map<String, Any?>
                for ((key, value) in dict) {
                    if (!keyMatches(key.lowercase())) continue
                    if (value is String && value.isNotEmpty() && valueOk(value)) return value
                    if (value is Number && valueOk(value.toString())) return value.toString()
                }
                for ((_, value) in dict) {
                    deepFirstString(value, keyMatches, valueOk)?.let { return it }
                }
            }
            is List<*> -> {
                for (item in node) {
                    deepFirstString(item, keyMatches, valueOk)?.let { return it }
                }
            }
        }
        return null
    }

    /** 像快递单号：8-24 位字母数字且至少含 5 位数字（排除纯字母、日期、短码）。 */
    private fun isPlausibleTrackingNumber(s: String): Boolean {
        if (!s.matches(Regex("^[A-Za-z0-9]{8,24}$"))) return false
        return s.count { it.isDigit() } >= 5
    }

    /** 从页面文本收集疑似快递单号：纯数字需 ≥12 位（避开手机号），或字母前缀运单。 */
    fun collectTrackingNumbers(text: String): List<String> {
        val out = linkedSetOf<String>()
        val patterns = listOf(
            Regex("""\b[A-Za-z]{2,4}\d{8,16}\b"""),
            Regex("""\b\d{12,19}\b""")
        )
        patterns.forEach { regex ->
            regex.findAll(text).forEach { out += it.value }
        }
        return out.toList()
    }

    /**
     * 深度遍历 MTOP data，收集 key 名含 pickup/code 且值像取件码的字符串，
     * 兼容服务端字段改名或嵌套层级漂移。值不像取件码的长文本再走一遍候选提取。
     */
    fun collectPickupCodeFields(node: Any?, into: MutableCollection<String>) {
        when (node) {
            is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                val dict = node as Map<String, Any?>
                for ((key, value) in dict) {
                    val lk = key.lowercase()
                    if ((lk.contains("pickup") || lk.contains("code")) && value is String) {
                        val trimmed = value.trim()
                        if (looksLikePickupCode(trimmed)) {
                            into += trimmed
                        } else if (lk.contains("pickup") && trimmed.matches(Regex("""\d{4,8}"""))) {
                            // pickup 系字段名本身提供上下文：纯数字也可能是取件码
                            into += trimmed
                        } else {
                            into += collectPickupCodeCandidates(trimmed)
                        }
                    }
                    collectPickupCodeFields(value, into)
                }
            }
            is List<*> -> {
                for (item in node) collectPickupCodeFields(item, into)
            }
        }
    }

    // MARK: - 取件码识别

    /** 规范化取件码列表：去空白 → 去除 XNST/FAIL_BIZ_ 前缀 → 去标点 → 去重 → 取 8 个。 */
    fun normalizePickupCodes(raw: List<String>): List<String> {
        val punctuation = charArrayOf(',', '，', ';', '；', '。', '.', ':', '：')
        return raw.map { it.trim() }
            .filter { it.isNotBlank() }
            .filterNot { it.startsWith("XNST", ignoreCase = true) }
            .filterNot { it.startsWith("FAIL_BIZ_", ignoreCase = true) }
            .map { it.trim(*punctuation) }
            .filter { it.isNotBlank() }
            .distinct()
            .take(8)
    }

    /** 从文本中收集取件码候选。 */
    fun collectPickupCodeCandidates(text: String): List<String> {
        val out = linkedSetOf<String>()
        val patterns = listOf(
            // (pattern, 取件码所在捕获组)
            Pair(Regex("""(取件码|校验码)\s*[:：]?\s*([A-Za-z0-9-]{4,20})"""), 2),
            Pair(Regex("""([A-Za-z0-9-]{4,20})\s*[:：]?\s*(取件码|校验码)"""), 1),
            // 货架号格式如 1-2-3004 / 19-2-0075（首段可能只有 1 位）
            Pair(Regex("""\b\d{1,4}-\d{1,3}-\d{3,6}\b"""), 0),
        )
        for ((regex, group) in patterns) {
            for (m in regex.findAll(text)) {
                val token = if (group == 0) m.value else m.groupValues.getOrNull(group).orEmpty()
                if (token.isNotEmpty()) out += token
            }
        }
        // 额外：匹配独立看起来像取件码的 token
        Regex("""\b[A-Za-z0-9-]{4,20}\b""").findAll(text).forEach { m ->
            if (looksLikePickupCode(m.value)) out += m.value
        }
        return out.toList()
    }

    /**
     * 判断一个 token 是否像取件码。
     *
     * 必须带 `-`：纯数字一律不认（页面里的手机尾号、时间、日期碎片太多，
     * 4-6 位裸数字误报严重）。纯数字取件码只在「取件码：」关键字上下文
     * 或 MTOP pickup 系字段中才被采纳。
     */
    fun looksLikePickupCode(token: String): Boolean {
        // 货架号：数字三段（1-2-3004、19-2-0075）
        if (token.matches(Regex("""\d{1,4}-\d{1,3}-\d{3,6}"""))) return true
        // 其余形态必须含字母和 -，排除纯数字串（日期 2026-10、电话片段 138-1234）
        if (!token.contains("-")) return false
        if (!token.matches(Regex("""^[A-Za-z0-9-]+$"""))) return false
        if (!token.matches(Regex(""".*[A-Za-z].*"""))) return false
        // 字母数字两段短码（A-1024、B2-33）
        if (token.matches(Regex("""^[A-Za-z0-9]{2,6}-[A-Za-z0-9]{2,6}$"""))) return true
        // 含 - 的 5-16 位字母数字码
        return token.matches(Regex("""^[A-Za-z0-9-]{5,16}$"""))
    }

    /** 提取待取件数：'待取件 5' / '5 个待取件' / '5 个待取'。 */
    fun extractPendingCount(text: String): Int? {
        val patterns = listOf(
            Regex("""待取件[^0-9]*(\d+)"""),
            Regex("""(\d+)[^0-9]*待取件"""),
            Regex("""(\d+)[^0-9]*个待取"""),
        )
        for (regex in patterns) {
            regex.find(text)?.let { m ->
                m.groupValues.getOrNull(1)?.toIntOrNull()?.let { return it }
            }
        }
        return null
    }
}
