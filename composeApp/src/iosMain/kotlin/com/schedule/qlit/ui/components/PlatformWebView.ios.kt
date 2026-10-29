package com.schedule.qlit.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSError
import platform.Foundation.NSURLRequest
import platform.Foundation.NSURL
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.WebKit.WKWebsiteDataStore
import platform.darwin.NSObject

/** 导航回调桥；navigationDelegate 是弱引用，由 controller 强持有。 */
internal class WebNavDelegate : NSObject(), WKNavigationDelegateProtocol {
    var onProgress: (Int) -> Unit = {}
    var onStarted: (String?) -> Unit = {}
    var onFinished: (String?) -> Unit = {}

    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, didStartProvisionalNavigation: WKNavigation?) {
        onProgress(10)
        onStarted(webView.URL?.absoluteString)
    }

    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, didCommitNavigation: WKNavigation?) {
        onProgress(60)
    }

    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
        onProgress(100)
        onFinished(webView.URL?.absoluteString)
    }

    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, didFailNavigation: WKNavigation?, withError: NSError) {
        onProgress(100)
        onFinished(webView.URL?.absoluteString)
    }

    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, didFailProvisionalNavigation: WKNavigation?, withError: NSError) {
        onProgress(100)
        onFinished(webView.URL?.absoluteString)
    }
}

actual class PlatformWebViewController internal constructor(
    internal val webView: WKWebView,
    internal val delegate: WebNavDelegate,
) {
    actual fun reload() {
        webView.reload()
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun InteractiveWebView(
    url: String,
    userAgent: String?,
    modifier: Modifier,
    onProgressChanged: (Int) -> Unit,
    onPageStarted: (url: String?) -> Unit,
    onPageFinished: (url: String?) -> Unit,
    onCreated: (PlatformWebViewController) -> Unit,
    onReleased: () -> Unit,
) {
    val delegate = remember { WebNavDelegate() }
    delegate.onProgress = onProgressChanged
    delegate.onStarted = onPageStarted
    delegate.onFinished = onPageFinished

    UIKitView(
        modifier = modifier,
        factory = {
            val config = WKWebViewConfiguration()
            // 与 webViewCookie/setWebViewCookie 桥读写同一个默认数据存储
            config.websiteDataStore = WKWebsiteDataStore.defaultDataStore()
            config.defaultWebpagePreferences.allowsContentJavaScript = true
            val webView = WKWebView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0), configuration = config)
            userAgent?.let { webView.customUserAgent = it }
            webView.allowsBackForwardNavigationGestures = false
            webView.navigationDelegate = delegate
            NSURL(string = url)?.let {
                webView.loadRequest(NSURLRequest.requestWithURL(it))
            }
            onCreated(PlatformWebViewController(webView, delegate))
            webView
        },
        onRelease = { webView ->
            webView.stopLoading()
            webView.navigationDelegate = null
            onReleased()
        },
    )
}
