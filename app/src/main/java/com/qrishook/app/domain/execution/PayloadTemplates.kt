package com.qrishook.app.domain.execution

import com.qrishook.app.data.model.Trigger

/** Payload default — PRD section 4. */
object PayloadTemplates {
    const val TRANSACTION = """{
  "event": "transaction",
  "app": "{{app}}",
  "title": "{{title}}",
  "text": "{{text}}",
  "timestamp": "{{timestamp}}"
}"""

    const val HEARTBEAT = """{
  "event": "heartbeat",
  "device": "{{device}}",
  "timestamp": "{{timestamp}}"
}"""

    fun defaultFor(trigger: Trigger): String = when (trigger) {
        is Trigger.NotificationTrigger -> TRANSACTION
        is Trigger.IntervalTrigger -> HEARTBEAT
    }
}
