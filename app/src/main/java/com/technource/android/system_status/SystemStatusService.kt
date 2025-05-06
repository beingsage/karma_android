package com.technource.android.system_status

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SystemStatusService : Service() {
    private val scope = CoroutineScope(Dispatchers.IO)
    private lateinit var database: ServiceLogDatabase
    private val SEVEN_DAYS_IN_MILLIS = 7 * 24 * 60 * 60 * 1000L // 7 days

    override fun onCreate() {
        super.onCreate()
        database = ServiceLogDatabase.getDatabase(this)
        createNotificationChannel()
        try {
            requestForegroundServicePermission()
        } catch (e: Exception) {
            SystemStatus.logEvent("SystemStatusService", "Failed to start foreground: ${e.message}")
        }
        startLogCleanup()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        SystemStatus.initialize(this)
        scope.launch {
            while (true) {
                SystemStatus.pingServices(this@SystemStatusService)
                delay(300000) // Ping every 5 minutes
            }
        }
        return START_STICKY
    }

    private fun startLogCleanup() {
        scope.launch {
            while (true) {
                val threshold = System.currentTimeMillis() - SEVEN_DAYS_IN_MILLIS
                database.serviceLogDao().deleteOldLogs(threshold)
                delay(24 * 60 * 60 * 1000L) // Run cleanup daily
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "SYSTEM_STATUS_CHANNEL",
            "System Status",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun requestForegroundServicePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val notification = NotificationCompat.Builder(this, "SYSTEM_STATUS_CHANNEL")
                .setContentTitle("System Status Monitoring")
                .setContentText("Monitoring background services")
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .build()

            startForeground(1, notification)
        }
    }

    override fun onBind(intent: Intent?): android.os.IBinder? = null
}