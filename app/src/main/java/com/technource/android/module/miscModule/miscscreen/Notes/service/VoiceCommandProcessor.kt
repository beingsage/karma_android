package com.mindkeep.service

import android.content.Context
import com.mindkeep.data.model.Note
import com.mindkeep.data.model.Reminder
import com.mindkeep.data.model.Priority
import com.technource.android.module.miscModule.miscscreen.Notes.NotesActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class VoiceCommandProcessor(private val context: Context) {
    
    private val application = context.applicationContext as NotesActivity
    private val notesRepository = application.notesRepository
    private val reminderRepository = application.reminderRepository
    
    fun processCommand(command: String, callback: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when {
                    command.contains("add note", ignoreCase = true) ||
                    command.contains("create note", ignoreCase = true) -> {
                        createNoteFromCommand(command)
                        callback(true)
                    }
                    
                    command.contains("add reminder", ignoreCase = true) ||
                    command.contains("remind me", ignoreCase = true) -> {
                        createReminderFromCommand(command)
                        callback(true)
                    }
                    
                    command.contains("search", ignoreCase = true) -> {
                        // Handle search command
                        callback(true)
                    }
                    
                    else -> {
                        // Use AI/LLM to process unknown commands
                        processWithAI(command, callback)
                    }
                }
            } catch (e: Exception) {
                callback(false)
            }
        }
    }
    
    private suspend fun createNoteFromCommand(command: String) {
        val content = extractContentFromCommand(command, listOf("add note", "create note"))
        val tags = extractTagsFromCommand(command)
        val category = extractCategoryFromCommand(command)
        
        val note = Note(
            title = "Voice Note",
            content = content,
            color = android.graphics.Color.YELLOW,
            tags = tags,
            category = category,
            createdAt = Date(),
            updatedAt = Date()
        )
        
        notesRepository.insertNote(note)
    }
    
    private suspend fun createReminderFromCommand(command: String) {
        val content = extractContentFromCommand(command, listOf("add reminder", "remind me"))
        val dateTime = extractDateTimeFromCommand(command)
        val priority = extractPriorityFromCommand(command)
        val category = extractCategoryFromCommand(command)
        
        val reminder = Reminder(
            title = "Voice Reminder",
            description = content,
            dateTime = dateTime ?: Date(System.currentTimeMillis() + 3600000), // Default: 1 hour from now
            priority = priority,
            category = category ?: "General",
            createdAt = Date(),
            updatedAt = Date()
        )
        
        reminderRepository.insertReminder(reminder)
    }
    
    private fun extractContentFromCommand(command: String, triggers: List<String>): String {
        var content = command
        triggers.forEach { trigger ->
            content = content.replace(trigger, "", ignoreCase = true)
        }
        return content.trim()
    }
    
    private fun extractTagsFromCommand(command: String): List<String> {
        val tags = mutableListOf<String>()
        
        // Look for common tag indicators
        when {
            command.contains("work", ignoreCase = true) -> tags.add("Work")
            command.contains("personal", ignoreCase = true) -> tags.add("Personal")
            command.contains("important", ignoreCase = true) -> tags.add("Important")
            command.contains("urgent", ignoreCase = true) -> tags.add("Urgent")
        }
        
        return tags
    }
    
    private fun extractCategoryFromCommand(command: String): String? {
        return when {
            command.contains("work", ignoreCase = true) -> "Work"
            command.contains("personal", ignoreCase = true) -> "Personal"
            command.contains("health", ignoreCase = true) -> "Health"
            command.contains("finance", ignoreCase = true) -> "Finance"
            else -> null
        }
    }
    
    private fun extractDateTimeFromCommand(command: String): Date? {
        // Simple date/time extraction - in a real app, you'd use more sophisticated NLP
        return when {
            command.contains("tomorrow", ignoreCase = true) -> {
                Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_MONTH, 1)
                }.time
            }
            command.contains("next week", ignoreCase = true) -> {
                Calendar.getInstance().apply {
                    add(Calendar.WEEK_OF_YEAR, 1)
                }.time
            }
            command.contains("tonight", ignoreCase = true) -> {
                Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 20)
                    set(Calendar.MINUTE, 0)
                }.time
            }
            else -> null
        }
    }
    
    private fun extractPriorityFromCommand(command: String): Priority {
        return when {
            command.contains("high priority", ignoreCase = true) ||
            command.contains("urgent", ignoreCase = true) -> Priority.HIGH
            command.contains("low priority", ignoreCase = true) -> Priority.LOW
            else -> Priority.MEDIUM
        }
    }
    
    private suspend fun processWithAI(command: String, callback: (Boolean) -> Unit) {
        // Here you would integrate with an AI/LLM service
        // For now, we'll create a generic note
        val note = Note(
            title = "AI Processed Note",
            content = command,
            color = android.graphics.Color.CYAN,
            tags = listOf("AI"),
            createdAt = Date(),
            updatedAt = Date()
        )
        
        notesRepository.insertNote(note)
        callback(true)
    }
}
