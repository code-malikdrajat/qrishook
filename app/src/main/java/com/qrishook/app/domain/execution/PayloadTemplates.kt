package com.qrishook.app.domain.execution

import com.qrishook.app.data.model.Trigger

/** Payload default — disamakan dengan contoh MacroDroid pengguna. */
object PayloadTemplates {
    const val TRANSACTION = """{
  "app": "{{app}}",
  "text": "{{text}}",
  "notif_id": "{{notif_id}}"
}"""

    const val HEARTBEAT = """{
  "is_heartbeat": true,
  "battery": {{battery}},
  "device_name": "HP Android"
}"""

    fun defaultFor(trigger: Trigger): String = when (trigger) {
        is Trigger.NotificationTrigger -> TRANSACTION
        is Trigger.IntervalTrigger -> HEARTBEAT
    }
}
