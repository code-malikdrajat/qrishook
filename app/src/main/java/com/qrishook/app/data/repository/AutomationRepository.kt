package com.qrishook.app.data.repository

import com.qrishook.app.data.db.AutomationDao
import com.qrishook.app.data.model.Automation
import com.qrishook.app.data.model.LogStatus
import kotlinx.coroutines.flow.Flow

class AutomationRepository(private val dao: AutomationDao) {
    fun getAll(): Flow<List<Automation>> = dao.getAll()

    suspend fun getById(id: Long): Automation? = dao.getById(id)

    suspend fun getEnabled(): List<Automation> = dao.getEnabled()

    suspend fun insert(automation: Automation): Long = dao.insert(automation)

    suspend fun update(automation: Automation) = dao.update(automation)

    suspend fun delete(automation: Automation) = dao.delete(automation)

    suspend fun setEnabled(id: Long, enabled: Boolean) = dao.toggleEnabled(id, enabled)

    suspend fun updateLastRun(id: Long, at: Long, status: LogStatus) =
        dao.updateLastRun(id, at, status)
}
