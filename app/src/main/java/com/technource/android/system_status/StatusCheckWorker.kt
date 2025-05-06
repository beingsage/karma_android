package com.technource.android.system_status

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class StatusCheckWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        SystemStatus.pingServices(applicationContext)
        return Result.success()
    }
}

fun scheduleStatusCheck(context: Context) {
    val request = PeriodicWorkRequestBuilder<StatusCheckWorker>(15, TimeUnit.MINUTES)
        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        .build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "status_check",
        ExistingPeriodicWorkPolicy.KEEP,
        request
    )
}