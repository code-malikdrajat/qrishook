package com.qrishook.app.domain.trigger

import com.qrishook.app.data.model.Trigger
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TriggerEvaluatorTest {
    private val trigger = Trigger.NotificationTrigger(
        sourceApps = listOf("com.gojek.app", "id.co.bca"),
        keywords = listOf("QRIS", "masuk"),
    )

    @Test
    fun matchesAppAndKeyword() {
        assertTrue(TriggerEvaluator.matches("com.gojek.app", "QRIS Diterima", "", trigger))
    }

    @Test
    fun anyKeywordMatches() {
        assertTrue(TriggerEvaluator.matches("com.gojek.app", "", "Dana masuk Rp 10.000", trigger))
    }

    @Test
    fun keywordIsCaseInsensitive() {
        assertTrue(TriggerEvaluator.matches("com.gojek.app", "qris diterima", "", trigger))
    }

    @Test
    fun rejectsOtherApp() {
        assertFalse(TriggerEvaluator.matches("com.whatsapp", "QRIS Diterima", "", trigger))
    }

    @Test
    fun rejectsWithoutKeyword() {
        assertFalse(
            TriggerEvaluator.matches(
                "com.gojek.app",
                "Promo hari ini",
                "Diskon 50%",
                trigger,
            ),
        )
    }

    @Test
    fun emptyFiltersAcceptEverything() {
        val all = Trigger.NotificationTrigger()
        assertTrue(TriggerEvaluator.matches("com.anything", "apa saja", "isi", all))
    }

    @Test
    fun keywordMatchesTitleOrText() {
        val titleOnly = Trigger.NotificationTrigger(keywords = listOf("qris"))
        assertTrue(TriggerEvaluator.matches("com.x", "QRIS", "", titleOnly))
    }
}
