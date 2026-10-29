package com.schedule.qlit.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.schedule.qlit.data.debugLog
import com.schedule.qlit.data.platformWebUserAgent
import com.schedule.qlit.data.repository.TaobaoHomeRepository
import com.schedule.qlit.data.repository.parseJsonMap
import com.schedule.qlit.data.repository.refreshWidgets
import com.schedule.qlit.data.repository.saveTaobaoToWidgetCache
import com.schedule.qlit.data.repository.toJsonElement
import com.schedule.qlit.data.store.kvStore
import com.schedule.qlit.ui.components.AdaptiveSnackbarHost
import com.schedule.qlit.ui.components.Code128Barcode
import com.schedule.qlit.ui.components.InteractiveWebView
import com.schedule.qlit.ui.components.PlatformWebViewController
import com.schedule.qlit.ui.components.ScreenBrightnessBoost
import com.schedule.qlit.ui.components.rememberAdaptiveSnackbarHostState
import com.schedule.qlit.ui.design.QActionRow
import com.schedule.qlit.ui.design.QButton
import com.schedule.qlit.ui.design.QButtonKind
import com.schedule.qlit.ui.design.QCard
import com.schedule.qlit.ui.design.QChip
import com.schedule.qlit.ui.design.QEmpty
import com.schedule.qlit.ui.design.QIconButton
import com.schedule.qlit.ui.design.QIcons
import com.schedule.qlit.ui.design.QInfoLine
import com.schedule.qlit.ui.design.QList
import com.schedule.qlit.ui.design.QListShape
import com.schedule.qlit.ui.design.QLoading
import com.schedule.qlit.ui.design.QNotice
import com.schedule.qlit.ui.design.QPage
import com.schedule.qlit.ui.design.QProgressBar
import com.schedule.qlit.ui.design.QSheet
import com.schedule.qlit.ui.design.QSwitchRow
import com.schedule.qlit.ui.design.QText
import com.schedule.qlit.ui.design.QTintIcon
import com.schedule.qlit.ui.design.qBottomInset
import com.schedule.qlit.ui.design.qClickable
import com.schedule.qlit.ui.theme.Q
import com.schedule.qlit.ui.theme.QType
import com.schedule.qlit.ui.theme.QlitHues
import io.ktor.http.Url
import kotlinx.coroutines.delay
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon

private const val TAOBAO_IDENTITY_BOOST_BRIGHTNESS_KEY = "identity_boost_brightness"

/** 默认 UA；请求 mtop 时与 WebView 版保持一致，避免 UA 指纹差异触发风控。 */
private val defaultUa get() = platformWebUserAgent

private fun hostOf(url: String): String =
    runCatching { Url(url).host }.getOrDefault("")

/** 数据层写进 stationHint 的状态文案；不是驿站名，摘要卡上不显示。 */
private val TAOBAO_STATUS_HINTS = setOf(
    "身份码已就绪",
    "已登录淘宝取件",
    "正在同步淘宝取件数据…",
    "淘宝校园取件首页",
)

private data class TaobaoPickupSnapshot(
    val pageTitle: String,
    val pageUrl: String,
    val stationHint: String,
    val pendingCount: Int?,
    val pickupCodes: List<String>,
    val packages: List<TaobaoHomeRepository.TaobaoPackage>,
    val highlightLines: List<String>,
    val rawSnippet: String,
    val updatedAtText: String,
    val userId: String = "",
    val loginDetected: Boolean = false
)

private data class TaobaoIdentityInfo(
    val identityCode: String,
    val maskedPhone: String,
    val accountName: String,
    val updatedAtText: String
)

private fun nowText(): String {
    val t = kotlin.time.Clock.System.now()
        .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
    return "${t.hour.toString().padStart(2, '0')}:${t.minute.toString().padStart(2, '0')}:${t.second.toString().padStart(2, '0')}"
}

/** 在响应树里找第一个 stationName 或 station.text（兜底驿站名）。 */
private fun deepStationName(node: Any?): String {
    when (node) {
        is Map<*, *> -> {
            (node["stationName"] as? String)?.takeIf { it.isNotBlank() }?.let { return it }
            ((node["station"] as? Map<*, *>)?.get("text") as? String)
                ?.takeIf { it.isNotBlank() }?.let { return it }
            for (v in node.values) {
                val r = deepStationName(v)
                if (r.isNotBlank()) return r
            }
        }
        is List<*> -> {
            for (v in node) {
                val r = deepStationName(v)
                if (r.isNotBlank()) return r
            }
        }
    }
    return ""
}

/**
 * WebView 兜底模式。
 * null：无头路径，页面上没有 WebView；
 * LOGIN/CHALLENGE：显示说明卡，点按钮后在底部弹窗里打开淘宝网页，让用户登录或过人机校验。
 */
