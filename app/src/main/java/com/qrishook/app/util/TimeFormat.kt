package com.qrishook.app.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

private val dateTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.forLanguageTag("id-ID"))

fun formatDateTime(timestamp: Long): String =
    Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(dateTimeFormatter)

fun formatRelativeTime(timestamp: Long, now: Long = System.currentTimeMillis()): String {
    val minutes = max(0, now - timestamp) / 60_000
    return when {
        minutes < 1 -> "baru saja"
        minutes < 60 -> "$minutes menit lalu"
        minutes < 24 * 60 -> "${minutes / 60} jam lalu"
        minutes < 7 * 24 * 60 -> "${minutes / (24 * 60)} hari lalu"
        else -> formatDateTime(timestamp)
    }
}
