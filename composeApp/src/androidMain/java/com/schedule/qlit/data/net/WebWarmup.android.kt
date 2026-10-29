package com.schedule.qlit.data.net

import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.schedule.qlit.data.store.AppContext

internal actual fun warmupWebSession(url: String, onFinished: () -> Unit): () -> Unit {
    // 延迟回调记录下来，取消时移除，避免 WebView 销毁后旧回调仍触发
    val pendingFinishRunnable = Runnable { onFinished() }
    val warmupWebView = WebView(AppContext.context).apply warmup@{
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = false
            displayZoomControls = false
            cacheMode = WebSettings.LOAD_DEFAULT
            userAgentString =
                "Mozilla/5.0 (Linux; Android 15; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
        }
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(this@warmup, true)
            flush()
        }
        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                CookieManager.getInstance().flush()
                view?.postDelayed(pendingFinishRunnable, 800)
            }
        }
        loadUrl(url)
    }
    return {
        warmupWebView.removeCallbacks(pendingFinishRunnable)
        warmupWebView.stopLoading()
        warmupWebView.destroy()
    }
}

internal actual fun flushWebCookies() {
    CookieManager.getInstance().flush()
}
