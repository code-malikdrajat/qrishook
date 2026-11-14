package com.qrishook.app.data.prefs

import com.qrishook.app.data.model.KeyValue
import kotlinx.serialization.Serializable

@Serializable
data class WebhookSecret(
    val url: String,
    val headers: List<KeyValue> = emptyList(),
)

interface SecurePrefs {
    fun saveWebhookSecret(automationId: Long, url: String, headers: List<KeyValue>)
    fun getWebhookSecret(automationId: Long): WebhookSecret?
    fun deleteWebhookSecret(automationId: Long)
}
