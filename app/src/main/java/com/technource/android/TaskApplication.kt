package com.technource.android

import android.app.AlarmManager
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.content.ContextCompat
import com.technource.android.eTMS.macro.EternalTimeTableUnitService
import com.technource.android.module.homeModule.HomeScreen
import com.technource.android.system_status.SystemStatus
import com.technource.android.system_status.SystemStatusService
import com.technource.android.system_status.scheduleStatusCheck
import com.technource.android.utils.PreferencesManager
import com.technource.android.utils.FloatingService
import com.technource.android.utils.PermissionActivity
import dagger.hilt.android.HiltAndroidApp
import java.util.Calendar

@HiltAndroidApp
class TaskApplication : Application() {
    private var NOTIFICATION_CHANNEL_ID = "task_application"

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize PreferencesManager
        PreferencesManager.init(this)

        // Initialize SystemStatus
        SystemStatus.initialize(this)
        SystemStatus.logEvent("TaskApplication", "Application started") 

        // Launch PermissionActivity
        val intent = Intent(this, PermissionActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

     fun startServices() {
         startForegroundService(Intent(this, SystemStatusService::class.java))
         SystemStatus.logEvent("TaskApplication", "SystemStatusService started")

//       Start Floating Service
//       startForegroundService(Intent(this, FloatingService::class.java))
//       SystemStatus.logEvent("TaskApplication", "Floating Service started")

         SystemStatus.logEvent("EternalTimeTableUnitService", "Starting eTMS unit from Task Application")
         val serviceIntent = Intent(this, EternalTimeTableUnitService::class.java)
         ContextCompat.startForegroundService(this, serviceIntent)

     }

    companion object {
        lateinit var instance: TaskApplication
            private set
    }
}