//package com.technource.android.module.miscModule.miscscreen.Body.repository
//
//import android.app.Service
//import android.content.Intent
//import android.os.IBinder
//import androidx.work.*
//
//import dagger.hilt.android.AndroidEntryPoint
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.SupervisorJob
//import kotlinx.coroutines.launch
//import java.util.concurrent.TimeUnit
//import javax.inject.Inject
//
//@AndroidEntryPoint
//class SyncService : Service() {
//
//    @Inject
//    lateinit var repository: BodyTrackRepository
//
//    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
//
//    override fun onBind(intent: Intent?): IBinder? = null
//
//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        when (intent?.action) {
//            ACTION_SYNC_NOW -> performImmediateSync()
//            ACTION_SCHEDULE_SYNC -> schedulePeriodicSync()
//        }
//        return START_NOT_STICKY
//    }
//
//    private fun performImmediateSync() {
//        serviceScope.launch {
//            try {
//                repository.syncWithWebApp()
//            } catch (e: Exception) {
//                // Handle sync error
//            } finally {
//                stopSelf()
//            }
//        }
//    }
//
//    private fun schedulePeriodicSync() {
//        val constraints = Constraints.Builder()
//            .setRequiredNetworkType(NetworkType.CONNECTED)
//            .setRequiresBatteryNotLow(true)
//            .build()
//
//        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
//            repeatInterval = 15,
//            repeatIntervalTimeUnit = TimeUnit.MINUTES
//        )
//            .setConstraints(constraints)
//            .build()
//
//        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
//            "sync_work",
//            ExistingPeriodicWorkPolicy.KEEP,
//            syncRequest
//        )
//
//        stopSelf()
//    }
//
//    companion object {
//        const val ACTION_SYNC_NOW = "com.bodytrack.SYNC_NOW"
//        const val ACTION_SCHEDULE_SYNC = "com.bodytrack.SCHEDULE_SYNC"
//    }
//}
//
//class SyncWorker(
//    context: android.content.Context,
//    params: WorkerParameters
//) : CoroutineWorker(context, params) {
//
//    override suspend fun doWork(): Result {
//        return try {
//            // Perform sync operation
//            Result.success()
//        } catch (e: Exception) {
//            Result.retry()
//        }
//    }
//}
