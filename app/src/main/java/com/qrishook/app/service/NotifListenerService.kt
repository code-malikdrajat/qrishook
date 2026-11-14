package com.qrishook.app.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import com.qrishook.app.data.model.Trigger
import com.qrishook.app.data.repository.AutomationRepository
import com.qrishook.app.domain.execution.AutomationExecutor
import com.qrishook.app.domain.trigger.TriggerEvaluator
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NotifListenerService : NotificationListenerService() {

    @Inject lateinit var automationRepository: AutomationRepository
    @Inject lateinit var executor: AutomationExecutor

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return // jangan trigger oleh notifikasi sendiri

        val extras = sbn.notification?.extras
        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = (
            extras?.getCharSequence(Notification.EXTRA_TEXT)
                ?: extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)
            )?.toString().orEmpty()
        val postedAt = Instant.ofEpochMilli(sbn.postTime).toString()

        scope.launch {
            automationRepository.getEnabled()
                .filter { it.trigger is Trigger.NotificationTrigger }
                .forEach { automation ->
                    val trigger = automation.trigger as Trigger.NotificationTrigger
                    if (TriggerEvaluator.matches(sbn.packageName, title, text, trigger)) {
                        executor.execute(
                            automation,
                            mapOf(
                                "app" to sbn.packageName,
                                "title" to title,
                                "text" to text,
                                "timestamp" to postedAt,
                            ),
                        )
                    }
                }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        fun isPermissionGranted(context: android.content.Context): Boolean =
            NotificationManagerCompat.getEnabledListenerPackages(context)
                .contains(context.packageName)
    }
}
