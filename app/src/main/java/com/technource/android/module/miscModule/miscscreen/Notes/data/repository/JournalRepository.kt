package com.mindkeep.data.repository

import com.mindkeep.data.dao.JournalDao
import com.mindkeep.data.model.JournalEntry
import kotlinx.coroutines.flow.Flow

class JournalRepository(private val journalDao: JournalDao) {
    
    fun getAllJournalEntries(): Flow<List<JournalEntry>> = journalDao.getAllJournalEntries()
    
    fun getJournalEntriesByTemplate(templateType: String): Flow<List<JournalEntry>> = 
        journalDao.getJournalEntriesByTemplate(templateType)
    
    suspend fun getJournalEntryById(id: Long): JournalEntry? = journalDao.getJournalEntryById(id)
    
    suspend fun insertJournalEntry(journalEntry: JournalEntry): Long = journalDao.insertJournalEntry(journalEntry)
    
    suspend fun updateJournalEntry(journalEntry: JournalEntry) = journalDao.updateJournalEntry(journalEntry)
    
    suspend fun deleteJournalEntry(journalEntry: JournalEntry) = journalDao.deleteJournalEntry(journalEntry)
}