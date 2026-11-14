package com.qrishook.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.qrishook.app.data.model.Automation
import com.qrishook.app.data.model.LogStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface AutomationDao {
    @Query("SELECT * FROM automations ORDER BY createdAt DESC")
    fun getAll(): Flow<List<Automation>>

    @Query("SELECT * FROM automations WHERE id = :id")
    suspend fun getById(id: Long): Automation?

    @Query("SELECT * FROM automations WHERE enabled = 1 ORDER BY createdAt DESC")
    suspend fun getEnabled(): List<Automation>

    @Insert
    suspend fun insert(automation: Automation): Long

    @Update
    suspend fun update(automation: Automation)

    @Delete
    suspend fun delete(automation: Automation)

    @Query("UPDATE automations SET enabled = :enabled WHERE id = :id")
    suspend fun toggleEnabled(id: Long, enabled: Boolean)

    @Query("UPDATE automations SET lastRunAt = :at, lastStatus = :status WHERE id = :id")
    suspend fun updateLastRun(id: Long, at: Long, status: LogStatus)
}
