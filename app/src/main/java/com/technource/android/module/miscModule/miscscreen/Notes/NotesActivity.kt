package com.technource.android.module.miscModule.miscscreen.Notes

import android.app.Application
import androidx.room.Room
import com.mindkeep.data.database.MindKeepDatabase
import com.mindkeep.data.repository.NotesRepository
import com.mindkeep.data.repository.JournalRepository
import com.mindkeep.data.repository.ReminderRepository

class NotesActivity : Application() {
    
    val database by lazy {
        Room.databaseBuilder(
            applicationContext,
            MindKeepDatabase::class.java,
            "mindkeep_database"
        ).build()
    }
    
    val notesRepository by lazy { NotesRepository(database.noteDao()) }
    val journalRepository by lazy { JournalRepository(database.journalDao()) }
    val reminderRepository by lazy { ReminderRepository(database.reminderDao()) }
}
