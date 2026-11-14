package com.qrishook.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.qrishook.app.MainActivity
import com.qrishook.app.R
import com.qrishook.app.data.model.Trigger
import com.qrishook.app.data.repository.AutomationRepository
import com.qrishook.app.domain.execution.AutomationExecutor
import com.qrishook.app.domain.execution.IntervalSchedule
import com.qrishook.app.util.Battery
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

/**
 * Notifikasi persisten + ticker heartbeat (interval minimum 5 menit).
 * Dipilih ticker di service (bukan WorkManager) karena periode < 15 menit.
 */
@AndroidEntryPoint
class QRISHookForegroundService : Service() {

    @Inject lateinit var automationRepository: AutomationRepository
    @Inject lateinit var executor: AutomationExecutor

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observer: Job? = null
    private var ticker: Job? = null
    private val tickMutex = Mutex()

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundWithNotification(0)
        startTicker()

        observer?.cancel()
        observer = scope.launch {
            automationRepository.getAll().collect { automations ->
                val active = automations.count { it.enabled }
                if (active == 0) {
                    stopSelf()
                } else {
                    notifyCount(active)
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?) = null

    override fun onDestroy() {
        isRunning = false
        observer?.cancel()
        ticker?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    private fun startTicker() {
        if (ticker?.isActive == true) return
        ticker = scope.launch {
            while (isActive) {
                runCatching { tickDueIntervals() }
                delay(TICK_INTERVAL_MS)
            }
        }
    }

    private suspend fun tickDueIntervals() {
        if (!tickMutex.tryLock()) return
        try {
            val now = System.currentTimeMillis()
            val contextMap = mapOf(
                "device" to Build.MODEL,
                "device_name" to "HP Android",
                "timestamp" to Instant.now().toString(),
                "battery" to Battery.percent(this@QRISHookForegroundService).toString(),
            )
            automationRepository.getEnabled()
                .filter { it.trigger is Trigger.IntervalTrigger }
                .forEach { automation ->
                    val trigger = automation.trigger as Trigger.IntervalTrigger
                    if (IntervalSchedule.isDue(automation.lastRunAt, trigger.effectiveMinutes, now)) {
                        executor.execute(automation, contextMap)
                    }
                }
        } finally {
            tickMutex.unlock()
        }
    }

    private fun startForegroundWithNotification(count: Int) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(count), type)
    }

    private fun notifyCount(count: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, buildNotification(count))
    }

    private fun buildNotification(count: Int): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_qrishook)
            .setContentTitle(getString(R.string.notif_service_title))
            .setContentText(getString(R.string.notif_service_text, count))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(contentIntent)
            .build()
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notif_channel_service),
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "qrishook_service"
        private const val NOTIFICATION_ID = 1001
        private const val TICK_INTERVAL_MS = 30_000L

        @Volatile
        var isRunning: Boolean = false
            private set

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, QRISHookForegroundService::class.java),
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, QRISHookForegroundService::class.java))
        }
    }
}
