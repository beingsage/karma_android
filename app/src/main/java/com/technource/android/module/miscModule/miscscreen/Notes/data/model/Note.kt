package com.mindkeep.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.mindkeep.data.database.Converters
import java.util.*

@Entity(tableName = "notes")
@TypeConverters(Converters::class)
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val color: Int,
    val tags: List<String> = emptyList(),
    val attachmentPath: String? = null,
    val reminderTime: Date? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val category: String? = null
)
