package com.qrishook.app.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.qrishook.app.data.model.KeyValue
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptedSecurePrefs @Inject constructor(
    @ApplicationContext private val context: Context,
) : SecurePrefs {

    private val json = Json { ignoreUnknownKeys = true }

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    override fun saveWebhookSecret(automationId: Long, url: String, headers: List<KeyValue>) {
        val secret = WebhookSecret(url = url, headers = headers)
        prefs.edit().putString(key(automationId), json.encodeToString(WebhookSecret.serializer(), secret)).apply()
    }

    override fun getWebhookSecret(automationId: Long): WebhookSecret? {
        val raw = prefs.getString(key(automationId), null) ?: return null
        return runCatching { json.decodeFromString(WebhookSecret.serializer(), raw) }.getOrNull()
    }

    override fun deleteWebhookSecret(automationId: Long) {
        prefs.edit().remove(key(automationId)).apply()
    }

    private fun key(automationId: Long) = "webhook_$automationId"

    companion object {
        private const val FILE_NAME = "qrishook_secure"
    }
}
