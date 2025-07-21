package com.mindkeep.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.technource.android.module.miscModule.miscscreen.Notes.utils.NotificationScheduler

class ReminderReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra("reminder_id", 0)
        val title = intent.getStringExtra("reminder_title") ?: "Reminder"
        val description = intent.getStringExtra("reminder_description") ?: ""
        
        NotificationScheduler.showNotification(context, title, description, reminderId)
    }
}