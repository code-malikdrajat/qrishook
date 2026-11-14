package com.qrishook.app.domain.execution

/** Interval heartbeat dijalankan oleh ticker foreground service (minimum 5 menit). */
object IntervalSchedule {
    fun isDue(lastRunAt: Long?, intervalMinutes: Int, now: Long): Boolean {
        if (lastRunAt == null) return true
        return now - lastRunAt >= intervalMinutes * 60_000L
    }
}
