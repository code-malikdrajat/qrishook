package com.qrishook.app.data.db

import androidx.room.TypeConverter
import com.qrishook.app.data.model.Action
import com.qrishook.app.data.model.LogStatus
import com.qrishook.app.data.model.Trigger
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @TypeConverter
    fun triggerToJson(trigger: Trigger): String = json.encodeToString(Trigger.serializer(), trigger)

    @TypeConverter
    fun jsonToTrigger(value: String): Trigger = json.decodeFromString(Trigger.serializer(), value)

    @TypeConverter
    fun actionToJson(action: Action): String = json.encodeToString(Action.serializer(), action)

    @TypeConverter
    fun jsonToAction(value: String): Action = json.decodeFromString(Action.serializer(), value)

    @TypeConverter
    fun statusToString(status: LogStatus?): String? = status?.name

    @TypeConverter
    fun stringToStatus(value: String?): LogStatus? =
        value?.let { runCatching { LogStatus.valueOf(it) }.getOrNull() }
}
