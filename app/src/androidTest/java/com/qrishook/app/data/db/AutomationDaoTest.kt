package com.qrishook.app.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.qrishook.app.data.model.Action
import com.qrishook.app.data.model.Automation
import com.qrishook.app.data.model.Trigger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AutomationDaoTest {
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun insertAndRead() = runTest {
        val automation = Automation(
            name = "QRIS masuk",
            trigger = Trigger.NotificationTrigger(keywords = listOf("QRIS")),
            action = Action(url = "https://example.com/hook", bodyTemplate = """{"event":"transaction"}"""),
        )

        val id = db.automationDao().insert(automation)
        val stored = db.automationDao().getById(id)

        assertNotNull(stored)
        assertEquals("QRIS masuk", stored!!.name)
        assertEquals(listOf("QRIS"), (stored.trigger as Trigger.NotificationTrigger).keywords)
        assertEquals(1, db.automationDao().getAll().first().size)
    }
}
