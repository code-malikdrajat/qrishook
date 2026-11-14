package com.qrishook.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TriggerTest {
    @Test
    fun intervalClampsToMinimumFifteenMinutes() {
        assertEquals(15, Trigger.IntervalTrigger(10).effectiveMinutes)
        assertEquals(15, Trigger.IntervalTrigger(15).effectiveMinutes)
    }

    @Test
    fun intervalKeepsLongerValues() {
        assertEquals(60, Trigger.IntervalTrigger(60).effectiveMinutes)
    }

    @Test
    fun typeLabelsAreStable() {
        assertEquals("Notifikasi", Trigger.NotificationTrigger().typeLabel)
        assertEquals("Interval", Trigger.IntervalTrigger(15).typeLabel)
    }
}
