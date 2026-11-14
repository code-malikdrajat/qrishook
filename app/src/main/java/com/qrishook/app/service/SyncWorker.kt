package com.qrishook.app.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.qrishook.app.data.model.LogStatus
import com.qrishook.app.data.repository.AutomationRepository
import com.qrishook.app.data.repository.LogRepository
import com.qrishook.app.domain.execution.AutomationExecutor
import com.qrishook.app.domain.webhook.WebhookResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/** Proses antrean offline: kirim ulang log QUEUED saat jaringan tersedia. */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val logRepository: LogRepository,
    private val automationRepository: AutomationRepository,
    private val executor: AutomationExecutor,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        logRepository.getQueued().forEach { log ->
            val automation = automationRepository.getById(log.automationId)
            if (automation == null) {
                logRepository.updateResult(
                    id = log.id,
                    status = LogStatus.FAILED,
                    httpCode = null,
                    responseSnippet = null,
                    errorMessage = "Automation sudah dihapus",
                    durationMs = null,
                )
                return@forEach
            }

            val result = executor.perform(automation, log.payloadSent)
            logRepository.updateResult(
                id = log.id,
                status = when (result) {
                    is WebhookResult.Success -> LogStatus.SUCCESS
                    is WebhookResult.Failed -> LogStatus.FAILED
                    is WebhookResult.NetworkError -> LogStatus.QUEUED
                },
                httpCode = when (result) {
                    is WebhookResult.Success -> result.httpCode
                    is WebhookResult.Failed -> result.httpCode
                    is WebhookResult.NetworkError -> null
                },
                responseSnippet = when (result) {
                    is WebhookResult.Success -> result.body
                    is WebhookResult.Failed -> result.body
                    is WebhookResult.NetworkError -> null
                },
                errorMessage = when (result) {
                    is WebhookResult.Failed -> result.error
                    is WebhookResult.NetworkError -> result.message
                    is WebhookResult.Success -> null
                },
                durationMs = (result as? WebhookResult.Success)?.durationMs,
            )
            if (result is WebhookResult.Success) {
                automationRepository.updateLastRun(automation.id, System.currentTimeMillis(), LogStatus.SUCCESS)
            }
        }
        return Result.success()
    }

    companion object {
        private const val UNIQUE_NAME = "sync_offline_queue"

        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
