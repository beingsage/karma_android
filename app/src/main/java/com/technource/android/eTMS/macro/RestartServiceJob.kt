package com.technource.android.eTMS.macro

import android.app.job.JobParameters
import android.app.job.JobService
import com.technource.android.system_status.SystemStatus

// Add this class within EternalTimeTableUnitService.kt
class RestartServiceJob : JobService() {
    override fun onStartJob(params: JobParameters?): Boolean {

        //  val serviceIntent = Intent(this, EternalTimeTableUnitService::class.java)
        // ContextCompat.startForegroundService(this, serviceIntent)
        // SystemStatus.logEvent("RestartServiceJob", "Service restarted via JobScheduler")
        // jobFinished(params, false)
        // return false
        // DO NOT start foreground service directly here on Android 14+
        // Instead, use WorkManager or send a notification to the user to reopen the app
        SystemStatus.logEvent("RestartServiceJob", "Job started, but foreground service cannot be started from background on Android 14+")
        jobFinished(params, false)
        return false
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        SystemStatus.logEvent("RestartServiceJob", "Job stopped, rescheduling")
        return true // Reschedule if stopped
    }
}