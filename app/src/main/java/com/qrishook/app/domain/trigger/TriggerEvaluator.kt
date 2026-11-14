package com.qrishook.app.domain.trigger

import com.qrishook.app.data.model.Trigger

object TriggerEvaluator {
    /** App filter: kosong = semua app. Keyword: OR, case-insensitive, kosong = semua notifikasi. */
    fun matches(
        packageName: String,
        title: String,
        text: String,
        trigger: Trigger.NotificationTrigger,
    ): Boolean {
        val appMatches = trigger.sourceApps.isEmpty() || packageName in trigger.sourceApps
        if (!appMatches) return false

        if (trigger.keywords.isEmpty()) return true

        val haystack = "$title $text".lowercase()
        return trigger.keywords.any { haystack.contains(it.lowercase()) }
    }
}
