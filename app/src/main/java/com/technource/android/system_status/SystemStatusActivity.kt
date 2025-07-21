package com.technource.android.system_status

import android.Manifest
import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.webkit.WebView
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.technource.android.R
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SystemStatusActivity : AppCompatActivity() {
    private lateinit var database: ServiceLogDatabase
//    private lateinit var webView: WebView

    // Helper function to create cell with selectable text
    private fun createCell(text: String, isHeader: Boolean): TextView {
        return TextView(this).apply {
            this.text = text
            setTextColor(if (isHeader) Color.WHITE else Color.LTGRAY)
            setPadding(8, 4, 8, 4)
            setTextIsSelectable(true) // Make text selectable

            layoutParams = TableRow.LayoutParams(
                if (isHeader) 120.dpToPx() else TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT
            )

            if (isHeader) {
                setTypeface(typeface, Typeface.BOLD)
                setBackgroundColor(Color.DKGRAY)
            } else {
                isSingleLine = false
                maxLines = Integer.MAX_VALUE
                setHorizontallyScrolling(true)
                ellipsize = null
                scrollBarStyle = View.SCROLLBARS_INSIDE_INSET
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = true
            }
        }
    }

    // Extension function to convert dp to px
    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    // Helper function to format timestamp
    private fun formatTime(timestamp: Long): String {
        return SimpleDateFormat("MM/dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
    }

    // Helper function to copy text to clipboard
    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Service Logs", text)
        clipboard.setPrimaryClip(clip)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_system_status)
        database = ServiceLogDatabase.getDatabase(this)
        // SystemStatus.initialize(this)

         // Initialize WebView for flowchart
//        webView = WebView(this).apply {
//            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, 600) // Adjust height as needed
//            setBackgroundColor(Color.WHITE)
//        }
//        SystemStatus.initialize(this, webView)
//
//         // Add WebView to layout
//        val unitContainer = findViewById<LinearLayout>(R.id.unitContainer)
//        unitContainer.addView(webView, 0)

        
        requestPermissions()
        checkDeviceServices()
        setupUnitTables()
        setupGlobalDeleteButton()
    }

    @SuppressLint("BatteryLife")
    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.VIBRATE,
            Manifest.permission.SET_ALARM,
            Manifest.permission.SET_WALLPAPER
        )
        requestPermissions(permissions, 100)
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            })
        }
    }

    private fun setupGlobalDeleteButton() {
        val unitContainer = findViewById<LinearLayout>(R.id.unitContainer)
        val deleteAllButton = Button(this).apply {
            text = "Delete All Logs"
            setBackgroundColor(Color.RED)
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                bottomMargin = 8
            }
            setOnClickListener {
                lifecycleScope.launch {
                    database.serviceLogDao().deleteAllLogs()
                }
            }
        }
        unitContainer.addView(deleteAllButton, 0) // Add at the top
    }

    @SuppressLint("SetTextI18n")
    private fun setupUnitTables() {
        val unitContainer = findViewById<LinearLayout>(R.id.unitContainer)
        val services = listOf("EternalTimeTableUnitService","HomeScreen", "TaskViewModel", "TimelineView", "TaskAdapter",
            "TTS", "TaskIteratorSimulator", "Vibration", "Wallpaper", "Alarm", "Widget",
            "TaskSyncWorker", "StatsPage", "TaskCompletionChart", "SettingsActivity",
            "TimeTableManager", "TaskLoggerActivity", "TaskIterator", "StatsViewModel",
            "DefaultTaskFetcher", "LockScreenService", "StatsCalculator", "TaskFetcher",
            "DefaultTimeTableComparison", "System", "Network", "SystemStatusService"
        )

        lifecycleScope.launch {
            services.forEach { service ->
                val unitLayout = LinearLayout(this@SystemStatusActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setBackgroundColor(Color.parseColor("#333333"))
                    setPadding(8, 8, 8, 8)
                    layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                        bottomMargin = 8
                    }
                }

                val title = TextView(this@SystemStatusActivity).apply {
                    text = service
                    textSize = 18f
                    setTextColor(Color.WHITE)
                    setTypeface(null, Typeface.BOLD)
                }
                unitLayout.addView(title)

                val status = TextView(this@SystemStatusActivity).apply {
                    text = "Status: Checking..."
                    setTextColor(Color.YELLOW)
                }
                unitLayout.addView(status)

                val deleteButton = Button(this@SystemStatusActivity).apply {
                    text = "Delete $service Logs"
                    setBackgroundColor(Color.RED)
                    setTextColor(Color.WHITE)
                    layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                        bottomMargin = 8
                    }
                    setOnClickListener {
                        lifecycleScope.launch {
                            database.serviceLogDao().deleteLogsForService(service)
                        }
                    }
                }

                // Add copy button
                val copyButton = Button(this@SystemStatusActivity).apply {
                    text = "Copy All Logs"
                    setBackgroundColor(Color.BLUE)
                    setTextColor(Color.WHITE)
                    layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                        bottomMargin = 8
                        marginStart = 8
                    }
                }

                // Create horizontal layout for buttons and add both buttons
                val buttonLayout = LinearLayout(this@SystemStatusActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                }
                buttonLayout.addView(deleteButton)
                buttonLayout.addView(copyButton)
                unitLayout.addView(buttonLayout)

                val scrollView = HorizontalScrollView(this@SystemStatusActivity).apply {
                    layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                    isFillViewport = false
                    isHorizontalScrollBarEnabled = true
                    overScrollMode = HorizontalScrollView.OVER_SCROLL_ALWAYS
                    minimumWidth = 0
                }

                val table = TableLayout(this@SystemStatusActivity).apply {
                    setBackgroundColor(Color.BLACK)
                    layoutParams = TableLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                        weight = 1f
                    }
                }

                val headerRow = TableRow(this@SystemStatusActivity).apply {
                    layoutParams = TableLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                    addView(createCell("Timestamp", true))
                    addView(createCell("Log", true))
                }
                table.addView(headerRow)

                scrollView.addView(table)
                unitLayout.addView(scrollView)

                database.serviceLogDao().getLogs(service).observe(this@SystemStatusActivity) { logs ->
                    status.text = "Status: ${logs.firstOrNull()?.status ?: "Unknown"}"
                    status.setTextColor(when (logs.firstOrNull()?.status) {
                        "Running" -> Color.GREEN
                        "Failed" -> Color.RED
                        else -> Color.YELLOW
                    })

                    if (table.childCount > 1) {
                        table.removeViews(1, table.childCount - 1)
                    }

                    // Update copy button click listener with current logs
                    copyButton.setOnClickListener {
                        val logText = buildString {
                            logs.forEach { log ->
                                append("${formatTime(log.timestamp)}: ${log.log}\n")
                            }
                        }
                        copyToClipboard(logText)
                        // Show a toast to confirm copy
                        Toast.makeText(this@SystemStatusActivity,
                            "Logs copied to clipboard", 
                            Toast.LENGTH_SHORT).show()
                    }

//                    logs.forEach { log ->
//                        val row = TableRow(this@SystemStatusActivity).apply {
//                            layoutParams = TableLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
//                                weight = 1f
//                            }
//                            addView(createCell(formatTime(log.timestamp), false))
//                            // Remove the unnecessary measure calls
//                            addView(createCell(log.log, false))
//                        }
//                        table.addView(row)
//                    }

                      logs.forEach { log ->
                         val row = TableRow(this@SystemStatusActivity).apply {
                             layoutParams = TableLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                                 weight = 1f
                             }
                             addView(createCell(formatTime(log.timestamp), false))
                             addView(createCell(log.log, false).apply {
                                 measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
                                 Log.d("SystemStatus", "Log width: ${measuredWidth}, Text: ${log.log}")
                             })
                         }
                         table.addView(row)
                    }
                }
                unitContainer.addView(unitLayout)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun checkDeviceServices() {
        val tts = TextToSpeech(this) { status ->
            val ttsStatus = if (status == TextToSpeech.SUCCESS) "Running" else "Failed"
            findViewById<TextView>(R.id.ttsStatus).apply {
                text = "TTS: $ttsStatus"
                setTextColor(if (ttsStatus == "Running") Color.GREEN else Color.RED)
            }
            SystemStatus.logStatus("TTS", ttsStatus)
        }

        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        val vibrationStatus = if (vibrator.hasVibrator()) "Running" else "Failed"
        findViewById<TextView>(R.id.vibrationStatus).apply {
            text = "Vibration: $vibrationStatus"
            setTextColor(if (vibrationStatus == "Running") Color.GREEN else Color.RED)
        }
        SystemStatus.logStatus("Vibration", vibrationStatus)

        val wallpaperManager = WallpaperManager.getInstance(this)
        val wallpaperStatus = try {
            wallpaperManager.drawable
            "Running"
        } catch (e: Exception) {
            "Failed"
        }
        findViewById<TextView>(R.id.wallpaperStatus).apply {
            text = "Wallpaper: $wallpaperStatus"
            setTextColor(if (wallpaperStatus == "Running") Color.GREEN else Color.RED)
        }
        SystemStatus.logStatus("Wallpaper", wallpaperStatus)

        val alarmStatus = if (checkSelfPermission(Manifest.permission.SET_ALARM) == PackageManager.PERMISSION_GRANTED) "Running" else "Failed"
        findViewById<TextView>(R.id.alarmStatus).apply {
            text = "Alarm: $alarmStatus"
            setTextColor(if (alarmStatus == "Running") Color.GREEN else Color.RED)
        }
        SystemStatus.logStatus("Alarm", alarmStatus)

        val widgetStatus = "Not Implemented"
        findViewById<TextView>(R.id.widgetStatus).apply {
            text = "Widget: $widgetStatus"
            setTextColor(Color.YELLOW)
        }
        SystemStatus.logStatus("Widget", widgetStatus)
    }
}