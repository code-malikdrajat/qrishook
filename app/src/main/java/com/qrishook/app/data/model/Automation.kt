package com.qrishook.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "automations")
data class Automation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val trigger: Trigger,
    val action: Action,
    val lastRunAt: Long? = null,
    val lastStatus: LogStatus? = null,
)
