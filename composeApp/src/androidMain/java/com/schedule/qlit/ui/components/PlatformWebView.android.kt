package com.schedule.qlit.ui.components

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

actual class PlatformWebViewController internal constructor(
    private val webView: WebView,
) {
    actual fun reload() = webView.reload()
}

@SuppressLint("SetJavaScriptEnabled")
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
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply web@{
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.builtInZoomControls = false
                settings.displayZoomControls = false
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                userAgent?.let { settings.userAgentString = it }

                CookieManager.getInstance().apply {
                    setAcceptCookie(true)
                    setAcceptThirdPartyCookies(this@web, true)
                    flush()
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        onProgressChanged(newProgress)
                    }
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, pageUrl: String?, favicon: android.graphics.Bitmap?) {
                        onPageStarted(pageUrl)
                    }

                    override fun onPageFinished(view: WebView?, pageUrl: String?) {
                        CookieManager.getInstance().flush()
                        onPageFinished(pageUrl)
                    }
                }

                loadUrl(url)
                onCreated(PlatformWebViewController(this))
            }
        },
        onRelease = { view ->
            view.stopLoading()
            view.destroy()
            onReleased()
        },
    )
}
