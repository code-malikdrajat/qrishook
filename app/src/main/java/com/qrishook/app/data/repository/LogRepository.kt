package com.qrishook.app.data.repository

import com.qrishook.app.data.db.LogDao
import com.qrishook.app.data.model.ExecutionLog
import com.qrishook.app.data.model.LogStatus
import kotlinx.coroutines.flow.Flow

class LogRepository(private val dao: LogDao) {
    fun getAll(): Flow<List<ExecutionLog>> = dao.getAll()

    fun getByAutomation(automationId: Long): Flow<List<ExecutionLog>> = dao.getByAutomation(automationId)

    suspend fun getById(id: Long): ExecutionLog? = dao.getById(id)

    suspend fun insert(log: ExecutionLog): Long = dao.insert(log)

    suspend fun deleteOlderThan(before: Long) = dao.deleteOlderThan(before)

    suspend fun getQueued(): List<ExecutionLog> = dao.getQueued()

    suspend fun updateResult(
        id: Long,
        status: LogStatus,
        httpCode: Int?,
        responseSnippet: String?,
        errorMessage: String?,
        durationMs: Long?,
    ) = dao.updateResult(id, status, httpCode, responseSnippet, errorMessage, durationMs)
}
