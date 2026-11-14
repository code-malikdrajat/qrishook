package com.qrishook.app.domain.execution

import com.qrishook.app.data.model.Trigger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IntervalScheduleTest {
    private val now = 1_700_000_000_000L
    private val fiveMinutes = 5 * 60_000L

    @Test
    fun neverRunIsDue() {
        assertTrue(IntervalSchedule.isDue(null, 5, now))
    }

    @Test
    fun dueAfterInterval() {
        assertTrue(IntervalSchedule.isDue(now - fiveMinutes, 5, now))
        assertTrue(IntervalSchedule.isDue(now - fiveMinutes - 1, 5, now))
    }

    @Test
    fun notDueBeforeInterval() {
        assertFalse(IntervalSchedule.isDue(now - fiveMinutes + 1, 5, now))
        assertFalse(IntervalSchedule.isDue(now, 5, now))
    }

    @Test
    fun intervalTriggerClampsToFiveMinutes() {
        assertEquals(5, Trigger.IntervalTrigger(1).effectiveMinutes)
    }
}