private enum class TaobaoWebFallback { LOGIN, CHALLENGE }

@Composable
fun TaobaoPickupScreen(onBack: () -> Unit) {
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = rememberAdaptiveSnackbarHostState()
    val prefs = remember { kvStore("taobao_pickup_prefs") }

    // —— 无头路径状态 ——
    var webUserAgent by remember { mutableStateOf("") }
    var identityInfo by remember { mutableStateOf<TaobaoIdentityInfo?>(null) }
    var showIdentityDialog by remember { mutableStateOf(false) }
    var identityLoading by remember { mutableStateOf(false) }
    var snapshot by remember { mutableStateOf<TaobaoPickupSnapshot?>(null) }
    var boostBrightnessOnIdentity by remember {
        mutableStateOf(prefs.getBoolean(TAOBAO_IDENTITY_BOOST_BRIGHTNESS_KEY, false))
    }
    var syncing by remember { mutableStateOf(false) }
    var syncGeneration by remember { mutableIntStateOf(0) }
    var lastSyncError by remember { mutableStateOf<String?>(null) }

    // —— WebView 兜底状态（只在需要登录/风控时创建） ——
    var webFallback by remember { mutableStateOf<TaobaoWebFallback?>(null) }
    var showWebSheet by remember { mutableStateOf(false) }
    var webSheetMode by remember { mutableStateOf(TaobaoWebFallback.LOGIN) }
    var webViewInstance by remember { mutableStateOf<PlatformWebViewController?>(null) }
    var webPageHost by remember { mutableStateOf("") }
    var pageProgress by remember { mutableIntStateOf(0) }
    var isPageLoading by remember { mutableStateOf(false) }

    fun upsertSnapshot(transform: (TaobaoPickupSnapshot?) -> TaobaoPickupSnapshot) {
        snapshot = transform(snapshot)
    }

    fun persistHomeSummary() {
        prefs.putBoolean("home_login_detected", snapshot?.loginDetected == true)
        prefs.putInt("home_pending_count", snapshot?.pendingCount ?: -1)
        prefs.putString("home_station_hint", snapshot?.stationHint.orEmpty())
        prefs.putString("home_updated_at", snapshot?.updatedAtText.orEmpty())
        prefs.putBoolean("home_identity_ready", identityInfo != null)
        saveTaobaoToWidgetCache(
            loggedIn = snapshot?.loginDetected == true,
            pendingCount = snapshot?.pendingCount ?: -1,
        )
        refreshWidgets()
    }

    /** [json] 是已去掉 jsonp 外壳的 JSON 文本（来自 MtopResult.json），不要再次拆包。 */
    fun parseApiResponse(apiUrl: String, json: String) {
        @Suppress("UNCHECKED_CAST")
        val root = runCatching {
            parseJsonMap(json)
        }.getOrNull() ?: return

        @Suppress("UNCHECKED_CAST")
        val outerData = root["data"] as? Map<String, Any?> ?: emptyMap()
        @Suppress("UNCHECKED_CAST")
        val innerData = outerData["data"] as? Map<String, Any?> ?: emptyMap()
        val retText = (root["ret"] as? List<*>)
            ?.joinToString(" ") { it.toString() }
            .orEmpty()

        when {
            apiUrl.contains("mtop.cainiao.pickup.identity.getIdentityMessage", ignoreCase = true) -> {
                val accountName = innerData["accountName"]?.toString().orEmpty()
                val identityCode = innerData["identityCode"]?.toString().orEmpty()
                val maskedPhone = innerData["phone"]?.toString().orEmpty()
                if (identityCode.isNotBlank()) {
                    identityInfo = TaobaoIdentityInfo(
                        identityCode = identityCode,
                        maskedPhone = maskedPhone,
                        accountName = accountName,
                        updatedAtText = nowText()
                    )
                    identityLoading = false
                    upsertSnapshot { old ->
                        old?.copy(
                            stationHint = old.stationHint.ifBlank { "身份码已就绪" },
                            highlightLines = (listOfNotNull(
                                maskedPhone.takeIf { it.isNotBlank() },
                                accountName.takeIf { it.isNotBlank() }
                            ) + old.highlightLines).distinct().take(12),
                            rawSnippet = json.take(1200),
                            updatedAtText = nowText(),
                            loginDetected = true
                        ) ?: TaobaoPickupSnapshot(
                            pageTitle = "",
                            pageUrl = "",
                            stationHint = "身份码已就绪",
                            pendingCount = null,
                            pickupCodes = emptyList(),
                            packages = emptyList(),
                            highlightLines = listOfNotNull(
                                maskedPhone.takeIf { it.isNotBlank() },
                                accountName.takeIf { it.isNotBlank() }
                            ),
                            rawSnippet = json.take(1200),
                            updatedAtText = nowText(),
                            loginDetected = true
                        )
                    }
                }
            }

            apiUrl.contains("mtop.cainiao.pickup.productType.preCheck", ignoreCase = true) -> {
                // userId 可能在 data.userId 或 data.data.userId（接口层级有过漂移）
                val userId = innerData["userId"]?.toString()
                    ?: outerData["userId"]?.toString()
                    ?: ""
                upsertSnapshot { old ->
                    old?.copy(
                        stationHint = old.stationHint.ifBlank { "已登录淘宝取件" },
                        updatedAtText = nowText(),
                        userId = userId,
                        loginDetected = userId.isNotBlank() || old.loginDetected
                    ) ?: TaobaoPickupSnapshot(
                        pageTitle = "",
                        pageUrl = "",
                        stationHint = "已登录淘宝取件",
                        pendingCount = null,
                        pickupCodes = emptyList(),
                        packages = emptyList(),
                        highlightLines = emptyList(),
                        rawSnippet = "",
                        updatedAtText = nowText(),
                        userId = userId,
                        loginDetected = userId.isNotBlank()
                    )
                }
            }

            apiUrl.contains("mtop.cainiao.pickup.plus.queryMultiStaPackages4Xy", ignoreCase = true) -> {
                // 递归结构化解析：staPkgAssembles 可能是按驿站分组再嵌套包裹数组
                var totalCount = 0
                val packages = mutableListOf<TaobaoHomeRepository.TaobaoPackage>()
                for (listKey in listOf("homeBarList", "staPkgAssembles")) {
                    val node = innerData[listKey] ?: continue
                    when (node) {
                        is List<*> -> {
                            totalCount += node.size
                            node.forEach { packages += TaobaoHomeRepository.parsePackagesFromNode(it) }
                        }
                        is Map<*, *> -> {
                            packages += TaobaoHomeRepository.parsePackagesFromNode(node)
                        }
                    }
                }

                // 深度收集 pickup/code 系字段的取件码（包裹里没挖到的兜底）
                val codes = linkedSetOf<String>()
                TaobaoHomeRepository.collectPickupCodeFields(innerData, codes)

                // 包裹列表里的取件码也并进独立 codes 集合（确保 pickupCodes 与 packages 对齐）
                packages.forEach { if (it.pickupCode.isNotEmpty()) codes += it.pickupCode }

                // 深扫出来的码若没对应包裹，补一条裸码包裹，保证每个码都有一行
                val covered = packages.map { it.pickupCode }.toSet()
                val normalizedCodes = TaobaoHomeRepository.normalizePickupCodes(codes.toList())
                normalizedCodes.forEach { code ->
                    if (code !in covered) {
                        packages += TaobaoHomeRepository.TaobaoPackage(pickupCode = code)
                    }
                }

                val stationHint = packages.firstOrNull { it.station.isNotEmpty() }?.station
                    ?: deepStationName(innerData)

                val highlightLines = packages.mapNotNull { pkg ->
                    listOfNotNull(
                        pkg.carrier.takeIf { it.isNotBlank() },
                        pkg.trackingNumber.takeIf { it.isNotBlank() },
                        pkg.station.takeIf { it.isNotBlank() },
                    ).joinToString(" · ").takeIf { it.isNotBlank() }
                }.distinct()

                upsertSnapshot { old ->
                    val isSuccess = retText.contains("SUCCESS", ignoreCase = true)
                    val hasPackages = packages.isNotEmpty()
                    val finalCount = when {
                        totalCount > 0 -> totalCount
                        hasPackages -> packages.size
                        isSuccess -> 0  // 接口成功但空列表 = 0 件，不是 null
                        else -> old?.pendingCount
                    }
                    old?.copy(
                        stationHint = old.stationHint.ifBlank {
                            stationHint.ifBlank { "淘宝校园取件首页" }
                        },
                        pendingCount = finalCount,
                        pickupCodes = normalizedCodes.ifEmpty { old.pickupCodes },
                        packages = packages.ifEmpty { old.packages },
                        highlightLines = (highlightLines + old.highlightLines).distinct().take(12),
                        rawSnippet = innerData.toJsonElement().toString().take(1200),
                        updatedAtText = nowText(),
                        // needLogin=true 的接口能返回成功即算登录证据
                        loginDetected = isSuccess || hasPackages || old.loginDetected
                    ) ?: TaobaoPickupSnapshot(
                        pageTitle = "",
                        pageUrl = "",
                        stationHint = stationHint.ifBlank { "淘宝校园取件首页" },
                        pendingCount = finalCount,
                        pickupCodes = normalizedCodes,
                        packages = packages,
                        highlightLines = highlightLines,
                        rawSnippet = innerData.toJsonElement().toString().take(1200),
                        updatedAtText = nowText(),
                        loginDetected = isSuccess || hasPackages
                    )
                }
            }
        }
    }

    /**
     * 处理一次 mtop 结果的非成功分支：
     * 登录失效/风控时切到登录/校验说明卡，其它失败交给 [onFailure]（默认 snackbar 提示）。
     * 返回 true 表示已经消费（成功或已处理），false 表示发生了需要 WebView 介入的错误。
     */
    fun handleMtopOutcome(
        result: TaobaoHomeRepository.MtopResult,
        sourceDesc: String,
        onFailure: (String) -> Unit = { message -> scope.launch { snackbarHostState.showSnackbar(message) } },
    ): Boolean {
        if (result.status != TaobaoHomeRepository.MtopStatus.SUCCESS) {
            debugLog("Taobao", "mtop ${sourceDesc} → ${result.status} ret=${result.retText.take(80)}")
        }
        return when (result.status) {
            TaobaoHomeRepository.MtopStatus.SUCCESS -> {
                // 无头链路一旦通了就说明登录/风控已解决：收起说明卡，网页弹窗随之自动关闭
                if (webFallback != null && snapshot?.loginDetected == true) {
                    val sheetWasOpen = showWebSheet
                    webFallback = null
                    showWebSheet = false
                    if (sheetWasOpen) {
                        scope.launch { snackbarHostState.showSnackbar("已登录，正在同步包裹") }
                    }
                }
                true
            }
            TaobaoHomeRepository.MtopStatus.SESSION_EXPIRED -> {
                webFallback = TaobaoWebFallback.LOGIN
                false
            }
            TaobaoHomeRepository.MtopStatus.CHALLENGED -> {
                webFallback = TaobaoWebFallback.CHALLENGE
                false
            }
            TaobaoHomeRepository.MtopStatus.TOKEN_EXPIRED,
            TaobaoHomeRepository.MtopStatus.FAILED -> {
                onFailure(result.retText.ifBlank { "$sourceDesc 失败" })
                false
            }
        }
    }

    fun refreshNativePickupData() {
        val ua = webUserAgent.ifBlank { defaultUa }
        val traceId = TaobaoHomeRepository.buildTraceId()
        val generation = ++syncGeneration
        syncing = true
        lastSyncError = null
        upsertSnapshot { old ->
            // 刷新时清掉旧数据但保留 loginDetected/userId（重新判登录态前需要 preCheck 结果）
            TaobaoPickupSnapshot(
                pageTitle = old?.pageTitle.orEmpty(),
                pageUrl = old?.pageUrl.orEmpty(),
                stationHint = old?.stationHint?.ifBlank { "正在同步淘宝取件数据…" } ?: "正在同步淘宝取件数据…",
                pendingCount = null,
                pickupCodes = emptyList(),
                packages = emptyList(),
                highlightLines = emptyList(),
                rawSnippet = "",
                updatedAtText = nowText(),
                userId = old?.userId.orEmpty(),
                loginDetected = old?.loginDetected == true
            )
        }
        scope.launch {
            try {
                val preCheck = runCatching {
                    val preCheckData = linkedMapOf(
                        "stationType" to "XY",
                        "source" to "",
                        "traceId" to traceId,
                        "accessSource" to "",
                        "subSource" to "",
                        "productType" to "XY"
                    ).toJsonElement().toString()
                    TaobaoHomeRepository.requestMtop(
                        api = "mtop.cainiao.pickup.productType.preCheck",
                        dataJson = preCheckData,
                        referer = TaobaoHomeRepository.PICKUP_URL,
                        userAgent = ua,
                        callback = "mtopjsonp_native"
                    )
                }.getOrElse {
                    lastSyncError = it.message ?: "网络异常"
                    return@launch
                }
                if (!handleMtopOutcome(preCheck, "登录校验", onFailure = { lastSyncError = it })) return@launch
                preCheck.json?.let { parseApiResponse("mtop.cainiao.pickup.productType.preCheck", it) }

                val home = runCatching {
                    val data = linkedMapOf(
                        "stationType" to "XY",
                        "source" to "",
                        "traceId" to traceId
                    ).toJsonElement().toString()
                    TaobaoHomeRepository.requestMtop(
                        api = "mtop.cainiao.pickup.plus.queryMultiStaPackages4Xy",
                        dataJson = data,
                        referer = TaobaoHomeRepository.PICKUP_URL,
                        userAgent = ua,
                        callback = "mtopjsonp_native"
                    )
                }.getOrElse {
                    lastSyncError = it.message ?: "网络异常"
                    return@launch
                }
                if (!handleMtopOutcome(home, "取件列表", onFailure = { lastSyncError = it })) return@launch
                home.json?.let { parseApiResponse("mtop.cainiao.pickup.plus.queryMultiStaPackages4Xy", it) }
            } finally {
                if (generation == syncGeneration) syncing = false
            }
        }
    }

    fun requestIdentityCode(clearExisting: Boolean = false) {
        identityLoading = true
        if (clearExisting) {
            identityInfo = null
        }
        val ua = webUserAgent.ifBlank { defaultUa }
        val traceId = TaobaoHomeRepository.buildTraceId()
        scope.launch {
            runCatching {
                val data = linkedMapOf(
                    "stationType" to "XY",
                    "source" to "",
                    "traceId" to traceId
                ).toJsonElement().toString()
                TaobaoHomeRepository.requestMtop(
                    api = "mtop.cainiao.pickup.identity.getIdentityMessage",
                    dataJson = data,
                    referer = TaobaoHomeRepository.IDENTITY_URL,
                    userAgent = ua,
                    callback = "mtopjsonp_native"
                )
            }.onSuccess { result ->
                if (!handleMtopOutcome(result, "身份码")) {
                    identityLoading = false
                    // 需要登录/校验时收起身份码弹窗，让页面上的说明卡露出来
                    if (webFallback != null) showIdentityDialog = false
                } else {
                    result.json?.let { parseApiResponse("mtop.cainiao.pickup.identity.getIdentityMessage", it) }
                    identityLoading = false
                }
            }.onFailure {
                identityLoading = false
                snackbarHostState.showSnackbar(it.message ?: "获取身份码失败，请先登录淘宝")
            }
        }
    }

    fun copyPickupCode(code: String) {
        clipboardManager.setText(AnnotatedString(code))
        scope.launch { snackbarHostState.showSnackbar("取件码 $code 已复制") }
    }

    fun openIdentitySheet() {
        showIdentityDialog = true
        requestIdentityCode(clearExisting = identityInfo == null)
    }

    ScreenBrightnessBoost(showIdentityDialog && boostBrightnessOnIdentity)

    LaunchedEffect(snapshot?.loginDetected, snapshot?.pendingCount, snapshot?.stationHint, snapshot?.updatedAtText, identityInfo?.identityCode) {
        persistHomeSummary()
    }

    // 进页面先走无头链路；只有返回 SESSION_EXPIRED/CHALLENGED 才会显示登录/校验说明卡。
    LaunchedEffect(Unit) {
        refreshNativePickupData()
    }

    val current = snapshot
    val pendingCount = current?.pendingCount
    val displayPackages = current?.packages.orEmpty().ifEmpty {
        current?.pickupCodes.orEmpty().map { TaobaoHomeRepository.TaobaoPackage(pickupCode = it) }
    }

    QPage(
        title = "淘宝取件",
        onBack = onBack,
        actions = {
            QIconButton(
                QIcons.Refresh,
                "刷新",
                onClick = { refreshNativePickupData() },
                enabled = !syncing,
                tint = Q.colors.ink,
            )
        },
        snackbarHost = { AdaptiveSnackbarHost(state = snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = innerPadding.calculateTopPadding() + 4.dp, bottom = qBottomInset()),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            val fallback = webFallback
            when {
                fallback != null -> {
                    item(key = "gate") {
                        PickupGateCard(
                            mode = fallback,
                            onOpenWeb = {
                                webSheetMode = fallback
                                showWebSheet = true
                            },
                        )
                    }
                    item(key = "gate_foot") {
                        QText(
                            "只读取驿站取件信息和身份码，不会下单或修改你的淘宝账号。",
                            modifier = Modifier.padding(horizontal = 24.dp),
                            style = QType.caption,
                            color = Q.colors.ink3,
                        )
                    }
                }

                syncing && pendingCount == null -> {
                    item(key = "skeleton") { PickupSkeleton() }
                }

                else -> {
                    if (lastSyncError != null && pendingCount == null) {
                        item(key = "error") {
                            QNotice(
                                title = "没能同步取件数据",
                                text = lastSyncError.orEmpty(),
                                modifier = Modifier.padding(horizontal = 16.dp),
                                action = {
                                    QText(
                                        "重试",
                                        modifier = Modifier
                                            .qClickable { refreshNativePickupData() }
                                            .padding(horizontal = 2.dp, vertical = 6.dp),
                                        style = QType.bodyStrong.copy(fontSize = 14.sp),
                                        color = Q.colors.seal,
                                    )
                                },
                            )
                        }
                    }
                    item(key = "summary") {
                        PickupSummaryCard(
                            count = pendingCount,
                            where = pickupWhereText(current, pendingCount, displayPackages),
                            updatedAt = current?.updatedAtText.orEmpty(),
                            onShowIdentity = { openIdentitySheet() },
                        )
                    }
                    if (displayPackages.isNotEmpty()) {
                        item(key = "packages") {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 6.dp),
                                    verticalAlignment = Alignment.Bottom,
                                ) {
                                    QText("待取包裹", modifier = Modifier.weight(1f), style = QType.label, color = Q.colors.ink2)
                                    QText("点卡片复制取件码", style = QType.sub, color = Q.colors.ink3)
                                }
                                displayPackages.forEach { pkg ->
                                    PickupPackageCard(pkg = pkg, onCopy = { copyPickupCode(it) })
                                }
                            }
                        }
                    } else if (pendingCount == 0) {
                        item(key = "empty") {
                            QEmpty(
                                text = "包裹到站后会出现在这里",
                                subtitle = "今天页的“取件”也会显示红点",
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                    } else if (pendingCount != null) {
                        item(key = "no_codes") {
                            QEmpty(
                                text = "没有识别到取件码",
                                subtitle = "可以出示身份码，让驿站扫码取件",
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    QSheet(
        show = showWebSheet && webFallback != null,
        onDismissRequest = { showWebSheet = false },
    ) {
        val login = webSheetMode == TaobaoWebFallback.LOGIN
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QText(if (login) "登录淘宝" else "人机校验", modifier = Modifier.weight(1f), style = QType.sheetTitle)
            // 重载会让页面里的风控组件重跑，滑块卡住时有用
            QIconButton(QIcons.Refresh, "重新加载", onClick = { webViewInstance?.reload() }, tint = Q.colors.ink, background = Q.colors.surface2)
            QIconButton(QIcons.Close, "收起", onClick = { showWebSheet = false }, tint = Q.colors.ink, background = Q.colors.surface2)
        }
        QText(
            (if (login) "在下面的淘宝页面里登录。" else "在下面完成滑块验证。") + "完成后这里会自动收起，并刷新包裹。",
            modifier = Modifier.padding(top = 6.dp),
            style = QType.sub,
            color = Q.colors.ink2,
        )
        val webHeight = LocalWindowInfo.current.containerDpSize.height * 0.66f
        Column(
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .weight(1f, fill = false)
                .height(webHeight)
                .clip(QListShape)
                .background(Q.colors.bg)
                .border(1.dp, Q.colors.line, QListShape),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(QIcons.Lock, contentDescription = null, modifier = Modifier.size(13.dp), tint = Q.colors.ink3)
                QText(
                    webPageHost.ifBlank { hostOf(TaobaoHomeRepository.PICKUP_URL) },
                    style = QType.caption,
                    color = Q.colors.ink3,
                    maxLines = 1,
                )
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Q.colors.line))
            if (isPageLoading) {
                QProgressBar(
                    progress = pageProgress / 100f,
                    height = 2.dp,
                    trackColor = Color.Transparent,
                )
            } else {
                Spacer(Modifier.height(2.dp))
            }
            InteractiveWebView(
                url = TaobaoHomeRepository.PICKUP_URL,
                userAgent = defaultUa,
                modifier = Modifier.fillMaxWidth().weight(1f),
                onProgressChanged = { pageProgress = it },
                onPageStarted = { pageUrl ->
                    isPageLoading = true
                    pageUrl?.let { webPageHost = hostOf(it) }
                },
                onPageFinished = {
                    isPageLoading = false
                    // 登录回跳后 cookie/_m_h5_tk 才最新，自动拉一次无头数据；
                    // 成功后说明卡与弹窗自动收起，不依赖手动刷新。
                    scope.launch {
                        delay(500)  // 让 Set-Cookie 先落到 cookie 存储
                        refreshNativePickupData()
                    }
                },
                onCreated = { controller ->
                    webViewInstance = controller
                    webUserAgent = defaultUa
                },
                onReleased = {
                    webViewInstance = null
                    isPageLoading = false
                    webPageHost = ""
                },
            )
        }
    }

    QSheet(
        show = showIdentityDialog,
        onDismissRequest = { showIdentityDialog = false },
    ) {
        IdentitySheetContent(
            info = identityInfo,
            isRefreshing = identityLoading,
            boostBrightness = boostBrightnessOnIdentity,
            onBoostBrightnessChange = { checked ->
                boostBrightnessOnIdentity = checked
                prefs.putBoolean(TAOBAO_IDENTITY_BOOST_BRIGHTNESS_KEY, checked)
            },
            onRefresh = { requestIdentityCode(clearExisting = false) },
            onClose = { showIdentityDialog = false },
        )
    }
}

private fun pickupWhereText(
    snapshot: TaobaoPickupSnapshot?,
    count: Int?,
    packages: List<TaobaoHomeRepository.TaobaoPackage>,
): String = when {
    count == null -> if (snapshot?.loginDetected == true) "已登录淘宝取件，包裹数据暂未同步" else "还没有同步到包裹数据"
    count == 0 -> "已登录淘宝取件，暂时没有到站的包裹"
    else -> packages.map { it.station }.filter { it.isNotBlank() }.distinct().joinToString(" / ").ifBlank {
        snapshot?.stationHint?.takeIf { it.isNotBlank() && it !in TAOBAO_STATUS_HINTS } ?: "淘宝校园取件"
    }
}

@Composable
private fun PickupSummaryCard(
    count: Int?,
    where: String,
    updatedAt: String,
    onShowIdentity: () -> Unit,
) {
    QCard(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            QText("待取件", modifier = Modifier.weight(1f), style = QType.sub, color = Q.colors.ink2)
            if (updatedAt.isNotBlank()) {
                QText("更新于 ", style = QType.caption, color = Q.colors.ink3)
                QText(updatedAt.take(5), style = QType.num(13.sp, FontWeight.Medium), color = Q.colors.ink3)
            }
        }
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 4.dp)) {
            QText(
                count?.toString() ?: "--",
                style = QType.num(56.sp),
                color = when {
                    count == null -> Q.colors.ink3
                    count > 0 -> Q.colors.seal
                    else -> Q.colors.ink
                },
            )
            QText("件", modifier = Modifier.padding(start = 4.dp, bottom = 6.dp), style = QType.body, color = Q.colors.ink2)
        }
        QText(where, modifier = Modifier.padding(top = 8.dp), style = QType.sub, color = Q.colors.ink2)
        QButton(
            text = "出示身份码",
            onClick = onShowIdentity,
            modifier = Modifier.padding(top = 18.dp).fillMaxWidth(),
            kind = QButtonKind.Primary,
            icon = BarcodeIcon,
        )
    }
}

