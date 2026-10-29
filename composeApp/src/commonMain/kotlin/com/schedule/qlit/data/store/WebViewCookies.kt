package com.schedule.qlit.data.store

/**
 * WebView cookie 仓库桥：Android 对应 CookieManager，iOS 对应
 * WKWebsiteDataStore 的 httpCookieStore。仓库侧只做读取/写回，
 * 登录流程本身仍在平台 UI 层。
 */
internal expect suspend fun webViewCookie(url: String): String?

internal expect suspend fun setWebViewCookie(url: String, cookie: String)

internal expect suspend fun flushWebViewCookies()
