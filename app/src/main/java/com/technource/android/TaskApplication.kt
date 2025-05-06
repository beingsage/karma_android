package com.technource.android

import android.app.AlarmManager
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import com.technource.android.module.homeModule.HomeScreen
import com.technource.android.system_status.SystemStatus
import com.technource.android.system_status.SystemStatusService
import com.technource.android.system_status.scheduleStatusCheck
import com.technource.android.ETMS.micro.TaskNotificationService
import com.technource.android.utils.Constants
import com.technource.android.utils.PreferencesManager
import com.technource.android.ETMS.macro.TaskSyncBackendWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.Calendar

@HiltAndroidApp
class TaskApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize PreferencesManager
        PreferencesManager.init(this)

        // Initialize SystemStatus
        SystemStatus.initialize(this)
        SystemStatus.logEvent("TaskApplication", "Application started")

        // Schedule background sync
        TaskSyncBackendWorker.schedule(this)
        SystemStatus.logEvent("TaskApplication", "TaskSyncBackendWorker scheduled")

        // Create notification channel
        createNotificationChannel()

        // Schedule wake-up at 5:30 AM
        scheduleWakeUp()
        scheduleStatusCheck(context = applicationContext)

        // Launch PermissionActivity
        val intent = Intent(this, PermissionActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

     fun startServices() {
         // Start SystemStatusService
         startForegroundService(Intent(this, SystemStatusService::class.java))
         SystemStatus.logEvent("TaskApplication", "SystemStatusService started")

         // Start Floating Service
         startForegroundService(Intent(this, FloatingService::class.java))
         SystemStatus.logEvent("TaskApplication", "Floating Service started")

         // Start TaskNotificationService
         startForegroundService(Intent(this, TaskNotificationService::class.java))
         SystemStatus.logEvent("TaskApplication", "TaskNotificationService started")
     }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            Constants.NOTIFICATION_CHANNEL_ID,
            "Task Notifications",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            enableVibration(true)
            setShowBadge(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        SystemStatus.logEvent("TaskApplication", "Notification channel created")
    }

    private fun scheduleWakeUp() {
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val wakeIntent = Intent(this, HomeScreen::class.java).apply {
            action = "WAKE_UP_ACTION"
        }
        val wakePendingIntent = PendingIntent.getActivity(
            this,
            "wake_app".hashCode(),
            wakeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 5)
            set(Calendar.MINUTE, 30)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            wakePendingIntent
        )
        SystemStatus.logEvent("TaskApplication", "Wake-up scheduled at 5:30 AM")
    }

    companion object {
        lateinit var instance: TaskApplication
            private set
    }
}