/** 一件包裹：取件码最大，点整张卡复制；快递公司/驿站在上，商品名和单号在下。 */
@Composable
private fun PickupPackageCard(
    pkg: TaobaoHomeRepository.TaobaoPackage,
    onCopy: (String) -> Unit,
) {
    val hasCode = pkg.pickupCode.isNotBlank()
    QCard(
        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
        shape = QListShape,
        onClick = if (hasCode) ({ onCopy(pkg.pickupCode) }) else null,
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 15.dp),
    ) {
        if (pkg.carrier.isNotBlank() || pkg.station.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (pkg.carrier.isNotBlank()) QChip(pkg.carrier, hue = QlitHues.PICKUP)
                if (pkg.station.isNotBlank()) {
                    QText(pkg.station, modifier = Modifier.weight(1f), style = QType.caption, color = Q.colors.ink3, maxLines = 1)
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (hasCode) {
                QText(
                    pkg.pickupCode,
                    modifier = Modifier.weight(1f),
                    style = QType.num(38.sp, FontWeight.Bold).copy(letterSpacing = 0.01.em),
                    maxLines = 1,
                )
                Icon(QIcons.Copy, contentDescription = "复制取件码", modifier = Modifier.size(20.dp), tint = Q.colors.ink3)
            } else {
                QText("取件码未识别", modifier = Modifier.weight(1f), style = QType.body, color = Q.colors.ink3)
            }
        }
        if (pkg.title.isNotBlank()) {
            QText(pkg.title, style = QType.body.copy(fontSize = 14.sp), color = Q.colors.ink2, maxLines = 1)
        }
        if (pkg.trackingNumber.isNotBlank()) {
            Row(modifier = Modifier.padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                QText("单号 ", style = QType.caption, color = Q.colors.ink3)
                QText(pkg.trackingNumber, style = QType.num(13.5.sp, FontWeight.Medium), color = Q.colors.ink3, maxLines = 1)
            }
        }
    }
}

