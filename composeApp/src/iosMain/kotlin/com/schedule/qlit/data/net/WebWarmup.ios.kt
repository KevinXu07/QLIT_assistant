package com.schedule.qlit.data.net

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSURLRequest
import platform.Foundation.NSURL
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject
import platform.darwin.dispatch_after
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_time
import platform.darwin.DISPATCH_TIME_NOW
import platform.CoreGraphics.CGRectMake

private class WarmupDelegate(
    private val onFinished: () -> Unit,
) : NSObject(), WKNavigationDelegateProtocol {

    override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
        // WKHTTPCookieStore 持久化异步，留与 Android 一致的 ~800ms 缓冲
        dispatch_after(
            dispatch_time(DISPATCH_TIME_NOW, 800_000_000L),
            dispatch_get_main_queue(),
        ) { onFinished() }
    }
}

/** 预热期间的 delegate/webview 保活引用。 */
private var warmupHolder: Pair<WarmupDelegate, WKWebView>? = null

@OptIn(ExperimentalForeignApi::class)
internal actual fun warmupWebSession(url: String, onFinished: () -> Unit): () -> Unit {
    val delegate = WarmupDelegate {
        warmupHolder = null
        onFinished()
    }
    val config = WKWebViewConfiguration()
    val webView = WKWebView(frame = CGRectMake(0.0, 0.0, 1.0, 1.0), configuration = config)
    webView.navigationDelegate = delegate
    webView.customUserAgent = com.schedule.qlit.data.platformWebUserAgent
    warmupHolder = delegate to webView
    NSURL.URLWithString(url)?.let { webView.loadRequest(NSURLRequest.requestWithURL(it)) }
        ?: run { warmupHolder = null; onFinished() }
    return {
        webView.stopLoading()
        warmupHolder = null
    }
}

internal actual fun flushWebCookies() = Unit
