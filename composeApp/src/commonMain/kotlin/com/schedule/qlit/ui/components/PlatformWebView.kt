package com.schedule.qlit.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** 交互式 WebView 的平台句柄（Android=WebView / iOS=WKWebView）。 */
expect class PlatformWebViewController {
    fun reload()
}

/**
 * 登录/风控兜底用的交互式 WebView。
 * cookie 落到平台共享存储（Android CookieManager / iOS 默认 WKWebsiteDataStore），
 * 与 `webViewCookie`/`setWebViewCookie` 桥读写的是同一份。
 */
@Composable
expect fun InteractiveWebView(
    url: String,
    userAgent: String?,
    modifier: Modifier = Modifier,
    onProgressChanged: (Int) -> Unit = {},
    onPageStarted: (url: String?) -> Unit = {},
    onPageFinished: (url: String?) -> Unit = {},
    onCreated: (PlatformWebViewController) -> Unit = {},
    onReleased: () -> Unit = {},
)
