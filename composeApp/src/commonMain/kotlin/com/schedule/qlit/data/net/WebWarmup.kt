package com.schedule.qlit.data.net

/**
 * 无头 WebView 预热：加载 [url] 让平台 cookie 库（CookieManager /
 * WKHTTPCookieStore）拿到会话 cookie，之后 [Http] 即可带会话请求。
 * 返回取消函数（页面销毁时调用）；加载完成（并留缓冲）后回调 onFinished。
 */
internal expect fun warmupWebSession(url: String, onFinished: () -> Unit): () -> Unit

/** 把 WebView 进程内 cookie 强制落盘。iOS WKHTTPCookieStore 自动持久化。 */
internal expect fun flushWebCookies()
