package com.schedule.qlit.data.net

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.timeout
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.HttpMethod

/**
 * 各 Repository 共用的薄网络层（KMP 版，原 HttpURLConnection 封装的等价物）。
 *
 * 语义对齐旧实现：
 * - 不自动收集跨请求 cookie，Set-Cookie 由调用方从 [HttpResponse.headers] 自取；
 * - 4xx/5xx 不抛异常，响应体照常返回（调用方按 [HttpResponse.code] 分支）；
 * - gzip 交给引擎层透明处理（OkHttp/NSURLSession 都会自动声明并解压），
 *   调用方拿到的 [HttpResponse.body] 恒为明文。
 */
internal class HttpResponse(
    val code: Int,
    /** 原始响应头，保序保重名（Set-Cookie 等多值头逐条保留）。 */
    val headers: List<Pair<String, String>>,
    val body: ByteArray,
) {
    val ok: Boolean get() = code in 200..399

    fun text(): String = body.decodeToString()

    fun header(name: String): String? = headerValues(name).lastOrNull()

    fun headerValues(name: String): List<String> =
        headers.filter { it.first.equals(name, ignoreCase = true) }.map { it.second }
}

internal object Http {

    const val DEFAULT_TIMEOUT_MS = 15_000

    private val clientFollow by lazy { newClient(followRedirects = true) }
    private val clientNoFollow by lazy { newClient(followRedirects = false) }

    suspend fun get(
        url: String,
        headers: Map<String, String> = emptyMap(),
        followRedirects: Boolean = true,
        timeoutMs: Int = DEFAULT_TIMEOUT_MS,
    ): HttpResponse = request(HttpMethod.Get, url, null, headers, followRedirects, timeoutMs)

    suspend fun post(
        url: String,
        body: ByteArray,
        headers: Map<String, String> = emptyMap(),
        followRedirects: Boolean = true,
        timeoutMs: Int = DEFAULT_TIMEOUT_MS,
    ): HttpResponse = request(HttpMethod.Post, url, body, headers, followRedirects, timeoutMs)

    private suspend fun request(
        method: HttpMethod,
        url: String,
        body: ByteArray?,
        headers: Map<String, String>,
        followRedirects: Boolean,
        timeoutMs: Int,
    ): HttpResponse {
        val client = if (followRedirects) clientFollow else clientNoFollow
        val resp = client.request(url) {
            this.method = method
            timeout {
                requestTimeoutMillis = timeoutMs.toLong()
                connectTimeoutMillis = timeoutMs.toLong()
                socketTimeoutMillis = timeoutMs.toLong()
            }
            headers.forEach { (key, value) -> header(key, value) }
            if (body != null) setBody(body)
        }
        return HttpResponse(
            code = resp.status.value,
            headers = resp.headers.entries()
                .flatMap { (name, values) -> values.map { name to it } },
            body = resp.bodyAsBytes(),
        )
    }

    private fun newClient(followRedirects: Boolean) = HttpClient {
        expectSuccess = false
        this.followRedirects = followRedirects
        install(HttpTimeout)
    }
}
