package com.qrishook.app.domain.webhook

import com.qrishook.app.data.model.HttpMethod
import com.qrishook.app.data.model.KeyValue
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Satu-satunya jalur HTTP aplikasi. Retry 3x (1s, 2s, 4s) hanya untuk network error dan 5xx.
 * Blocking call dijalankan di Dispatchers.IO — aman dipanggil dari Main.
 */
class OkHttpWebhookClient(
    private val client: OkHttpClient,
) : WebhookClient {

    override suspend fun send(
        url: String,
        method: HttpMethod,
        headers: List<KeyValue>,
        body: String,
    ): WebhookResult = withContext(Dispatchers.IO) {
        var attempt = 0
        var result = executeOnce(url, method, headers, body)
        while (isRetryable(result) && attempt < RETRY_DELAYS_MS.size) {
            delay(RETRY_DELAYS_MS[attempt])
            attempt++
            result = executeOnce(url, method, headers, body)
        }
        result
    }

    private fun isRetryable(result: WebhookResult): Boolean =
        result is WebhookResult.NetworkError ||
            (result is WebhookResult.Failed && result.httpCode >= 500)

    private fun executeOnce(
        url: String,
        method: HttpMethod,
        headers: List<KeyValue>,
        body: String,
    ): WebhookResult {
        val base = url.toHttpUrlOrNull()
            ?: return WebhookResult.Failed(0, "", "URL tidak valid")

        val requestUrl = if (method == HttpMethod.GET && body.isNotBlank()) {
            base.newBuilder().apply { appendQueryFromBody(body) }
        } else {
            base.newBuilder()
        }

        val requestBuilder = Request.Builder().url(requestUrl.build())
        headers.filter { it.key.isNotBlank() }.forEach {
            requestBuilder.addHeader(it.key.trim(), it.value)
        }
        when (method) {
            HttpMethod.POST -> requestBuilder.post(body.toRequestBody(JSON_MEDIA_TYPE))
            HttpMethod.GET -> requestBuilder.get()
        }

        val startedAt = System.nanoTime()
        return try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                val responseBody = response.body?.string().orEmpty().take(MAX_BODY_CHARS)
                val durationMs = (System.nanoTime() - startedAt) / 1_000_000
                if (response.isSuccessful) {
                    WebhookResult.Success(response.code, responseBody, durationMs)
                } else {
                    WebhookResult.Failed(response.code, responseBody, response.message)
                }
            }
        } catch (e: IOException) {
            WebhookResult.NetworkError(e.message ?: "Koneksi gagal")
        }
    }

    private fun HttpUrl.Builder.appendQueryFromBody(body: String) {
        body.trim().removePrefix("?").split("&").filter { it.isNotBlank() }.forEach { pair ->
            val separator = pair.indexOf('=')
            if (separator >= 0) {
                addQueryParameter(pair.substring(0, separator), pair.substring(separator + 1))
            } else {
                addQueryParameter(pair, "")
            }
        }
    }

    companion object {
        val RETRY_DELAYS_MS = listOf(1_000L, 2_000L, 4_000L)
        private const val MAX_BODY_CHARS = 4_000
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
