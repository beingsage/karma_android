package com.technource.android.eTMS.macro

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.technource.android.system_status.SystemStatus


// AutoStart Receiver
class AutoStartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action in listOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_USER_PRESENT, Intent.ACTION_POWER_CONNECTED)) {
            context?.let {
                val serviceIntent = Intent(it, EternalTimeTableUnitService::class.java)
                try {
                    ContextCompat.startForegroundService(it, serviceIntent)
                    if (intent != null) {
                        SystemStatus.logEvent("AutoStartReceiver", "Restarted service after ${intent.action}")
                    }

                    // Re-register JobScheduler
                    val jobScheduler = it.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                    val jobInfo = JobInfo.Builder(1001, ComponentName(it, RestartServiceJob::class.java))
                        .setPeriodic(15 * 60 * 1000L)
                        .setPersisted(true)
                        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE)
                        .setRequiresCharging(false)
                        .setRequiresDeviceIdle(false)
                        .build()
                    jobScheduler.schedule(jobInfo)
                    SystemStatus.logEvent("AutoStartReceiver", "Re-registered JobScheduler")
                }
                catch (e: Exception) {
                    SystemStatus.logEvent("AutoStartReceiver", "Failed to start service: ${e.message}")
                    val notificationManager = it.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    val channel = NotificationChannel(
                        ETMSConfig.NOTIFICATION_CHANNEL_ID,
                        "Eternal Service",
                        NotificationManager.IMPORTANCE_HIGH
                    )
                    notificationManager.createNotificationChannel(channel)
                    val notification = NotificationCompat.Builder(it, "${ETMSConfig.NOTIFICATION_CHANNEL_ID}_warning")
                        .setContentTitle("Auto-Start Disabled")
                        .setContentText("Please enable auto-start in system settings for reliable task execution.")
                        .setSmallIcon(android.R.drawable.ic_dialog_alert)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .build()
                    notificationManager.notify(System.currentTimeMillis().toInt(), notification)
                }
            }
        }
    }
}