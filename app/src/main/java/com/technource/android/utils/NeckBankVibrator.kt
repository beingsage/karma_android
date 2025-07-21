package com.technource.android.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.technource.android.R
import android.util.Log

object NeckbandVibrationUtil {
    private const val TAG = "NeckbandVibrationUtil"

    fun triggerHiddenVibration(context: Context) {
        Log.d(TAG, "Attempting to trigger neckband vibration")

        // Set audio mode to simulate incoming call
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        try {
            audioManager.isBluetoothA2dpOn = true // Encourage Bluetooth audio routing
            audioManager.mode = AudioManager.MODE_IN_CALL // Mimic incoming call audio context
            Log.d(TAG, "AudioManager set to MODE_IN_CALL, Bluetooth A2DP: ${audioManager.isBluetoothA2dpOn}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set AudioManager properties: ${e.message}")
        }

        // 1. Play a silent audio file with USAGE_VOICE_COMMUNICATION to mimic call audio
        var mediaPlayer: MediaPlayer? = null
        try {
            mediaPlayer = MediaPlayer.create(context, R.raw.raw_audio)
            if (mediaPlayer == null) {
                Log.e(TAG, "Failed to create MediaPlayer for raw_audio.mp3")
                return
            }
            mediaPlayer.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION) // Mimic call audio
                    .build()
            )
            mediaPlayer.setVolume(0f, 0f) // Mute the audio
            mediaPlayer.setOnPreparedListener {
                Log.d(TAG, "MediaPlayer prepared, starting playback")
                it.start()
            }
            mediaPlayer.setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                true
            }
            mediaPlayer.prepareAsync() // Use async to avoid state issues
        } catch (e: Exception) {
            Log.e(TAG, "MediaPlayer setup failed: ${e.message}")
            mediaPlayer?.release()
        }

        // 2. Create a high-priority notification to mimic incoming call
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "vibration_channel"

        // Create notification channel for Android 8.0+ (Oreo)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Incoming Call Simulation",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 200, 100, 200, 100, 200) // Mimic incoming call vibration
                setSound(null, null) // No sound to keep it silent
            }
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created: $channelId")
        }

        // Build the notification
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_phone_call) // Call-related icon
            .setContentTitle("Incoming Call")
            .setContentText("Simulating incoming call for neckband vibration")
            .setPriority(NotificationCompat.PRIORITY_MAX) // Max priority for call-like behavior
            .setCategory(NotificationCompat.CATEGORY_CALL) // Mimic incoming call
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 200, 100, 200, 100, 200)) // Call-like vibration pattern
            .setSound(null) // Ensure no sound

        // Show the notification
        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted, skipping notification")
                return@with
            }
            notify(1, builder.build())
            Log.d(TAG, "Notification sent to trigger neckband vibration")
        }

        // Release MediaPlayer resources
        mediaPlayer?.setOnCompletionListener {
            Log.d(TAG, "MediaPlayer completed, releasing")
            it.release()
            // Reset AudioManager mode
            try {
                audioManager.mode = AudioManager.MODE_NORMAL
                Log.d(TAG, "AudioManager reset to MODE_NORMAL")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reset AudioManager mode: ${e.message}")
            }
        }
    }
}

/*
 * Instructions:
 * 1. Ensure a silent MP3 file (0.1 seconds long) is placed in `res/raw/raw_audio.mp3`. Create it using Audacity or download from a free online source (e.g., freesound.org).
 * 2. Verify `AndroidManifest.xml` includes the following permissions (ensure CALL_PHONE and USE_FULL_SCREEN_INTENT are removed):
 * ```xml
 * <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
 * <uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
 * <uses-permission android:name="android.permission.BLUETOOTH" />
 * <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
 * <uses-permission android:name="android.permission.VIBRATE" />
 * ```
 * 3. For Android 13+ (API 33+), request the POST_NOTIFICATIONS permission at runtime in your activity (e.g., SettingsScreen):
 * ```kotlin
 * if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
 *     ActivityCompat.requestPermissions(
 *         this,
 *         arrayOf(Manifest.permission.POST_NOTIFICATIONS),
 *         1
 *     )
 * }
 * ```
 * 4. Ensure the neckband (e.g., boAt Rockerz 235 Pro) is connected via Bluetooth before triggering.
 * 5. Call this function from your settings page as: `NeckbandVibrationUtil.triggerHiddenVibration(this)`.
 * 6. Check logs (filter by "NeckbandVibrationUtil") to verify MediaPlayer and notification behavior:
 *    - Use Logcat filter: `tag:NeckbandVibrationUtil`
 *    - Look for "MediaPlayer prepared", "Notification sent", and any errors.
 * 7. If the neckband doesn't vibrate, try changing AudioAttributes.USAGE_ to USAGE_NOTIFICATION or USAGE_ALARM:
 * ```kotlin
 * .setUsage(AudioAttributes.USAGE_NOTIFICATION)
 * ```
 * 8. Alternatively, try AudioManager.MODE_RINGING instead of MODE_IN_CALL:
 * ```kotlin
 * audioManager.mode = AudioManager.MODE_RINGING
 * ```
 *
 * Important Notes:
 * - This approach mimics an incoming call using a notification and call audio mode. However, if the neckband (e.g., boAt Rockerz 235 Pro) only vibrates for actual incoming calls, Android's public APIs cannot fully simulate this due to system restrictions.
 * - The MediaPlayer error (state 8) was fixed with async preparation and error handling.
 * - If this doesn't trigger the neckband, alternative solutions include:
 *   - **Controlled Outgoing Call**: Initiate and immediately cancel an outgoing call to a dummy number (requires CALL_PHONE permission, may show brief UI).
 *   - **Secondary Device**: Use another phone to send an actual incoming call (impractical).
 *   - **BLE Reverse-Engineering**: Capture the boAt app's Bluetooth commands (advanced, requires tools like Wireshark).
 *   - **Switch Hardware**: Use a neckband/fitness band with explicit BLE vibration control (e.g., Xiaomi Smart Band).
 * - If the notification or audio triggers the phone's vibration instead, confirm the neckband's behavior with the boAt app or actual incoming calls.
 */