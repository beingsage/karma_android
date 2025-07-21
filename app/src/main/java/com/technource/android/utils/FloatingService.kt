package com.technource.android.utils

import android.Manifest
import android.app.AlertDialog
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.view.GestureDetectorCompat
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.technource.android.R
import java.io.File
import java.io.IOException

class FloatingService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var params: WindowManager.LayoutParams
    private val handler = Handler(Looper.getMainLooper())
    private var isVisible = false
    private var isDragging = false
    private var initialX: Int = 0
    private var initialY: Int = 0
    private var initialTouchX: Float = 0f
    private var initialTouchY: Float = 0f
    private var dotSize: Int = 0
    private var panelSize: Int = 0
    private var mediaRecorder: MediaRecorder? = null
    private var mediaProjection: MediaProjection? = null
    private var cameraDevice: CameraDevice? = null
    private var NOTIFICATION_CHANNEL_ID = "Floating_Widget"

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate() {
        super.onCreate()

        // Set up foreground service notification
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Floating Widget")
            .setContentText("Floating service is running")
            .setSmallIcon(R.drawable.ic_notification) // Replace with your notification icon
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        startForeground(1, notification)

        setupFloatingView()
        setupTouchListener()
        setupRecyclerView()
        resetAutoHide()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Floating Service Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notification for Floating Service"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun setupFloatingView() {
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_panel, null)
        val density = resources.displayMetrics.density
        dotSize = (50 * density).toInt() // Tiny dot when minimized
        panelSize = (200 * density).toInt() // Compact panel when maximized

        params = WindowManager.LayoutParams(
            dotSize, dotSize,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 300
        params.alpha = 0.1f // Nearly invisible when minimized

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.addView(floatingView, params)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchListener() {
        val gestureDetector =
            GestureDetectorCompat(this, object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapUp(e: MotionEvent): Boolean {
                    if (!isVisible) {
                        showFloatingView()
                    }
                    return true
                }
            })

        floatingView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (isVisible && event.x < 50) { // Center drag area
                        isDragging = true
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                    }
                    return@setOnTouchListener true
                }

                MotionEvent.ACTION_MOVE -> {
                    if (isDragging) {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(floatingView, params)
                    }
                    return@setOnTouchListener true
                }

                MotionEvent.ACTION_UP -> {
                    isDragging = false
                    if (isVisible) resetAutoHide()
                }
            }
            false
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun setupRecyclerView() {
        val recyclerView = floatingView.findViewById<RecyclerView>(R.id.action_recycler)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = ActionAdapter(listOf(
            Action(R.drawable.ic_screenshot, ::takeScreenshot),
            Action(R.drawable.ic_cameraaa, ::captureCamera),
            Action(R.drawable.ic_notesss, ::showNotesDialog),
            Action(R.drawable.ic_toggles, ::showToggleDialog),
            Action(R.drawable.ic_text, ::showText),
            Action(R.drawable.ic_audio, ::recordAudio),
            Action(R.drawable.ic_questions, ::askQuestions),
            Action(R.drawable.ic_search, ::searchText),
            Action(R.drawable.ic_share, ::shareContent),
            Action(R.drawable.ic_settingsss, ::quickSettings)
        ))
    }

    private fun takeScreenshot() {
        val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        // Note: Requires user approval via startActivityForResult in a real app
        Toast.makeText(this, "Screenshot captured and analyzed", Toast.LENGTH_SHORT).show()
        // Implement OCR or image analysis here
        resetAutoHide()
    }

    private fun captureCamera() {
        val cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        try {
            val cameraId = cameraManager.cameraIdList[0]
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    // Simplified: Capture image
                    Toast.makeText(this@FloatingService, "Camera image captured", Toast.LENGTH_SHORT).show()
                    camera.close()
                    resetAutoHide()
                }
                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                }
                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                }
            }, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showNotesDialog() {
        val input = EditText(this).apply { setText(getSavedNotes()) }
        AlertDialog.Builder(this)
            .setTitle("Notes")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                saveNotes(input.text.toString())
                Toast.makeText(this, "Notes saved", Toast.LENGTH_SHORT).show()
                resetAutoHide()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showToggleDialog() {
        val toggles = arrayOf("Dark Mode", "Notifications", "Vibrate")
        val checked = booleanArrayOf(false, true, false)
        AlertDialog.Builder(this)
            .setTitle("Settings")
            .setMultiChoiceItems(toggles, checked) { _, which, isChecked -> checked[which] = isChecked }
            .setPositiveButton("Done") { _, _ -> resetAutoHide() }
            .show()
    }

    private fun showText() {
        AlertDialog.Builder(this)
            .setTitle("Info")
            .setMessage("Service is active at ${System.currentTimeMillis()}")
            .setPositiveButton("OK") { _, _ -> resetAutoHide() }
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun recordAudio() {
        val file = File(getExternalFilesDir(Environment.DIRECTORY_RECORDINGS), "audio_${System.currentTimeMillis()}.3gp")
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(file.absolutePath)
            try {
                prepare()
                start()
                Toast.makeText(this@FloatingService, "Recording...", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        handler.postDelayed({
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            Toast.makeText(this, "Audio saved at ${file.absolutePath}", Toast.LENGTH_SHORT).show()
            resetAutoHide()
        }, 3000)
    }

    private fun askQuestions() {
        val input = EditText(this)
        AlertDialog.Builder(this)
            .setTitle("Quick Question")
            .setMessage("What's on your mind?")
            .setView(input)
            .setPositiveButton("Submit") { _, _ ->
                saveResponse(input.text.toString())
                Toast.makeText(this, "Response saved", Toast.LENGTH_SHORT).show()
                resetAutoHide()
            }
            .show()
    }

    private fun searchText() {
        val input = EditText(this)
        AlertDialog.Builder(this)
            .setTitle("Search")
            .setView(input)
            .setPositiveButton("Go") { _, _ ->
                // Implement search (e.g., open browser or app search)
                Toast.makeText(this, "Searching: ${input.text}", Toast.LENGTH_SHORT).show()
                resetAutoHide()
            }
            .show()
    }

    private fun shareContent() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Shared from Floating Widget")
        }
        startActivity(Intent.createChooser(intent, "Share"))
        resetAutoHide()
    }

    private fun quickSettings() {
        startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
        resetAutoHide()
    }

    private fun getSavedNotes(): String = getSharedPreferences("floating_service", MODE_PRIVATE)
        .getString("notes", "") ?: ""

    private fun saveNotes(notes: String) = getSharedPreferences("floating_service", MODE_PRIVATE)
        .edit().putString("notes", notes).apply()

    private fun saveResponse(response: String) = getSharedPreferences("floating_service", MODE_PRIVATE)
        .edit().putString("response_${System.currentTimeMillis()}", response).apply()

    private fun resetAutoHide() {
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ hideFloatingView() }, 3000)
    }

    private fun hideFloatingView() {
        params.width = dotSize
        params.height = dotSize
        params.alpha = 0.1f
        ValueAnimator.ofInt(params.x, 0).apply {
            duration = 400
            addUpdateListener { animation ->
                params.x = animation.animatedValue as Int
                windowManager.updateViewLayout(floatingView, params)
            }
            start()
        }
        floatingView.findViewById<RecyclerView>(R.id.action_recycler).visibility = View.GONE
        floatingView.animate().scaleX(0.5f).scaleY(0.5f).setDuration(400).start()
        isVisible = false
    }

    private fun showFloatingView() {
        params.width = panelSize
        params.height = panelSize
        params.alpha = 0.7f // Semi-transparent when maximized
        ValueAnimator.ofInt(params.x, 0).apply {
            duration = 400
            addUpdateListener { animation ->
                params.x = animation.animatedValue as Int
                windowManager.updateViewLayout(floatingView, params)
            }
            start()
        }
        floatingView.findViewById<RecyclerView>(R.id.action_recycler).visibility = View.VISIBLE
        floatingView.animate().scaleX(1f).scaleY(1f).setDuration(400).start()
        animateBackground()
        isVisible = true
        resetAutoHide()
    }

    private fun animateBackground() {
        val view = floatingView.findViewById<View>(R.id.panel_background)
        ValueAnimator.ofArgb(
            Color.parseColor("#80000000"),
            Color.parseColor("#80FF6F00"),
            Color.parseColor("#80000000")
        ).apply {
            duration = 2000
            addUpdateListener { animator ->
                view.setBackgroundColor(animator.animatedValue as Int)
            }
            start()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        if (::floatingView.isInitialized) windowManager.removeView(floatingView)
        mediaRecorder?.release()
        cameraDevice?.close()
        mediaProjection?.stop()
        super.onDestroy()
    }
}

data class Action(val icon: Int, val callback: () -> Unit)

class ActionAdapter(private val actions: List<Action>) : RecyclerView.Adapter<ActionAdapter.ViewHolder>() {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val button: ImageButton = view.findViewById(R.id.action_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.action_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val action = actions[position]
        holder.button.setImageResource(action.icon)
        holder.button.setOnClickListener { action.callback() }
    }

    override fun getItemCount(): Int = actions.size
}