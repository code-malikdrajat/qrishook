package com.qrishook.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.qrishook.app.data.repository.AutomationRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var automationRepository: AutomationRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val enabled = automationRepository.getEnabled()
                IntervalWorker.scheduleAll(context, enabled)
                SyncWorker.schedulePeriodic(context)
                if (enabled.isNotEmpty()) QRISHookForegroundService.start(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