@Composable
private fun PickupGateCard(mode: TaobaoWebFallback, onOpenWeb: () -> Unit) {
    val login = mode == TaobaoWebFallback.LOGIN
    QCard(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
        QTintIcon(
            icon = if (login) QIcons.Lock else QIcons.Warning,
            hue = QlitHues.PICKUP,
            size = 46.dp,
            iconSize = 24.dp,
            shape = RoundedCornerShape(14.dp),
        )
        QText(
            if (login) "需要登录淘宝" else "需要完成一次人机校验",
            modifier = Modifier.padding(top = 16.dp, bottom = 6.dp),
            style = QType.title.copy(fontSize = 21.sp, fontWeight = FontWeight.Bold),
        )
        QText(
            if (login) {
                "登录一次就会一直保持，之后打开这里就能直接看到驿站里的包裹。"
            } else {
                "淘宝要求先拖一下滑块确认是本人，完成后会自动回到列表。"
            },
            style = QType.body.copy(fontSize = 14.sp, lineHeight = 1.65.em),
            color = Q.colors.ink2,
        )
        QButton(
            text = if (login) "登录淘宝" else "去验证",
            onClick = onOpenWeb,
            modifier = Modifier.padding(top = 18.dp).fillMaxWidth(),
            kind = QButtonKind.Primary,
        )
    }
}

