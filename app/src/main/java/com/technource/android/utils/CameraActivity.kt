package com.technource.android.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.cloudinary.android.MediaManager
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.technource.android.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CameraActivity : AppCompatActivity() {
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var imageCapture: ImageCapture
    private lateinit var outputDirectory: File
    private lateinit var resultTextView: TextView

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        // Initialize views
        resultTextView = findViewById(R.id.resultTextView)
        findViewById<Button>(R.id.captureButton).setOnClickListener { takePhoto() }

        // Initialize camera executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Initialize output directory
        outputDirectory = getOutputDirectory()

        // Check camera permission
        checkCameraPermission()

        // Initialize Cloudinary
        initCloudinary()
    }

    private fun initCloudinary() {
        val config = mapOf(
            "cloud_name" to "dipwueexh",
            "api_key" to "695189316414971",
            "api_secret" to "b0aUYEyI03R5mZR3NCT8eG9xX6s"
        )
        try {
            MediaManager.init(this, config)
        } catch (e: Exception) {
            Log.e(TAG, "Cloudinary initialization failed: ${e.message}", e)
            Toast.makeText(this, "Failed to initialize Cloudinary", Toast.LENGTH_SHORT).show()
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED ->
                startCamera()
            else ->
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(findViewById<PreviewView>(R.id.viewFinder).surfaceProvider)
            }
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Log.e(TAG, "Camera binding failed", exc)
                Toast.makeText(this, "Failed to start camera", Toast.LENGTH_SHORT).show()
                setResult(RESULT_CANCELED)
                finish()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        if (isFinishing) return
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(Date()) + ".jpg"
        )
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    processPhoto(savedUri)
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    Toast.makeText(this@CameraActivity, "Failed to capture photo", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_CANCELED)
                    finish()
                }
            }
        )
    }

    private fun processPhoto(imageUri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            var extractedText = ""

            // Perform OCR
            try {
                val image = InputImage.fromFilePath(this@CameraActivity, imageUri)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                extractedText = withContext(Dispatchers.Default) {
                    com.google.android.gms.tasks.Tasks.await(recognizer.process(image)).text
                }
                withContext(Dispatchers.Main) {
                    resultTextView.text = extractedText.ifEmpty { "No text detected" }
                    showLoading()
                }
            } catch (e: Exception) {
                Log.e(TAG, "OCR failed: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CameraActivity, "OCR failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_CANCELED)
                    finish()
                }
                return@launch
            }

            // Enqueue upload with WorkManager
            try {
                val file = getFileFromUri(imageUri)
                val fileName = imageUri.lastPathSegment ?: "image_${System.currentTimeMillis()}.jpg"
                val uploadWork = OneTimeWorkRequestBuilder<CloudinaryUploadWorker>()
                    .setInputData(
                        workDataOf(
                            "file_path" to file.absolutePath,
                            "file_name" to fileName
                        )
                    )
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build()
                    )
                    .build()
                WorkManager.getInstance(this@CameraActivity).enqueue(uploadWork)

                // Monitor work status
                lifecycleScope.launch {
                    WorkManager.getInstance(this@CameraActivity)
                        .getWorkInfoByIdLiveData(uploadWork.id)
                        .observe(this@CameraActivity) { workInfo ->
                            when (workInfo?.state) {
                                WorkInfo.State.SUCCEEDED -> {
                                    val shareableLink = workInfo.outputData.getString("secure_url") ?: ""
                                    lifecycleScope.launch(Dispatchers.Main) {
                                        hideLoading()
                                        Toast.makeText(this@CameraActivity, "Deep work image processed successfully: URL stored.", Toast.LENGTH_SHORT).show()
                                        val resultIntent = Intent().apply {
                                            putExtra(EXTRA_EXTRACTED_TEXT, extractedText)
                                            putExtra(EXTRA_SHAREABLE_LINK, shareableLink)
                                        }
                                        setResult(RESULT_OK, resultIntent)
                                        finish()
                                    }
                                }
                                WorkInfo.State.FAILED -> {
                                    lifecycleScope.launch(Dispatchers.Main) {
                                        hideLoading()
                                        Toast.makeText(this@CameraActivity, "Failed to process deep work image: Upload error.", Toast.LENGTH_SHORT).show()
                                        setResult(RESULT_CANCELED)
                                        finish()
                                    }
                                }
                                else -> {
                                    lifecycleScope.launch(Dispatchers.Main) {
                                        showLoading()
                                    }
                                }
                            }
                        }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Upload failed: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    hideLoading()
                    Toast.makeText(this@CameraActivity, "Failed to process deep work image: ${e.message}.", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_CANCELED)
                    finish()
                }
            }
        }
    }

    private suspend fun getFileFromUri(uri: Uri): File {
        return withContext(Dispatchers.IO) {
            val tempFile = File(cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
            contentResolver.openInputStream(uri)?.use { input ->
                val bitmap = BitmapFactory.decodeStream(input)
                FileOutputStream(tempFile).use { output ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, output)
                }
            } ?: throw IllegalStateException("Failed to read image file")
            tempFile
        }
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return mediaDir ?: filesDir
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun showLoading() {
        findViewById<ProgressBar>(R.id.loadingProgress).visibility = View.VISIBLE
    }

    private fun hideLoading() {
        findViewById<ProgressBar>(R.id.loadingProgress).visibility = View.GONE
    }

    companion object {
        private const val TAG = "CameraActivity"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        const val EXTRA_SHAREABLE_LINK = "extra_shareable_link"
        const val EXTRA_EXTRACTED_TEXT = "extra_extracted_text"
    }
}

class CloudinaryUploadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val filePath = inputData.getString("file_path") ?: return@withContext Result.failure()
            val fileName = inputData.getString("file_name") ?: "image_${System.currentTimeMillis()}.jpg"

            // Create suspending function for Cloudinary upload
            val uploadResult = suspendCancellableCoroutine<Map<*, *>> { continuation ->
                MediaManager.get().upload(File(filePath).absolutePath)
                    .option("public_id", fileName.removeSuffix(".jpg"))
                    .option("quality", "auto:low")
                    .callback(object : UploadCallback {
                        override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                            continuation.resume(resultData)
                        }

                        override fun onError(requestId: String, error: ErrorInfo) {
                            continuation.resumeWithException(Exception(error.description))
                        }

                        override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                        override fun onStart(requestId: String) {}
                        override fun onReschedule(requestId: String, error: ErrorInfo) {}
                    })
                    .dispatch()
            }

            val secureUrl = uploadResult["secure_url"]?.toString() ?: return@withContext Result.failure()
            
            return@withContext Result.success(workDataOf("secure_url" to secureUrl))
        } catch  (e: Exception) {
            Log.e("CloudinaryUploadWorker", "Upload failed: ${e.message}", e)
            return@withContext Result.failure()
        }
    }
}