package com.qrishook.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class LogStatus { SUCCESS, FAILED, QUEUED }

@Entity(tableName = "execution_logs")
data class ExecutionLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val automationId: Long,
    val automationName: String,
    val triggerType: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: LogStatus,
    val httpCode: Int? = null,
    val responseSnippet: String? = null,
    val errorMessage: String? = null,
    val payloadSent: String = "",
    val durationMs: Long? = null,
)