@Composable
private fun PickupSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        QCard(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
            SkeletonBlock(Modifier.width(64.dp).height(14.dp))
            SkeletonBlock(Modifier.padding(top = 12.dp).width(92.dp).height(52.dp), radius = 12.dp)
            SkeletonBlock(Modifier.padding(top = 12.dp).width(180.dp).height(14.dp))
            SkeletonBlock(Modifier.padding(top = 18.dp).fillMaxWidth().height(50.dp), radius = 16.dp)
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(2) {
                QCard(
                    modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                    shape = QListShape,
                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 15.dp),
                ) {
                    SkeletonBlock(Modifier.width(140.dp).height(14.dp))
                    SkeletonBlock(Modifier.padding(top = 12.dp).width(180.dp).height(36.dp), radius = 10.dp)
                    SkeletonBlock(Modifier.padding(top = 12.dp).width(220.dp).height(13.dp))
                }
            }
        }
    }
}

/** 骨架占位块：surface2 底色上扫过一道浅色高光。 */
@Composable
private fun SkeletonBlock(modifier: Modifier, radius: Dp = 8.dp) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val shift by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1400, easing = LinearEasing)),
        label = "skeletonShift",
    )
    val highlight = Color.White.copy(alpha = if (Q.colors.isDark) 0.06f else 0.35f)
    Box(
        modifier
            .clip(RoundedCornerShape(radius))
            .background(Q.colors.surface2)
            .drawBehind {
                val x = shift * size.width
                drawRect(
                    Brush.horizontalGradient(
                        0.3f to Color.Transparent,
                        0.5f to highlight,
                        0.7f to Color.Transparent,
                        startX = x,
                        endX = x + size.width,
                    ),
                )
            },
    )
}

