package com.schedule.qlit.data.store

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSHTTPCookie
import platform.Foundation.NSURL
import platform.WebKit.WKWebsiteDataStore
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.coroutines.resume

private val cookieStore
    get() = WKWebsiteDataStore.defaultDataStore().httpCookieStore

private fun domainMatches(cookieDomain: String, host: String): Boolean {
    val bare = cookieDomain.removePrefix(".")
    return host == bare || host.endsWith(".$bare")
}

internal actual suspend fun webViewCookie(url: String): String? =
    suspendCancellableCoroutine { cont ->
        // WebKit 首次初始化必须发生在主线程，后台线程首触会触发 WebKit 内部断言直接中止进程
        dispatch_async(dispatch_get_main_queue()) {
            val host = NSURL(string = url)?.host.orEmpty()
            cookieStore.getAllCookies { cookies ->
                val joined = cookies.orEmpty()
                    .mapNotNull { it as? NSHTTPCookie }
                    .filter { domainMatches(it.domain, host) }
                    .joinToString("; ") { "${it.name}=${it.value}" }
                cont.resume(joined.ifBlank { null })
            }
        }
    }

internal actual suspend fun setWebViewCookie(url: String, cookie: String) {
    val nsUrl = NSURL(string = url) ?: return
    val parsed = NSHTTPCookie.cookiesWithResponseHeaderFields(
        mapOf("Set-Cookie" to cookie), nsUrl
    )
    parsed.mapNotNull { it as? NSHTTPCookie }.forEach { httpCookie ->
        suspendCancellableCoroutine { cont ->
            dispatch_async(dispatch_get_main_queue()) {
                cookieStore.setCookie(httpCookie) { cont.resume(Unit) }
            }
        }
    }
}

internal actual suspend fun flushWebViewCookies() {
    // WKHTTPCookieStore 即写即生效，无需 flush
}
