package com.qrishook.app.domain.execution

import android.os.Build
import java.time.Instant

/** Data dummy untuk tombol Test (UI_SPEC 3.5) + nilai asli yang tersedia. */
fun testContext(batteryPercent: Int = 87): Map<String, String> = mapOf(
    "app" to "com.contoh.app",
    "title" to "QRIS Diterima",
    "text" to "Anda menerima Rp 50.000 dari Budi",
    "device" to Build.MODEL,
    "device_name" to "HP Android",
    "timestamp" to Instant.now().toString(),
    "battery" to batteryPercent.toString(),
    "notif_id" to "test-${System.currentTimeMillis()}",
)
