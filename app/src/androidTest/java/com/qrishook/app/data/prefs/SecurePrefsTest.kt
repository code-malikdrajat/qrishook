package com.qrishook.app.data.prefs

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.qrishook.app.data.model.KeyValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecurePrefsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val prefs: SecurePrefs = EncryptedSecurePrefs(context)

    @Test
    fun saveReadDeleteRoundTrip() {
        prefs.saveWebhookSecret(42, "https://example.com/hook", listOf(KeyValue("X-Token", "rahasia")))

        val stored = prefs.getWebhookSecret(42)
        assertEquals("https://example.com/hook", stored?.url)
        assertEquals(listOf(KeyValue("X-Token", "rahasia")), stored?.headers)

        prefs.deleteWebhookSecret(42)
        assertNull(prefs.getWebhookSecret(42))
    }
}
