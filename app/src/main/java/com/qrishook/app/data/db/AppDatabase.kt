package com.qrishook.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.qrishook.app.data.model.Automation
import com.qrishook.app.data.model.ExecutionLog

@Database(
    entities = [Automation::class, ExecutionLog::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun automationDao(): AutomationDao
    abstract fun logDao(): LogDao

    companion object {
        const val NAME = "qrishook.db"
    }
}
