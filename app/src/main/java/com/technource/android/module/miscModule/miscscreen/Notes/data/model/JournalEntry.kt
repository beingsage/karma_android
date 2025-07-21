package com.mindkeep.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.mindkeep.data.database.Converters
import java.util.*

@Entity(tableName = "journal_entries")
@TypeConverters(Converters::class)
data class JournalEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val templateType: String,
    val tags: List<String> = emptyList(),
    val attachmentPath: String? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val mood: String? = null,
    val weather: String? = null
)
