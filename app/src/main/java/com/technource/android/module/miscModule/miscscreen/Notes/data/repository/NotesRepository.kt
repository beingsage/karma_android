package com.mindkeep.data.repository

import com.mindkeep.data.dao.NoteDao
import com.mindkeep.data.model.Note
import kotlinx.coroutines.flow.Flow

class NotesRepository(private val noteDao: NoteDao) {
    
    fun getAllNotes(): Flow<List<Note>> = noteDao.getAllNotes()
    
    fun searchNotes(query: String): Flow<List<Note>> = noteDao.searchNotes("%$query%")
    
    fun getNotesByCategory(category: String): Flow<List<Note>> = noteDao.getNotesByCategory(category)
    
    suspend fun getNoteById(id: Long): Note? = noteDao.getNoteById(id)
    
    suspend fun insertNote(note: Note): Long = noteDao.insertNote(note)
    
    suspend fun updateNote(note: Note) = noteDao.updateNote(note)
    
    suspend fun deleteNote(note: Note) = noteDao.deleteNote(note)
    
    suspend fun deleteNoteById(id: Long) = noteDao.deleteNoteById(id)
}
