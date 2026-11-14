package com.qrishook.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatTest {
    private val now = 1_700_000_000_000L

    @Test
    fun justNow() {
        assertEquals("baru saja", formatRelativeTime(now - 20_000, now))
    }

    @Test
    fun minutes() {
        assertEquals("5 menit lalu", formatRelativeTime(now - 5 * 60_000, now))
    }

    @Test
    fun hours() {
        assertEquals("3 jam lalu", formatRelativeTime(now - 3 * 60 * 60_000, now))
    }

    @Test
    fun days() {
        assertEquals("2 hari lalu", formatRelativeTime(now - 2 * 24 * 60 * 60_000, now))
    }

    @Test
    fun olderUsesAbsoluteDate() {
        val old = now - 30L * 24 * 60 * 60_000
        assertEquals(formatDateTime(old), formatRelativeTime(old, now))
    }

    @Test
    fun futureTimestampDoesNotGoNegative() {
        assertEquals("baru saja", formatRelativeTime(now + 60_000, now))
    }
}
