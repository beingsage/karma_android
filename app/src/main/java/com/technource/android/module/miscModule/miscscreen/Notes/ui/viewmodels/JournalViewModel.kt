package com.technource.android.module.miscModule.miscscreen.Notes.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mindkeep.data.model.JournalEntry
import com.mindkeep.data.repository.JournalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class JournalViewModel(private val repository: JournalRepository) : ViewModel() {
    
    private val _allJournalEntries = MutableStateFlow<List<JournalEntry>>(emptyList())
    val allJournalEntries: StateFlow<List<JournalEntry>> = _allJournalEntries.asStateFlow()
    
    init {
        viewModelScope.launch {
            repository.getAllJournalEntries().collect {
                _allJournalEntries.value = it
            }
        }
    }
    
    fun insertJournalEntry(journalEntry: JournalEntry): Long {
        var insertedId = 0L
        viewModelScope.launch {
            insertedId = repository.insertJournalEntry(journalEntry)
        }
        return insertedId
    }
    
    fun updateJournalEntry(journalEntry: JournalEntry) {
        viewModelScope.launch {
            repository.updateJournalEntry(journalEntry)
        }
    }
    
    fun deleteJournalEntry(journalEntry: JournalEntry) {
        viewModelScope.launch {
            repository.deleteJournalEntry(journalEntry)
        }
    }
    
    fun getJournalEntriesByTemplate(templateType: String) {
        viewModelScope.launch {
            repository.getJournalEntriesByTemplate(templateType).collect {
                _allJournalEntries.value = it
            }
        }
    }
}

class JournalViewModelFactory(private val repository: JournalRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(JournalViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return JournalViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}