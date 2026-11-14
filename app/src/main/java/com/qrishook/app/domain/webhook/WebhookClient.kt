package com.qrishook.app.domain.webhook

import com.qrishook.app.data.model.HttpMethod
import com.qrishook.app.data.model.KeyValue

sealed class WebhookResult {
    data class Success(val httpCode: Int, val body: String, val durationMs: Long) : WebhookResult()

    data class Failed(val httpCode: Int, val body: String, val error: String? = null) : WebhookResult()

    data class NetworkError(val message: String) : WebhookResult()
}

interface WebhookClient {
    suspend fun send(
        url: String,
        method: HttpMethod,
        headers: List<KeyValue>,
        body: String,
    ): WebhookResult
}
