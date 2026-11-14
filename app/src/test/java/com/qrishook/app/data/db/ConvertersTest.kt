package com.qrishook.app.data.db

import com.qrishook.app.data.model.Action
import com.qrishook.app.data.model.HttpMethod
import com.qrishook.app.data.model.KeyValue
import com.qrishook.app.data.model.LogStatus
import com.qrishook.app.data.model.Trigger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConvertersTest {
    private val converters = Converters()

    @Test
    fun notificationTriggerRoundTrip() {
        val trigger = Trigger.NotificationTrigger(
            sourceApps = listOf("com.gojek.app", "id.co.bank"),
            keywords = listOf("QRIS", "masuk"),
        )
        assertEquals(trigger, converters.jsonToTrigger(converters.triggerToJson(trigger)))
    }

    @Test
    fun intervalTriggerRoundTrip() {
        val trigger = Trigger.IntervalTrigger(30)
        assertEquals(trigger, converters.jsonToTrigger(converters.triggerToJson(trigger)))
    }

    @Test
    fun actionRoundTrip() {
        val action = Action(
            url = "https://example.com/hook",
            method = HttpMethod.GET,
            headers = listOf(KeyValue("X-Key", "value")),
            bodyTemplate = """{"event":"heartbeat"}""",
        )
        assertEquals(action, converters.jsonToAction(converters.actionToJson(action)))
    }

    @Test
    fun statusRoundTrip() {
        assertEquals(LogStatus.QUEUED, converters.stringToStatus(converters.statusToString(LogStatus.QUEUED)))
        assertNull(converters.stringToStatus(null))
    }
}
