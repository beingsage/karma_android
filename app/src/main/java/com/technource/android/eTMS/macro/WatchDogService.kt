package com.technource.android.eTMS.macro

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.technource.android.eTMS.DashboardActivity
import com.technource.android.system_status.SystemStatus
import com.technource.android.utils.DateFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WatchdogService : Service() {
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate() {
        super.onCreate()
        startForeground(
            ETMSConfig.NOTIFICATION_ID + 1,
            buildWatchdogNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED
        )
        SystemStatus.logEvent("WatchdogService", "Watchdog started at ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(
            Date()
        )}")
        DashboardActivity.logEventToWebView(
            "WatchdogService",
            "Watchdog started at ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}",
            "INFO",
            DateFormatter.formatDateTime(Date())
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isServiceRunning(EternalTimeTableUnitService::class.java)) {
            val serviceIntent = Intent(this, EternalTimeTableUnitService::class.java)
            ContextCompat.startForegroundService(this, serviceIntent)
            SystemStatus.logEvent("WatchdogService", "Restarted EternalTimeTableUnitService")
            DashboardActivity.logEventToWebView(
                "WatchdogService",
                "Restarted EternalTimeTableUnitService",
                "INFO",
                DateFormatter.formatDateTime(Date())
            )
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildWatchdogNotification(): Notification {
        val channelId = ETMSConfig.NOTIFICATION_CHANNEL_ID // Use the same channel ID everywhere
        val channel = NotificationChannel(
            channelId,
            "Eternal Service",
            NotificationManager.IMPORTANCE_MIN
        )
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("ETMS Watchdog")
            .setContentText("Monitoring task service")
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                manager.getRunningServices(Integer.MAX_VALUE)
                    .any { it.service.className == serviceClass.name && it.foreground }
            } else {
                manager.getRunningServices(Integer.MAX_VALUE)
                    .any { it.service.className == serviceClass.name }
            }
        } catch (e: SecurityException) {
            SystemStatus.logEvent("WatchdogService", "SecurityException in isServiceRunning: ${e.message}")
            mapOf("error" to e.message)?.let {
                DashboardActivity.logEventToWebView(
                    "WatchdogService",
                    "SecurityException in isServiceRunning: ${e.message}",
                    "ERROR",
                    DateFormatter.formatDateTime(Date()),
//                    it
                )
            }
            false
        }
    }
}
