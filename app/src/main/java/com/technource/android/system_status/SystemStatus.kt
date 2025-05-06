package com.technource.android.system_status

import android.Manifest
import android.app.NotificationManager
import android.app.WallpaperManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

object SystemStatus {
    private lateinit var database: ServiceLogDatabase
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    fun initialize(context: Context) {
        database = ServiceLogDatabase.getDatabase(context)
    }

    fun logEvent(serviceName: String, log: String) {
        scope.launch {
            database.serviceLogDao().insertLog(ServiceLog(
                serviceName = serviceName,
                timestamp = System.currentTimeMillis(),
                log = log,
                status = "N/A"
            ))
        }
    }

    fun logStatus(serviceName: String, status: String) {
        scope.launch {
            database.serviceLogDao().insertLog(ServiceLog(
                serviceName = serviceName,
                timestamp = System.currentTimeMillis(),
                log = "Status updated to $status",
                status = status
            ))
        }
    }

    fun pingServices(context: Context) {
        scope.launch {
            // TTS
            val tts = TextToSpeech(context) { status ->
                logStatus("TTS", if (status == TextToSpeech.SUCCESS) "Running" else "Failed")
            }
            // Vibration
            val vibrator = ContextCompat.getSystemService(context, Vibrator::class.java) as Vibrator
            logStatus("Vibration", if (vibrator.hasVibrator()) "Running" else "Failed")
            // Wallpaper
            val wallpaperManager = WallpaperManager.getInstance(context)
            logStatus("Wallpaper", try { wallpaperManager.drawable; "Running" } catch (e: Exception) { "Failed" })
            // Alarm
            logStatus("Alarm", if (context.checkSelfPermission(Manifest.permission.SET_ALARM) == PackageManager.PERMISSION_GRANTED) "Running" else "Failed")
            // Widget
            logStatus("Widget", "Not Implemented")
            // DND Check
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (notificationManager.isNotificationPolicyAccessGranted) {
                if (notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL) {
                    logEvent("Alarm", "DND active, may affect alarms")
                }
            } else {
                logEvent("Alarm", "DND permission not granted")
            }
        }
    }
}