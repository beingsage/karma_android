package com.mindkeep.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.mindkeep.data.database.Converters
import java.util.*

@Entity(tableName = "reminders")
@TypeConverters(Converters::class)
data class Reminder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val dateTime: Date,
    val priority: Priority,
    val category: String,
    val isCompleted: Boolean = false,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)

enum class Priority {
    LOW, MEDIUM, HIGH
}
