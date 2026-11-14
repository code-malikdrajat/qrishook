package com.qrishook.app.domain.execution

import com.qrishook.app.data.model.Automation
import com.qrishook.app.data.model.ExecutionLog
import com.qrishook.app.data.model.LogStatus
import com.qrishook.app.data.prefs.SecurePrefs
import com.qrishook.app.data.repository.AutomationRepository
import com.qrishook.app.data.repository.LogRepository
import com.qrishook.app.domain.webhook.TemplateEngine
import com.qrishook.app.domain.webhook.WebhookClient
import com.qrishook.app.domain.webhook.WebhookResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Render payload → kirim webhook → simpan log. Tidak pernah melempar ke caller. */
@Singleton
class AutomationExecutor @Inject constructor(
    private val webhookClient: WebhookClient,
    private val securePrefs: SecurePrefs,
    private val automationRepository: AutomationRepository,
    private val logRepository: LogRepository,
) {
    suspend fun execute(
        automation: Automation,
        contextMap: Map<String, String>,
    ): WebhookResult {
        val template = automation.action.bodyTemplate.ifBlank {
            PayloadTemplates.defaultFor(automation.trigger)
        }
        val payload = TemplateEngine.render(template, contextMap, escapeJson = true)
        return sendPayload(automation, payload)
    }

    suspend fun sendPayload(automation: Automation, payload: String): WebhookResult =
        withContext(Dispatchers.IO) {
            val secret = securePrefs.getWebhookSecret(automation.id)
            val url = secret?.url?.ifBlank { null } ?: automation.action.url
            val headers = secret?.headers?.takeIf { it.isNotEmpty() } ?: automation.action.headers

            val result = runCatching {
                webhookClient.send(url, automation.action.method, headers, payload)
            }.getOrElse { WebhookResult.NetworkError(it.message ?: "Gagal mengirim") }

            val now = System.currentTimeMillis()
            val (status, httpCode, responseSnippet, errorMessage, durationMs) = when (result) {
                is WebhookResult.Success ->
                    Logged(LogStatus.SUCCESS, result.httpCode, result.body, null, result.durationMs)
                is WebhookResult.Failed ->
                    Logged(LogStatus.FAILED, result.httpCode, result.body, result.error, null)
                is WebhookResult.NetworkError ->
                    Logged(LogStatus.QUEUED, null, null, result.message, null)
            }

            logRepository.insert(
                ExecutionLog(
                    automationId = automation.id,
                    automationName = automation.name,
                    triggerType = automation.trigger.typeLabel,
                    timestamp = now,
                    status = status,
                    httpCode = httpCode,
                    responseSnippet = responseSnippet,
                    errorMessage = errorMessage,
                    payloadSent = payload,
                    durationMs = durationMs,
                ),
            )
            automationRepository.updateLastRun(automation.id, now, status)
            result
        }

    private data class Logged(
        val status: LogStatus,
        val httpCode: Int?,
        val responseSnippet: String?,
        val errorMessage: String?,
        val durationMs: Long?,
    )
}
