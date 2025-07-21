package com.technource.android.module.miscModule.miscscreen.Notes.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mindkeep.data.model.Reminder
import com.mindkeep.data.repository.ReminderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReminderViewModel(private val repository: ReminderRepository) : ViewModel() {
    
    private val _allReminders = MutableStateFlow<List<Reminder>>(emptyList())
    val allReminders: StateFlow<List<Reminder>> = _allReminders.asStateFlow()
    
    private val _activeReminders = MutableStateFlow<List<Reminder>>(emptyList())
    val activeReminders: StateFlow<List<Reminder>> = _activeReminders.asStateFlow()
    
    private val _completedReminders = MutableStateFlow<List<Reminder>>(emptyList())
    val completedReminders: StateFlow<List<Reminder>> = _completedReminders.asStateFlow()
    
    private val _todayReminders = MutableStateFlow<List<Reminder>>(emptyList())
    val todayReminders: StateFlow<List<Reminder>> = _todayReminders.asStateFlow()
    
    init {
        viewModelScope.launch {
            repository.getAllReminders().collect {
                _allReminders.value = it
            }
        }
        
        viewModelScope.launch {
            repository.getActiveReminders().collect {
                _activeReminders.value = it
            }
        }
        
        viewModelScope.launch {
            repository.getCompletedReminders().collect {
                _completedReminders.value = it
            }
        }
        
        viewModelScope.launch {
            repository.getTodayReminders().collect {
                _todayReminders.value = it
            }
        }
    }
    
    fun insertReminder(reminder: Reminder): Long {
        var insertedId = 0L
        viewModelScope.launch {
            insertedId = repository.insertReminder(reminder)
        }
        return insertedId
    }
    
    fun updateReminder(reminder: Reminder) {
        viewModelScope.launch {
            repository.updateReminder(reminder)
        }
    }
    
    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            repository.deleteReminder(reminder)
        }
    }
}

class ReminderViewModelFactory(private val repository: ReminderRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReminderViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReminderViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}