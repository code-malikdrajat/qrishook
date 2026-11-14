package com.qrishook.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TriggerTest {
    @Test
    fun intervalClampsToMinimumFiveMinutes() {
        assertEquals(5, Trigger.IntervalTrigger(1).effectiveMinutes)
        assertEquals(5, Trigger.IntervalTrigger(5).effectiveMinutes)
    }

    @Test
    fun intervalKeepsLongerValues() {
        assertEquals(60, Trigger.IntervalTrigger(60).effectiveMinutes)
    }

    @Test
    fun typeLabelsAreStable() {
        assertEquals("Notifikasi", Trigger.NotificationTrigger().typeLabel)
        assertEquals("Interval", Trigger.IntervalTrigger(5).typeLabel)
    }
}
