package com.qrishook.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 1 Automation = 1 Trigger. Disimpan sebagai JSON lewat [com.qrishook.app.data.db.Converters]. */
@Serializable
sealed class Trigger {
    abstract val typeLabel: String

    @Serializable
    @SerialName("notification")
    data class NotificationTrigger(
        val sourceApps: List<String> = emptyList(), // kosong = semua app
        val keywords: List<String> = emptyList(),   // OR, case-insensitive
    ) : Trigger() {
        override val typeLabel: String get() = "Notifikasi"
    }

    @Serializable
    @SerialName("interval")
    data class IntervalTrigger(
        val intervalMinutes: Int,
    ) : Trigger() {
        override val typeLabel: String get() = "Interval"

        companion object {
            const val MIN_INTERVAL_MINUTES = 15
        }
    }
}
