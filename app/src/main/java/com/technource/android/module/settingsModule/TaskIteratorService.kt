package com.technource.android.module.settingsModule

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.technource.android.ETMS.micro.TTSManager
import com.technource.android.ETMS.micro.TaskIterator
import com.technource.android.R
import com.technource.android.local.TaskDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TaskIteratorService : Service() {
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var ttsManager: TTSManager
    private lateinit var taskIterator: TaskIterator

    override fun onCreate() {
        super.onCreate()
        taskIterator = TaskIterator(applicationContext, taskDao, ttsManager)
        startForeground(1, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        CoroutineScope(Dispatchers.Default).launch {
            taskIterator.start()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val channelId = "TaskIteratorChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Task Iterator Service",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Task Iterator Running")
            .setContentText("Processing tasks in the background")
            .setSmallIcon(R.drawable.ic_notification)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        taskIterator.stop()
    }
}