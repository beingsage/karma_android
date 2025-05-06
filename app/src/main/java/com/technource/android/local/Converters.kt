package com.technource.android.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromSubTaskList(subTasks: List<SubTask>?): String? {
        return subTasks?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toSubTaskList(subTasksString: String?): List<SubTask>? {
        return subTasksString?.let {
            val type = object : TypeToken<List<SubTask>>() {}.type
            gson.fromJson(it, type)
        }
    }

    @TypeConverter
    fun fromTaskStatus(status: TaskStatus?): String? {
        return status?.name
    }

    @TypeConverter
    fun toTaskStatus(statusString: String?): TaskStatus? {
        return statusString?.let { TaskStatus.valueOf(it) }
    }
}