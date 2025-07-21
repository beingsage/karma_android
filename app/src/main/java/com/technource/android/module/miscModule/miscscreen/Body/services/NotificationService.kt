//package com.technource.android.module.miscModule.miscscreen.Body.services
//
//import android.Manifest
//import android.app.*
//import android.content.Context
//import android.content.Intent
//import android.content.pm.PackageManager
//import android.os.Build
//import android.os.IBinder
//import androidx.core.app.ActivityCompat
//import androidx.core.app.NotificationCompat
//import androidx.core.app.NotificationManagerCompat
//import com.technource.android.R
//import com.technource.android.module.miscModule.miscscreen.Body.BodyActivity
//import dagger.hilt.android.AndroidEntryPoint
//import kotlinx.coroutines.*
//import java.util.*
//
//
//@AndroidEntryPoint
//class NotificationService : Service() {
//
//    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
//    private lateinit var notificationManager: NotificationManagerCompat
//
//    companion object {
//        const val CHANNEL_ID_REMINDERS = "health_reminders"
//        const val CHANNEL_ID_ACHIEVEMENTS = "achievements"
//        const val CHANNEL_ID_SYNC = "sync_status"
//
//        const val NOTIFICATION_ID_WATER = 1001
//        const val NOTIFICATION_ID_WORKOUT = 1002
//        const val NOTIFICATION_ID_WEIGHT = 1003
//        const val NOTIFICATION_ID_MEAL = 1004
//        const val NOTIFICATION_ID_SLEEP = 1005
//    }
//
//    override fun onCreate() {
//        super.onCreate()
//        notificationManager = NotificationManagerCompat.from(this)
//        createNotificationChannels()
//    }
//
//    override fun onBind(intent: Intent?): IBinder? = null
//
//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        when (intent?.action) {
//            "SHOW_WATER_REMINDER" -> showWaterReminder()
//            "SHOW_WORKOUT_REMINDER" -> showWorkoutReminder()
//            "SHOW_WEIGHT_REMINDER" -> showWeightReminder()
//            "SHOW_MEAL_REMINDER" -> {
//                val mealType = intent.getStringExtra("meal_type") ?: "meal"
//                showMealReminder(mealType)
//            }
//            "SHOW_SLEEP_REMINDER" -> showSleepReminder()
//            "SHOW_ACHIEVEMENT" -> {
//                val achievement = intent.getStringExtra("achievement") ?: ""
//                showAchievementNotification(achievement)
//            }
//        }
//        return START_NOT_STICKY
//    }
//
//    private fun createNotificationChannels() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val channels = listOf(
//                NotificationChannel(
//                    CHANNEL_ID_REMINDERS,
//                    "Health Reminders",
//                    NotificationManager.IMPORTANCE_DEFAULT
//                ).apply {
//                    description = "Reminders for health activities"
//                    enableVibration(true)
//                    setShowBadge(true)
//                },
//                NotificationChannel(
//                    CHANNEL_ID_ACHIEVEMENTS,
//                    "Achievements",
//                    NotificationManager.IMPORTANCE_HIGH
//                ).apply {
//                    description = "Achievement notifications"
//                    enableVibration(true)
//                    setShowBadge(true)
//                },
//                NotificationChannel(
//                    CHANNEL_ID_SYNC,
//                    "Sync Status",
//                    NotificationManager.IMPORTANCE_LOW
//                ).apply {
//                    description = "Data synchronization status"
//                    setShowBadge(false)
//                }
//            )
//
//            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//            channels.forEach { manager.createNotificationChannel(it) }
//        }
//    }
//
//    private fun showWaterReminder() {
//        val intent = Intent(this, BodyActivity::class.java).apply {
//            putExtra("QUICK_ACTION", "LOG_WATER")
//            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
//        }
//
//        val pendingIntent = PendingIntent.getActivity(
//            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
//
//        val logWaterIntent = Intent(this, BodyActivity::class.java).apply {
//            putExtra("WIDGET_ACTION", "LOG_WATER")
//        }
//        val logWaterPendingIntent = PendingIntent.getActivity(
//            this, 1, logWaterIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
//
//        val notification = NotificationCompat.Builder(this, CHANNEL_ID_REMINDERS)
//            .setSmallIcon(R.drawable.ic_water_drop)
//            .setContentTitle("Time to drink water! 💧")
//            .setContentText("Stay hydrated! Tap to log your water intake.")
//            .setContentIntent(pendingIntent)
//            .addAction(
//                R.drawable.ic_water_drop,
//                "Log 250ml",
//                logWaterPendingIntent
//            )
//            .setAutoCancel(true)
//            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
//            .build()
//
//        if (ActivityCompat.checkSelfPermission(
//                this,
//                Manifest.permission.POST_NOTIFICATIONS
//            ) != PackageManager.PERMISSION_GRANTED
//        ) {
//            // TODO: Consider calling
//            //    ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//            return
//        }
//        notificationManager.notify(NOTIFICATION_ID_WATER, notification)
//    }
//
//    private fun showWorkoutReminder() {
//        val intent = Intent(this, BodyActivity::class.java).apply {
//            putExtra("QUICK_ACTION", "START_WORKOUT")
//            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
//        }
//
//        val pendingIntent = PendingIntent.getActivity(
//            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
//
//        val notification = NotificationCompat.Builder(this, CHANNEL_ID_REMINDERS)
//            .setSmallIcon(R.drawable.ic_fitness)
//            .setContentTitle("Workout Time! 💪")
//            .setContentText("Don't skip your daily exercise. Your body will thank you!")
//            .setContentIntent(pendingIntent)
//            .setAutoCancel(true)
//            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
//            .setStyle(
//                NotificationCompat.BigTextStyle()
//                    .bigText("Don't skip your daily exercise. Your body will thank you! Tap to start your workout session.")
//            )
//            .build()
//
//        if (ActivityCompat.checkSelfPermission(
//                this,
//                Manifest.permission.POST_NOTIFICATIONS
//            ) != PackageManager.PERMISSION_GRANTED
//        ) {
//            // TODO: Consider calling
//            //    ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//            return
//        }
//        notificationManager.notify(NOTIFICATION_ID_WORKOUT, notification)
//    }
//
//    private fun showWeightReminder() {
//        val intent = Intent(this, BodyActivity::class.java).apply {
//            putExtra("QUICK_ACTION", "LOG_WEIGHT")
//            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
//        }
//
//        val pendingIntent = PendingIntent.getActivity(
//            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
//
//        val notification = NotificationCompat.Builder(this, CHANNEL_ID_REMINDERS)
//            .setSmallIcon(R.drawable.ic_scale)
//            .setContentTitle("Daily Weigh-in ⚖️")
//            .setContentText("Track your progress by logging your weight today.")
//            .setContentIntent(pendingIntent)
//            .setAutoCancel(true)
//            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
//            .build()
//
//        if (ActivityCompat.checkSelfPermission(
//                this,
//                Manifest.permission.POST_NOTIFICATIONS
//            ) != PackageManager.PERMISSION_GRANTED
//        ) {
//            // TODO: Consider calling
//            //    ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//            return
//        }
//        notificationManager.notify(NOTIFICATION_ID_WEIGHT, notification)
//    }
//
//    private fun showMealReminder(mealType: String) {
//        val mealEmoji = when (mealType.lowercase()) {
//            "breakfast" -> "🍳"
//            "lunch" -> "🥗"
//            "dinner" -> "🍽️"
//            else -> "🍴"
//        }
//
//        val intent = Intent(this, BodyActivity::class.java).apply {
//            putExtra("QUICK_ACTION", "LOG_MEAL")
//            putExtra("MEAL_TYPE", mealType)
//            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
//        }
//
//        val pendingIntent = PendingIntent.getActivity(
//            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
//
//        val notification = NotificationCompat.Builder(this, CHANNEL_ID_REMINDERS)
//            .setSmallIcon(R.drawable.ic_restaurant)
//            .setContentTitle("${mealType.capitalize()} Time! $mealEmoji")
//            .setContentText("Don't forget to log your ${mealType.lowercase()} for accurate nutrition tracking.")
//            .setContentIntent(pendingIntent)
//            .setAutoCancel(true)
//            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
//            .build()
//
//        if (ActivityCompat.checkSelfPermission(
//                this,
//                Manifest.permission.POST_NOTIFICATIONS
//            ) != PackageManager.PERMISSION_GRANTED
//        ) {
//            // TODO: Consider calling
//            //    ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//            return
//        }
//        notificationManager.notify(NOTIFICATION_ID_MEAL, notification)
//    }
//
//    private fun showSleepReminder() {
//        val intent = Intent(this, BodyActivity::class.java).apply {
//            putExtra("QUICK_ACTION", "LOG_SLEEP")
//            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
//        }
//
//        val pendingIntent = PendingIntent.getActivity(
//            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
//
//        val notification = NotificationCompat.Builder(this, CHANNEL_ID_REMINDERS)
//            .setSmallIcon(R.drawable.ic_bedtime)
//            .setContentTitle("Bedtime Reminder 🌙")
//            .setContentText("Time to wind down for a good night's sleep.")
//            .setContentIntent(pendingIntent)
//            .setAutoCancel(true)
//            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
//            .build()
//
//        if (ActivityCompat.checkSelfPermission(
//                this,
//                Manifest.permission.POST_NOTIFICATIONS
//            ) != PackageManager.PERMISSION_GRANTED
//        ) {
//            // TODO: Consider calling
//            //    ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//            return
//        }
//        notificationManager.notify(NOTIFICATION_ID_SLEEP, notification)
//    }
//
//    private fun showAchievementNotification(achievement: String) {
//        val intent = Intent(this, BodyActivity::class.java).apply {
//            putExtra("SHOW_ACHIEVEMENT", achievement)
//            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
//        }
//
//        val pendingIntent = PendingIntent.getActivity(
//            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
//
//        val notification = NotificationCompat.Builder(this, CHANNEL_ID_ACHIEVEMENTS)
//            .setSmallIcon(R.drawable.ic_trophy)
//            .setContentTitle("Achievement Unlocked! 🏆")
//            .setContentText(achievement)
//            .setContentIntent(pendingIntent)
//            .setAutoCancel(true)
//            .setPriority(NotificationCompat.PRIORITY_HIGH)
//            .setStyle(
//                NotificationCompat.BigTextStyle()
//                    .bigText("Congratulations! You've achieved: $achievement. Keep up the great work!")
//            )
//            .build()
//
//        if (ActivityCompat.checkSelfPermission(
//                this,
//                Manifest.permission.POST_NOTIFICATIONS
//            ) != PackageManager.PERMISSION_GRANTED
//        ) {
//            // TODO: Consider calling
//            //    ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//            return
//        }
//        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        serviceScope.cancel()
//    }
//}
