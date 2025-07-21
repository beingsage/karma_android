package com.mindkeep.data.dao

import androidx.room.*
import com.mindkeep.data.model.Reminder
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
interface ReminderDao {
    
    @Query("SELECT * FROM reminders ORDER BY dateTime ASC")
    fun getAllReminders(): Flow<List<Reminder>>
    
    @Query("SELECT * FROM reminders WHERE isCompleted = 0 ORDER BY dateTime ASC")
    fun getActiveReminders(): Flow<List<Reminder>>
    
    @Query("SELECT * FROM reminders WHERE isCompleted = 1 ORDER BY dateTime DESC")
    fun getCompletedReminders(): Flow<List<Reminder>>
    
    @Query("SELECT * FROM reminders WHERE DATE(dateTime/1000, 'unixepoch') = DATE('now') AND isCompleted = 0 ORDER BY dateTime ASC")
    fun getTodayReminders(): Flow<List<Reminder>>
    
    @Query("SELECT * FROM reminders WHERE category = :category ORDER BY dateTime ASC")
    fun getRemindersByCategory(category: String): Flow<List<Reminder>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder): Long
    
    @Update
    suspend fun updateReminder(reminder: Reminder)
    
    @Delete
    suspend fun deleteReminder(reminder: Reminder)
}
