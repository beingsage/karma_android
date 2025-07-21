//package com.technource.android.module.miscModule.miscscreen.Body.ui.screens
//
//import android.content.Context
//import android.graphics.Bitmap
//import androidx.camera.core.*
//import androidx.camera.lifecycle.ProcessCameraProvider
//import androidx.camera.view.PreviewView
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.platform.LocalLifecycleOwner
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.viewinterop.AndroidView
//import androidx.core.content.ContextCompat
//import androidx.navigation.NavController
//import com.google.mlkit.vision.barcode.BarcodeScanning
//import com.google.mlkit.vision.barcode.common.Barcode
//import com.google.mlkit.vision.common.InputImage
//import com.technource.android.module.miscModule.miscscreen.Body.ui.viewmodels.MainViewModel
//import java.util.concurrent.ExecutorService
//import java.util.concurrent.Executors
//
//@Composable
//fun CameraScreen(navController: NavController, viewModel: MainViewModel) {
//    val context = LocalContext.current
//    val lifecycleOwner = LocalLifecycleOwner.current
//    var cameraMode by remember { mutableStateOf(CameraMode.PHOTO) }
//    var isFlashOn by remember { mutableStateOf(false) }
//
//    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
//
//    Box(modifier = Modifier.fillMaxSize()) {
//        // Camera Preview
//        AndroidView(
//            factory = { ctx ->
//                val previewView = PreviewView(ctx)
//                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
//
//                cameraProviderFuture.addListener({
//                    val cameraProvider = cameraProviderFuture.get()
//                    bindCameraUseCases(
//                        cameraProvider,
//                        previewView,
//                        lifecycleOwner,
//                        cameraMode,
//                        isFlashOn,
//                        viewModel
//                    )
//                }, ContextCompat.getMainExecutor(ctx))
//
//                previewView
//            },
//            modifier = Modifier.fillMaxSize()
//        )
//
//        // Top Controls
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(16.dp),
//            horizontalArrangement = Arrangement.SpaceBetween,
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            IconButton(
//                onClick = { navController.popBackStack() },
//                modifier = Modifier
//                    .clip(CircleShape)
//                    .size(48.dp)
//            ) {
//                Icon(
//                    Icons.Default.ArrowBack,
//                    contentDescription = "Back",
//                    tint = MaterialTheme.colorScheme.onPrimary
//                )
//            }
//
//            Row {
//                IconButton(
//                    onClick = { isFlashOn = !isFlashOn },
//                    modifier = Modifier
//                        .clip(CircleShape)
//                        .size(48.dp)
//                ) {
//                    Icon(
//                        if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
//                        contentDescription = "Flash",
//                        tint = MaterialTheme.colorScheme.onPrimary
//                    )
//                }
//
//                IconButton(
//                    onClick = { /* Switch camera */ },
//                    modifier = Modifier
//                        .clip(CircleShape)
//                        .size(48.dp)
//                ) {
//                    Icon(
//                        Icons.Default.Cameraswitch,
//                        contentDescription = "Switch Camera",
//                        tint = MaterialTheme.colorScheme.onPrimary
//                    )
//                }
//            }
//        }
//
//        // Bottom Controls
//        Column(
//            modifier = Modifier
//                .align(Alignment.BottomCenter)
//                .padding(16.dp),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            // Camera Mode Selector
//            Row(
//                horizontalArrangement = Arrangement.spacedBy(16.dp),
//                modifier = Modifier.padding(bottom = 24.dp)
//            ) {
//                CameraModeButton(
//                    text = "Photo",
//                    isSelected = cameraMode == CameraMode.PHOTO,
//                    onClick = { cameraMode = CameraMode.PHOTO }
//                )
//                CameraModeButton(
//                    text = "Barcode",
//                    isSelected = cameraMode == CameraMode.BARCODE,
//                    onClick = { cameraMode = CameraMode.BARCODE }
//                )
//                CameraModeButton(
//                    text = "Progress",
//                    isSelected = cameraMode == CameraMode.PROGRESS,
//                    onClick = { cameraMode = CameraMode.PROGRESS }
//                )
//            }
//
//            // Capture Button
//            FloatingActionButton(
//                onClick = {
//                    when (cameraMode) {
//                        CameraMode.PHOTO -> viewModel.capturePhoto()
//                        CameraMode.BARCODE -> viewModel.scanBarcode()
//                        CameraMode.PROGRESS -> viewModel.captureProgressPhoto()
//                    }
//                },
//                modifier = Modifier.size(72.dp)
//            ) {
//                Icon(
//                    when (cameraMode) {
//                        CameraMode.BARCODE -> Icons.Default.QrCodeScanner
//                        else -> Icons.Default.CameraAlt
//                    },
//                    contentDescription = "Capture",
//                    modifier = Modifier.size(32.dp)
//                )
//            }
//        }
//    }
//}
//
//@Composable
//fun CameraModeButton(
//    text: String,
//    isSelected: Boolean,
//    onClick: () -> Unit
//) {
//    Button(
//        onClick = onClick,
//        colors = ButtonDefaults.buttonColors(
//            containerColor = if (isSelected)
//                MaterialTheme.colorScheme.primary
//            else
//                MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
//        )
//    ) {
//        Text(text)
//    }
//}
//
//enum class CameraMode {
//    PHOTO, BARCODE, PROGRESS
//}
//
//private fun bindCameraUseCases(
//    cameraProvider: ProcessCameraProvider,
//    previewView: PreviewView,
//    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
//    cameraMode: CameraMode,
//    isFlashOn: Boolean,
//    viewModel: MainViewModel
//) {
//    val preview = Preview.Builder().build()
//    preview.setSurfaceProvider(previewView.surfaceProvider)
//
//    val imageCapture = ImageCapture.Builder().build()
//
//    val imageAnalyzer = ImageAnalysis.Builder()
//        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//        .build()
//
//    if (cameraMode == CameraMode.BARCODE) {
//        imageAnalyzer.setAnalyzer(
//            ContextCompat.getMainExecutor(previewView.context)
//        ) { imageProxy ->
//            scanBarcodes(imageProxy, viewModel)
//        }
//    }
//
//    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
//
//    try {
//        cameraProvider.unbindAll()
//        val camera = cameraProvider.bindToLifecycle(
//            lifecycleOwner,
//            cameraSelector,
//            preview,
//            imageCapture,
//            imageAnalyzer
//        )
//
//        // Set flash mode
//        camera.cameraControl.enableTorch(isFlashOn)
//
//    } catch (exc: Exception) {
//        // Handle exception
//    }
//}
//
//private fun scanBarcodes(imageProxy: ImageProxy, viewModel: MainViewModel) {
//    val mediaImage = imageProxy.image
//    if (mediaImage != null) {
//        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
//        val scanner = BarcodeScanning.getClient()
//
//        scanner.process(image)
//            .addOnSuccessListener { barcodes ->
//                for (barcode in barcodes) {
//                    when (barcode.valueType) {
//                        Barcode.TYPE_PRODUCT -> {
//                            barcode.displayValue?.let { code ->
//                                viewModel.processBarcodeResult(code)
//                            }
//                        }
//                    }
//                }
//            }
//            .addOnCompleteListener {
//                imageProxy.close()
//            }
//    }
//}
