package com.schedule.qlit.data.store

import android.webkit.CookieManager

internal actual suspend fun webViewCookie(url: String): String? =
    CookieManager.getInstance().getCookie(url)

internal actual suspend fun setWebViewCookie(url: String, cookie: String) {
    CookieManager.getInstance().setCookie(url, cookie)
}

internal actual suspend fun flushWebViewCookies() {
    CookieManager.getInstance().flush()
}
