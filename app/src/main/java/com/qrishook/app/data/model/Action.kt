package com.qrishook.app.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class HttpMethod { POST, GET }

@Serializable
data class KeyValue(
    val key: String = "",
    val value: String = "",
)

@Serializable
data class Action(
    val url: String = "",
    val method: HttpMethod = HttpMethod.POST,
    val headers: List<KeyValue> = listOf(KeyValue("Content-Type", "application/json")),
    val bodyTemplate: String = "",
)
