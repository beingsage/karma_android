package com.mindkeep.data.repository

import com.mindkeep.data.dao.ReminderDao
import com.mindkeep.data.model.Reminder
import kotlinx.coroutines.flow.Flow

class ReminderRepository(private val reminderDao: ReminderDao) {
    
    fun getAllReminders(): Flow<List<Reminder>> = reminderDao.getAllReminders()
    
    fun getActiveReminders(): Flow<List<Reminder>> = reminderDao.getActiveReminders()
    
    fun getCompletedReminders(): Flow<List<Reminder>> = reminderDao.getCompletedReminders()
    
    fun getTodayReminders(): Flow<List<Reminder>> = reminderDao.getTodayReminders()
    
    fun getRemindersByCategory(category: String): Flow<List<Reminder>> = 
        reminderDao.getRemindersByCategory(category)
    
    suspend fun insertReminder(reminder: Reminder): Long = reminderDao.insertReminder(reminder)
    
    suspend fun updateReminder(reminder: Reminder) = reminderDao.updateReminder(reminder)
    
    suspend fun deleteReminder(reminder: Reminder) = reminderDao.deleteReminder(reminder)
}