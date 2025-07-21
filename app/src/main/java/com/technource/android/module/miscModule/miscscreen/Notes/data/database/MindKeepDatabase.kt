package com.mindkeep.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.mindkeep.data.dao.NoteDao
import com.mindkeep.data.dao.JournalDao
import com.mindkeep.data.dao.ReminderDao
import com.mindkeep.data.model.Note
import com.mindkeep.data.model.JournalEntry
import com.mindkeep.data.model.Reminder

@Database(
    entities = [Note::class, JournalEntry::class, Reminder::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MindKeepDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun journalDao(): JournalDao
    abstract fun reminderDao(): ReminderDao
}
