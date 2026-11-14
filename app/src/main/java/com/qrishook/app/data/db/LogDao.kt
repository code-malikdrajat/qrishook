package com.qrishook.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.qrishook.app.data.model.ExecutionLog
import com.qrishook.app.data.model.LogStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {
    @Query("SELECT * FROM execution_logs ORDER BY timestamp DESC")
    fun getAll(): Flow<List<ExecutionLog>>

    @Query("SELECT * FROM execution_logs WHERE automationId = :automationId ORDER BY timestamp DESC")
    fun getByAutomation(automationId: Long): Flow<List<ExecutionLog>>

    @Query("SELECT * FROM execution_logs WHERE id = :id")
    suspend fun getById(id: Long): ExecutionLog?

    @Insert
    suspend fun insert(log: ExecutionLog): Long

    @Query("DELETE FROM execution_logs WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("SELECT * FROM execution_logs WHERE status = 'QUEUED' ORDER BY timestamp ASC")
    suspend fun getQueued(): List<ExecutionLog>

    @Query(
        "UPDATE execution_logs SET status = :status, httpCode = :httpCode, " +
            "responseSnippet = :responseSnippet, errorMessage = :errorMessage, durationMs = :durationMs " +
            "WHERE id = :id"
    )
    suspend fun updateResult(
        id: Long,
        status: LogStatus,
        httpCode: Int?,
        responseSnippet: String?,
        errorMessage: String?,
        durationMs: Long?,
    )
}
