package com.qrishook.app.service

import android.content.Context
import android.os.Build
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.qrishook.app.data.model.Automation
import com.qrishook.app.data.model.Trigger
import com.qrishook.app.data.repository.AutomationRepository
import com.qrishook.app.domain.execution.AutomationExecutor
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Instant
import java.util.concurrent.TimeUnit

/** Heartbeat periodik; WorkManager sendiri yang menjadwalkan ulang. */
@HiltWorker
class IntervalWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val automationRepository: AutomationRepository,
    private val executor: AutomationExecutor,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val timestamp = Instant.now().toString()
        automationRepository.getEnabled()
            .filter { it.trigger is Trigger.IntervalTrigger }
            .forEach { automation ->
                executor.execute(
                    automation,
                    mapOf(
                        "device" to Build.MODEL,
                        "timestamp" to timestamp,
                    ),
                )
            }
        return Result.success()
    }

    companion object {
        fun uniqueName(automationId: Long) = "interval_$automationId"

        fun intervalMinutesOf(trigger: Trigger.IntervalTrigger): Long =
            trigger.effectiveMinutes.toLong()

        fun schedule(context: Context, automation: Automation) {
            val trigger = automation.trigger as? Trigger.IntervalTrigger ?: return
            val request = PeriodicWorkRequestBuilder<IntervalWorker>(
                intervalMinutesOf(trigger),
                TimeUnit.MINUTES,
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                uniqueName(automation.id),
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        fun cancel(context: Context, automationId: Long) {
            WorkManager.getInstance(context).cancelUniqueWork(uniqueName(automationId))
        }

        fun scheduleAll(context: Context, automations: List<Automation>) {
            automations.forEach { schedule(context, it) }
        }
    }
}
