package com.qrishook.app.domain.execution

import com.qrishook.app.data.db.AutomationDao
import com.qrishook.app.data.db.LogDao
import com.qrishook.app.data.model.Action
import com.qrishook.app.data.model.Automation
import com.qrishook.app.data.model.ExecutionLog
import com.qrishook.app.data.model.HttpMethod
import com.qrishook.app.data.model.KeyValue
import com.qrishook.app.data.model.LogStatus
import com.qrishook.app.data.model.Trigger
import com.qrishook.app.data.prefs.SecurePrefs
import com.qrishook.app.data.prefs.WebhookSecret
import com.qrishook.app.data.repository.AutomationRepository
import com.qrishook.app.data.repository.LogRepository
import com.qrishook.app.domain.webhook.WebhookClient
import com.qrishook.app.domain.webhook.WebhookResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AutomationExecutorTest {

    private val automation = Automation(
        id = 1,
        name = "QRIS masuk",
        trigger = Trigger.NotificationTrigger(keywords = listOf("QRIS")),
        action = Action(
            url = "https://example.com/hook",
            method = HttpMethod.POST,
            bodyTemplate = """{"event":"transaction","text":"{{text}}"}""",
        ),
    )

    @Test
    fun successWritesLogAndUpdatesAutomation() = runTest {
        val automationDao = FakeAutomationDao(automation)
        val logDao = FakeLogDao()
        val executor = newExecutor(
            result = WebhookResult.Success(200, """{"ok":true}""", 42),
            automationDao = automationDao,
            logDao = logDao,
        )

        val result = executor.execute(automation, mapOf("text" to "Rp 50.000"))

        assertTrue(result is WebhookResult.Success)
        val log = logDao.logs.value.single()
        assertEquals(LogStatus.SUCCESS, log.status)
        assertEquals(200, log.httpCode)
        assertEquals("""{"event":"transaction","text":"Rp 50.000"}""", log.payloadSent)
        assertEquals(LogStatus.SUCCESS, automationDao.current.lastStatus)
    }

    @Test
    fun httpFailureIsLoggedAsFailed() = runTest {
        val logDao = FakeLogDao()
        val executor = newExecutor(
            result = WebhookResult.Failed(500, "boom", "Server Error"),
            logDao = logDao,
        )

        executor.execute(automation, mapOf("text" to "x"))

        assertEquals(LogStatus.FAILED, logDao.logs.value.single().status)
    }

    @Test
    fun networkErrorIsQueued() = runTest {
        val logDao = FakeLogDao()
        val executor = newExecutor(
            result = WebhookResult.NetworkError("offline"),
            logDao = logDao,
        )

        executor.execute(automation, mapOf("text" to "x"))

        assertEquals(LogStatus.QUEUED, logDao.logs.value.single().status)
    }

    @Test
    fun secureUrlOverridesStoredUrl() = runTest {
        var sentUrl = ""
        val executor = newExecutor(
            result = WebhookResult.Success(200, "", 1),
            securePrefs = FakeSecurePrefs(
                WebhookSecret("https://secret.example/hook", listOf(KeyValue("X-Token", "t"))),
            ),
            onSend = { url -> sentUrl = url },
        )

        executor.execute(automation, mapOf("text" to "x"))

        assertEquals("https://secret.example/hook", sentUrl)
    }

    private fun newExecutor(
        result: WebhookResult,
        automationDao: FakeAutomationDao = FakeAutomationDao(automation),
        logDao: FakeLogDao = FakeLogDao(),
        securePrefs: SecurePrefs = FakeSecurePrefs(),
        onSend: (String) -> Unit = {},
    ) = AutomationExecutor(
        webhookClient = object : WebhookClient {
            override suspend fun send(
                url: String,
                method: HttpMethod,
                headers: List<KeyValue>,
                body: String,
            ): WebhookResult {
                onSend(url)
                return result
            }
        },
        securePrefs = securePrefs,
        automationRepository = AutomationRepository(automationDao),
        logRepository = LogRepository(logDao),
    )
}

private class FakeAutomationDao(seed: Automation) : AutomationDao {
    val items = MutableStateFlow(listOf(seed))
    val current: Automation get() = items.value.first()

    override fun getAll(): Flow<List<Automation>> = items

    override suspend fun getById(id: Long) = items.value.firstOrNull { it.id == id }

    override suspend fun getEnabled(): List<Automation> = items.value.filter { it.enabled }

    override suspend fun insert(automation: Automation): Long = automation.id

    override suspend fun update(automation: Automation) {
        items.value = items.value.map { if (it.id == automation.id) automation else it }
    }

    override suspend fun delete(automation: Automation) {
        items.value = items.value.filterNot { it.id == automation.id }
    }

    override suspend fun toggleEnabled(id: Long, enabled: Boolean) {
        items.value = items.value.map { if (it.id == id) it.copy(enabled = enabled) else it }
    }

    override suspend fun updateLastRun(id: Long, at: Long, status: LogStatus) {
        items.value = items.value.map {
            if (it.id == id) it.copy(lastRunAt = at, lastStatus = status) else it
        }
    }
}

private class FakeLogDao : LogDao {
    val logs = MutableStateFlow<List<ExecutionLog>>(emptyList())

    override fun getAll(): Flow<List<ExecutionLog>> = logs

    override fun getByAutomation(automationId: Long): Flow<List<ExecutionLog>> =
        logs.map { list -> list.filter { it.automationId == automationId } }

    override suspend fun getById(id: Long) = logs.value.firstOrNull { it.id == id }

    override suspend fun insert(log: ExecutionLog): Long {
        val id = (logs.value.maxOfOrNull { it.id } ?: 0) + 1
        logs.value = logs.value + log.copy(id = id)
        return id
    }

    override suspend fun deleteOlderThan(before: Long) {
        logs.value = logs.value.filterNot { it.timestamp < before }
    }

    override suspend fun getQueued(): List<ExecutionLog> =
        logs.value.filter { it.status == LogStatus.QUEUED }

    override suspend fun updateResult(
        id: Long,
        status: LogStatus,
        httpCode: Int?,
        responseSnippet: String?,
        errorMessage: String?,
        durationMs: Long?,
    ) {
        logs.value = logs.value.map {
            if (it.id == id) {
                it.copy(
                    status = status,
                    httpCode = httpCode,
                    responseSnippet = responseSnippet,
                    errorMessage = errorMessage,
                    durationMs = durationMs,
                )
            } else {
                it
            }
        }
    }
}

private class FakeSecurePrefs(private val secret: WebhookSecret? = null) : SecurePrefs {
    override fun saveWebhookSecret(automationId: Long, url: String, headers: List<KeyValue>) = Unit

    override fun getWebhookSecret(automationId: Long): WebhookSecret? = secret

    override fun deleteWebhookSecret(automationId: Long) = Unit
}
