package com.mindkeep.data.dao

import androidx.room.*
import com.mindkeep.data.model.JournalEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {
    
    @Query("SELECT * FROM journal_entries ORDER BY createdAt DESC")
    fun getAllJournalEntries(): Flow<List<JournalEntry>>
    
    @Query("SELECT * FROM journal_entries WHERE templateType = :templateType ORDER BY createdAt DESC")
    fun getJournalEntriesByTemplate(templateType: String): Flow<List<JournalEntry>>
    
    @Query("SELECT * FROM journal_entries WHERE id = :id")
    suspend fun getJournalEntryById(id: Long): JournalEntry?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournalEntry(journalEntry: JournalEntry): Long
    
    @Update
    suspend fun updateJournalEntry(journalEntry: JournalEntry)
    
    @Delete
    suspend fun deleteJournalEntry(journalEntry: JournalEntry)
}
