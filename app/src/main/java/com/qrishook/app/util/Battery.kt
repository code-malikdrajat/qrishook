package com.qrishook.app.util

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

object Battery {
    /** Persen 0..100, atau -1 kalau tidak terbaca. */
    fun percent(context: Context): Int {
        val manager = context.getSystemService(BatteryManager::class.java)
        val property = manager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        if (property in 0..100) return property

        val sticky = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
        ) ?: return -1
        val level = sticky.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = sticky.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        return if (level >= 0 && scale > 0) level * 100 / scale else -1
    }
}
