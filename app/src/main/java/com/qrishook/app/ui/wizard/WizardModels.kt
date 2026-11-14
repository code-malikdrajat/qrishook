package com.qrishook.app.ui.wizard

import android.os.Build
import com.qrishook.app.data.model.HttpMethod
import com.qrishook.app.data.model.KeyValue
import com.qrishook.app.data.model.Trigger
import com.qrishook.app.domain.execution.PayloadTemplates
import com.qrishook.app.ui.components.TriggerKind
import java.time.Instant

const val WIZARD_STEP_COUNT = 5
const val NAME_MAX_LENGTH = 50

data class InstalledApp(
    val packageName: String,
    val label: String,
)

sealed interface TestState {
    data object Idle : TestState
    data object Loading : TestState
    data class Success(val code: Int, val durationMs: Long, val response: String) : TestState
    data class Failed(val code: Int?, val message: String, val response: String) : TestState
}

data class WizardUiState(
    val step: Int = 0,
    val automationId: Long? = null,
    val isEdit: Boolean = false,
    val name: String = "",
    val triggerKind: TriggerKind = TriggerKind.NOTIFICATION,
    val sourceApps: List<String> = emptyList(),
    val keywords: List<String> = emptyList(),
    val keywordInput: String = "",
    val intervalInput: String = "15",
    val url: String = "",
    val method: HttpMethod = HttpMethod.POST,
    val headers: List<KeyValue> = listOf(KeyValue("Content-Type", "application/json")),
    val bodyTemplate: String = PayloadTemplates.TRANSACTION,
    val installedApps: List<InstalledApp> = emptyList(),
    val loadingApps: Boolean = true,
    val testState: TestState = TestState.Idle,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val saveError: Boolean = false,
) {
    val intervalMinutes: Int? get() = intervalInput.toIntOrNull()

    val intervalTooShort: Boolean
        get() = triggerKind == TriggerKind.INTERVAL &&
            (intervalMinutes ?: 0) < Trigger.IntervalTrigger.MIN_INTERVAL_MINUTES

    val urlValid: Boolean
        get() = url.startsWith("http://") || url.startsWith("https://")

    val canGoNext: Boolean
        get() = when (step) {
            0 -> name.isNotBlank() && name.length <= NAME_MAX_LENGTH
            1 -> true
            2 -> triggerKind == TriggerKind.NOTIFICATION || !intervalTooShort
            3, 4 -> urlValid
            else -> false
        }

    fun toTrigger(): Trigger = when (triggerKind) {
        TriggerKind.NOTIFICATION -> Trigger.NotificationTrigger(
            sourceApps = sourceApps,
            keywords = keywords,
        )
        TriggerKind.INTERVAL -> Trigger.IntervalTrigger(
            intervalMinutes = (intervalMinutes ?: Trigger.IntervalTrigger.MIN_INTERVAL_MINUTES)
                .coerceAtLeast(Trigger.IntervalTrigger.MIN_INTERVAL_MINUTES),
        )
    }

    fun activeHeaders(): List<KeyValue> = headers.filter { it.key.isNotBlank() }
}

/** Data dummy untuk tombol Test dan preview payload (UI_SPEC 3.5). */
fun WizardUiState.dummyContext(): Map<String, String> = mapOf(
    "app" to (sourceApps.firstOrNull() ?: "com.contoh.app"),
    "title" to "QRIS Diterima",
    "text" to "Anda menerima Rp 50.000 dari Budi",
    "device" to Build.MODEL,
    "timestamp" to Instant.now().toString(),
)