@Composable
private fun IdentitySheetContent(
    info: TaobaoIdentityInfo?,
    isRefreshing: Boolean,
    boostBrightness: Boolean,
    onBoostBrightnessChange: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    onClose: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        QText("身份码", modifier = Modifier.weight(1f), style = QType.sheetTitle)
        if (info != null) QChip("已认证", hue = QlitHues.CAMPUS_RUN)
    }
    QText(
        "给驿站工作人员扫这个码，就能取走名下所有包裹。",
        modifier = Modifier.padding(top = 6.dp),
        style = QType.sub,
        color = Q.colors.ink2,
    )

    if (info == null) {
        Box(
            modifier = Modifier
                .padding(top = 18.dp)
                .fillMaxWidth()
                .height(150.dp)
                .clip(QListShape)
                .background(Q.colors.bg),
            contentAlignment = Alignment.Center,
        ) {
            if (isRefreshing) {
                QLoading(text = "正在获取身份码…")
            } else {
                QText("暂未获取到身份码，点刷新重试", style = QType.sub, color = Q.colors.ink3)
            }
        }
    } else {
        IdentityBarcodeCard(info.identityCode)
        Column(modifier = Modifier.padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (info.maskedPhone.isNotBlank()) {
                QInfoLine("手机号", info.maskedPhone, numeric = true, labelWidth = 76.dp)
            }
            if (info.accountName.isNotBlank()) {
                QInfoLine("账号", info.accountName, labelWidth = 76.dp)
            }
            QInfoLine("更新于", info.updatedAtText, numeric = true, labelWidth = 76.dp)
        }
    }

    QList(modifier = Modifier.padding(top = 16.dp), color = Q.colors.bg) {
        QSwitchRow(
            title = "扫码时调高亮度",
            subtitle = "打开身份码时临时调到最亮",
            checked = boostBrightness,
            onCheckedChange = onBoostBrightnessChange,
        )
    }
    QActionRow(
        confirmText = if (isRefreshing) "刷新中" else "刷新",
        onConfirm = onRefresh,
        dismissText = "关闭",
        onDismiss = onClose,
        loading = isRefreshing,
    )
}

/** 条形码固定画在白底黑条上（不跟随深色模式），否则扫码枪读不出来。 */
@Composable
private fun IdentityBarcodeCard(identityCode: String) {
    val cardShape = QListShape
    Column(
        modifier = Modifier
            .padding(top = 18.dp)
            .fillMaxWidth()
            .clip(cardShape)
            .background(Color.White)
            .border(1.dp, Color.Black.copy(alpha = 0.06f), cardShape)
            .padding(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Code128Barcode(
            code = identityCode,
            modifier = Modifier.fillMaxWidth().height(92.dp),
        )
        QText(
            identityCode.chunked(4).joinToString(" "),
            modifier = Modifier.padding(top = 10.dp),
            style = QType.num(22.sp).copy(letterSpacing = 0.12.em),
            color = Color(0xFF111111),
            maxLines = 1,
        )
    }
}

private val BarcodeIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "barcode",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        addPath(
            pathData = PathParser().parsePathString("M4 6v12M7 6v12M10.5 6v12M13 6v12M16.5 6v12M20 6v12").toNodes(),
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }.build()
}
