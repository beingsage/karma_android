package com.technource.android.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.technource.android.R
import com.technource.android.TaskApplication
import com.technource.android.module.homeModule.HomeScreen
import com.technource.android.system_status.SystemStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PermissionActivity : AppCompatActivity() {

    @Inject lateinit var voiceAssistantManager: VoiceAssistantManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val permissionsToRequest = mutableListOf<String>()
    private val specialPermissions = mutableListOf<String>()
    private val REQUEST_CODE_PERMISSIONS = 1001
    private val REQUEST_CODE_SPECIAL = 1002

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission)

        val requestButton = findViewById<Button>(R.id.btnRequestPermissions)
        val statusText = findViewById<TextView>(R.id.tvPermissionStatus)
        val settingsContainer = findViewById<View>(R.id.settingsContainer)
        val proceedButton = findViewById<Button>(R.id.btnProceed)


        // Handle notification permission request from service
        if (intent?.action == "REQUEST_NOTIFICATION_PERMISSION" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Notification Permission Required")
                .setMessage("Notifications are required for task reminders and service reliability. Please grant the permission.")
                .setPositiveButton("Grant") { _, _ ->
                    ActivityCompat.requestPermissions(
                        this@PermissionActivity,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        REQUEST_CODE_PERMISSIONS
                    )
                }
                .setNegativeButton("Deny") { _, _ ->
                    SystemStatus.logEvent("PermissionActivity", "Notification permission denied by user")
                    Toast.makeText(this, "Notifications disabled; some features may not work", Toast.LENGTH_LONG).show()
                    finish()
                }
                .setCancelable(false)
                .show()
        }

        // Check notification permission periodically
        scope.launch {
            while (isActive) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasPermission(Manifest.permission.POST_NOTIFICATIONS)) {
                    if (!(isFinishing || isDestroyed)) {
                        MaterialAlertDialogBuilder(this@PermissionActivity)
                            .setTitle("Notification Permission Required")
                            .setMessage("Notifications are required for task reminders. Please grant the permission.")
                            .setPositiveButton("Grant") { _, _ ->
                                ActivityCompat.requestPermissions(
                                    this@PermissionActivity,
                                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                                    REQUEST_CODE_PERMISSIONS
                                )
                            }
                            .setNegativeButton("Deny") { _, _ ->
                                SystemStatus.logEvent("PermissionActivity", "Notification permission denied")
                            }
                            .show()
                    }
                }
                delay(60_000) // Check every minute
            }
        }

        checkAndRequestPermissions()

        requestButton.setOnClickListener {
            if (permissionsToRequest.isNotEmpty()) {
                permissionsToRequest.forEach { permission ->
                    if (shouldShowRationale(permission)) {
                        showPermissionRationale(permission)
                    }
                }
                ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toTypedArray(),
                    REQUEST_CODE_PERMISSIONS
                )
            } else if (specialPermissions.isNotEmpty()) {
                requestSpecialPermission(specialPermissions.first())
            } else {
                settingsContainer.visibility = View.VISIBLE
                requestButton.visibility = View.GONE
                statusText.text = "All permissions granted. Configure settings below."
                setupSettingsToggles()
            }
        }

        proceedButton.setOnClickListener {
            startServicesAndProceed()
        }
    }

    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun checkAndRequestPermissions() {
        permissionsToRequest.clear()
        specialPermissions.clear()

        // Dangerous permissions
        val dangerousPermissions = listOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION,
            Manifest.permission.CAMERA,
            Manifest.permission.FOREGROUND_SERVICE_MICROPHONE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.FOREGROUND_SERVICE_LOCATION
        ).filter { Build.VERSION.SDK_INT < Build.VERSION_CODES.R || it != Manifest.permission.READ_EXTERNAL_STORAGE && it != Manifest.permission.WRITE_EXTERNAL_STORAGE }

        dangerousPermissions.forEach { permission ->
            if (!hasPermission(permission)) {
                permissionsToRequest.add(permission)
                SystemStatus.logEvent("PermissionActivity", "Permission needed: $permission")
            }
        }

        // Special permissions
        val specialPerms = listOf(
            Manifest.permission.SCHEDULE_EXACT_ALARM,
            Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Manifest.permission.SYSTEM_ALERT_WINDOW,
            Manifest.permission.MANAGE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_NOTIFICATION_POLICY // Added notification policy access
        ).filter { Build.VERSION.SDK_INT >= Build.VERSION_CODES.R || it != Manifest.permission.MANAGE_EXTERNAL_STORAGE }

        specialPerms.forEach { permission ->
            if (!hasPermission(permission)) {
                specialPermissions.add(permission)
                SystemStatus.logEvent("PermissionActivity", "Special permission needed: $permission")
            }
        }

        val prefs = getSharedPreferences("etms_prefs", Context.MODE_PRIVATE)
        val hasShownBatteryPrompt = prefs.getBoolean("has_shown_battery_prompt", false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !hasPermission(Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS) && !hasShownBatteryPrompt) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Disable Battery Optimization")
                .setMessage("To ensure uninterrupted task management, please disable battery optimization for this app.")
                .setPositiveButton("Grant") { _, _ ->
                    requestSpecialPermission(Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    prefs.edit().putBoolean("has_shown_battery_prompt", true).apply()
                }
                .setNegativeButton("Later") { _, _ ->
                    SystemStatus.logEvent("PermissionActivity", "Battery optimization permission deferred")
                    Toast.makeText(this, "Battery optimization must be disabled for reliable task execution", Toast.LENGTH_LONG).show()
                    prefs.edit().putBoolean("has_shown_battery_prompt", true).apply()
                }
                .setCancelable(false)
                .show()
        }

        if (permissionsToRequest.isEmpty() && specialPermissions.isEmpty()) {
            findViewById<View>(R.id.settingsContainer).visibility = View.VISIBLE
            findViewById<Button>(R.id.btnRequestPermissions).visibility = View.GONE
            findViewById<TextView>(R.id.tvPermissionStatus).text = "All permissions granted. Configure settings below."
            setupSettingsToggles()
        } else {
            val allPermissions = (permissionsToRequest + specialPermissions).joinToString(", ")
            findViewById<TextView>(R.id.tvPermissionStatus).text = "Please grant the following permissions: $allPermissions"
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return when (permission) {
            Manifest.permission.SCHEDULE_EXACT_ALARM -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                    alarmManager.canScheduleExactAlarms()
                } else true
            }
            Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS -> {
                val powerManager = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
                powerManager.isIgnoringBatteryOptimizations(packageName)
            }
            Manifest.permission.SYSTEM_ALERT_WINDOW -> {
                Settings.canDrawOverlays(this)
            }
            Manifest.permission.MANAGE_EXTERNAL_STORAGE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Environment.isExternalStorageManager()
                } else true
            }
            Manifest.permission.ACCESS_NOTIFICATION_POLICY -> {
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                notificationManager.isNotificationPolicyAccessGranted
            }
            else -> ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun shouldShowRationale(permission: String): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
    }

    private fun showPermissionRationale(permission: String) {
        val rationaleMessage = when (permission) {
            Manifest.permission.RECORD_AUDIO -> "Microphone access is needed for voice commands."
            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE -> "Storage access is needed to save and read files."
            Manifest.permission.POST_NOTIFICATIONS -> "Notification permission is needed for task reminders."
            Manifest.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION -> "This permission is needed to display persistent widgets and overlays during screen sharing or projection."
            Manifest.permission.CAMERA -> "Camera access is needed for capturing images."
            Manifest.permission.FOREGROUND_SERVICE_MICROPHONE -> "This permission is needed for microphone usage in the foreground service."
            else -> "This permission is required for the app to function properly."
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Permission Required")
            .setMessage(rationaleMessage)
            .setPositiveButton("Grant") { _, _ ->
                ActivityCompat.requestPermissions(this, arrayOf(permission), REQUEST_CODE_PERMISSIONS)
            }
            .setNegativeButton("Deny") { _, _ ->
                SystemStatus.logEvent("PermissionActivity", "Permission rationale denied: $permission")
            }
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @SuppressLint("BatteryLife")
    private fun requestSpecialPermission(permission: String) {
        when (permission) {
            Manifest.permission.SCHEDULE_EXACT_ALARM -> {
                try {
                    startActivityForResult(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.fromParts("package", packageName, null)
                        },
                        REQUEST_CODE_SPECIAL
                    )
                } catch (e: Exception) {
                    SystemStatus.logEvent("PermissionActivity", "Failed to open exact alarm settings: ${e.message}")
                    Toast.makeText(this, "Unable to open exact alarm settings", Toast.LENGTH_SHORT).show()
                    specialPermissions.remove(permission)
                    checkAndRequestPermissions()
                }
            }
            Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS -> {
                try {
                    startActivityForResult(
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.fromParts("package", packageName, null)
                        },
                        REQUEST_CODE_SPECIAL
                    )
                } catch (e: Exception) {
                    SystemStatus.logEvent("PermissionActivity", "Failed to open battery optimization settings: ${e.message}")
                    Toast.makeText(this, "Unable to open battery optimization settings", Toast.LENGTH_SHORT).show()
                    specialPermissions.remove(permission)
                    checkAndRequestPermissions()
                }
            }
            Manifest.permission.SYSTEM_ALERT_WINDOW -> {
                try {
                    startActivityForResult(
                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                            data = Uri.fromParts("package", packageName, null)
                        },
                        REQUEST_CODE_SPECIAL
                    )
                } catch (e: Exception) {
                    SystemStatus.logEvent("PermissionActivity", "Failed to open overlay settings: ${e.message}")
                    Toast.makeText(this, "Unable to open overlay settings", Toast.LENGTH_SHORT).show()
                    specialPermissions.remove(permission)
                    checkAndRequestPermissions()
                }
            }
            Manifest.permission.MANAGE_EXTERNAL_STORAGE -> {
                try {
                    startActivityForResult(
                        Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                            data = Uri.fromParts("package", packageName, null)
                        },
                        REQUEST_CODE_SPECIAL
                    )
                } catch (e: Exception) {
                    SystemStatus.logEvent("PermissionActivity", "Failed to open storage settings: ${e.message}")
                    Toast.makeText(this, "Unable to open storage settings", Toast.LENGTH_SHORT).show()
                    specialPermissions.remove(permission)
                    checkAndRequestPermissions()
                }
            }
            Manifest.permission.ACCESS_NOTIFICATION_POLICY -> {
                try {
                    startActivityForResult(
                        Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS),
                        REQUEST_CODE_SPECIAL
                    )
                } catch (e: Exception) {
                    SystemStatus.logEvent("PermissionActivity", "Failed to open notification policy settings: ${e.message}")
                    Toast.makeText(this, "Unable to open notification policy settings", Toast.LENGTH_SHORT).show()
                    specialPermissions.remove(permission)
                    checkAndRequestPermissions()
                }
            }
        }
        SystemStatus.logEvent("PermissionActivity", "Requesting special permission: $permission")
    }

    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            val deniedPermissions = permissions.filterIndexed { index, permission ->
                if (grantResults[index] == PackageManager.PERMISSION_GRANTED) {
                    SystemStatus.logEvent("PermissionActivity", "Permission granted: $permission")
                    false
                } else {
                    SystemStatus.logEvent("PermissionActivity", "Permission denied: $permission")
                    true
                }
            }
            if (deniedPermissions.isEmpty()) {
                checkAndRequestPermissions()
            } else {
                Toast.makeText(
                    this,
                    "Some permissions were denied: ${deniedPermissions.joinToString(", ")}. Some features may not work.",
                    Toast.LENGTH_LONG
                ).show()
                findViewById<TextView>(R.id.tvPermissionStatus).text =
                    "Denied permissions: ${deniedPermissions.joinToString(", ")}"
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SPECIAL) {
            val permission = specialPermissions.firstOrNull()
            when (permission) {
                Manifest.permission.ACCESS_NOTIFICATION_POLICY -> {
                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                    if (notificationManager.isNotificationPolicyAccessGranted) {
                        SystemStatus.logEvent("PermissionActivity", "Notification policy access granted")
                    } else {
                        SystemStatus.logEvent("PermissionActivity", "Notification policy access denied")
                        Toast.makeText(this, "Notification policy access denied", Toast.LENGTH_SHORT).show()
                    }
                }
                Manifest.permission.SCHEDULE_EXACT_ALARM -> {
                    val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                        SystemStatus.logEvent("PermissionActivity", "Exact alarm permission granted")
                    } else {
                        SystemStatus.logEvent("PermissionActivity", "Exact alarm permission denied")
                        Toast.makeText(this, "Exact alarm permission denied", Toast.LENGTH_SHORT).show()
                    }
                }
                Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS -> {
                    val powerManager = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
                    if (powerManager.isIgnoringBatteryOptimizations(packageName)) {
                        SystemStatus.logEvent("PermissionActivity", "Battery optimization permission granted")
                    } else {
                        SystemStatus.logEvent("PermissionActivity", "Battery optimization permission denied")
                        Toast.makeText(this, "Battery optimization permission denied", Toast.LENGTH_SHORT).show()
                    }
                }
                Manifest.permission.SYSTEM_ALERT_WINDOW -> {
                    if (Settings.canDrawOverlays(this)) {
                        SystemStatus.logEvent("PermissionActivity", "Overlay permission granted")
                    } else {
                        SystemStatus.logEvent("PermissionActivity", "Overlay permission denied")
                        Toast.makeText(this, "Overlay permission denied", Toast.LENGTH_SHORT).show()
                    }
                }
                Manifest.permission.MANAGE_EXTERNAL_STORAGE -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
                        SystemStatus.logEvent("PermissionActivity", "Manage storage permission granted")
                    } else {
                        SystemStatus.logEvent("PermissionActivity", "Manage storage permission denied")
                        Toast.makeText(this, "Manage storage permission denied", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            specialPermissions.removeAt(0)
            checkAndRequestPermissions()
        }
    }

    private fun setupSettingsToggles() {
        val ttsSwitch = findViewById<SwitchMaterial>(R.id.switchTts)
        val notificationsSwitch = findViewById<SwitchMaterial>(R.id.switchNotifications)
        val vibrationSwitch = findViewById<SwitchMaterial>(R.id.switchVibration)
        val taskLoggerSwitch = findViewById<SwitchMaterial>(R.id.switchTaskLogger)
        val widgetsSwitch = findViewById<SwitchMaterial>(R.id.switchWidgets)
        val voiceCommandsSwitch = findViewById<SwitchMaterial>(R.id.switchVoiceCommands)

        scope.launch {
            PreferencesManager.isTtsEnabled().collect { ttsSwitch.isChecked = it }
            PreferencesManager.isNotificationsEnabled().collect { notificationsSwitch.isChecked = it }
            PreferencesManager.isVibrationEnabled().collect { vibrationSwitch.isChecked = it }
            PreferencesManager.isTaskLoggerEnabled().collect { taskLoggerSwitch.isChecked = it }
            PreferencesManager.isWidgetsEnabled().collect { widgetsSwitch.isChecked = it }
            PreferencesManager.isVoiceCommandsEnabled().collect { voiceCommandsSwitch.isChecked = it }
        }

        ttsSwitch.setOnCheckedChangeListener { _, isChecked ->
            scope.launch {
                PreferencesManager.setTtsEnabled(isChecked)
                SystemStatus.logEvent("PermissionActivity", "TTS preference set to $isChecked")
            }
        }
        notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            scope.launch {
                PreferencesManager.setNotificationsEnabled(isChecked)
                SystemStatus.logEvent("PermissionActivity", "Notifications preference set to $isChecked")
            }
        }
        vibrationSwitch.setOnCheckedChangeListener { _, isChecked ->
            scope.launch {
                PreferencesManager.setVibrationEnabled(isChecked)
                SystemStatus.logEvent("PermissionActivity", "Vibration preference set to $isChecked")
            }
        }
        taskLoggerSwitch.setOnCheckedChangeListener { _, isChecked ->
            scope.launch {
                PreferencesManager.setTaskLoggerEnabled(isChecked)
                SystemStatus.logEvent("PermissionActivity", "Task Logger preference set to $isChecked")
            }
        }
        widgetsSwitch.setOnCheckedChangeListener { _, isChecked ->
            scope.launch {
                PreferencesManager.setWidgetsEnabled(isChecked)
                SystemStatus.logEvent("PermissionActivity", "Widgets preference set to $isChecked")
            }
        }
        voiceCommandsSwitch.setOnCheckedChangeListener { _, isChecked ->
            scope.launch {
                PreferencesManager.setVoiceCommandsEnabled(isChecked)
                SystemStatus.logEvent("PermissionActivity", "Voice Commands preference set to $isChecked")
            }
        }
    }

    private fun startServicesAndProceed() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Battery Optimization")
                .setMessage("To ensure the app runs reliably in the background, please disable battery optimization for this app.")
                .setPositiveButton("Open Settings") { _, _ ->
                    try {
                        startActivityForResult(
                            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.fromParts("package", packageName, null)
                            },
                            REQUEST_CODE_SPECIAL
                        )
                    } catch (e: Exception) {
                        SystemStatus.logEvent("PermissionActivity", "Failed to open battery optimization settings: ${e.message}")
                        Toast.makeText(this, "Unable to open battery optimization settings", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel") { _, _ ->
                    SystemStatus.logEvent("PermissionActivity", "User declined battery optimization exemption")
                    proceedToHome()
                }
                .show()
        } else {
            proceedToHome()
        }
    }

    private fun proceedToHome() {
        TaskApplication.instance.startServices()
        SystemStatus.logEvent("PermissionActivity", "All services started")
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
            PreferencesManager.isVoiceCommandsEnabledSync()) {
            try {
                voiceAssistantManager.startWakeWordDetection()
                SystemStatus.logEvent("PermissionActivity", "VoiceAssistant initialized")
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to initialize voice assistant", Toast.LENGTH_SHORT).show()
                SystemStatus.logEvent("PermissionActivity", "VoiceAssistant initialization failed: ${e.message}")
            }
        }
        startActivity(Intent(this, HomeScreen::class.java))
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}