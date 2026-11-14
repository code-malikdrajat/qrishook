package com.qrishook.app.service

import com.qrishook.app.data.model.Trigger
import org.junit.Assert.assertEquals
import org.junit.Test

class IntervalWorkerTest {
    @Test
    fun clampsToMinimumFifteenMinutes() {
        assertEquals(15L, IntervalWorker.intervalMinutesOf(Trigger.IntervalTrigger(10)))
        assertEquals(15L, IntervalWorker.intervalMinutesOf(Trigger.IntervalTrigger(15)))
    }

    @Test
    fun keepsLongerIntervals() {
        assertEquals(60L, IntervalWorker.intervalMinutesOf(Trigger.IntervalTrigger(60)))
    }

    @Test
    fun uniqueWorkNameIsPerAutomation() {
        assertEquals("interval_7", IntervalWorker.uniqueName(7))
        assertEquals("interval_8", IntervalWorker.uniqueName(8))
    }
}
