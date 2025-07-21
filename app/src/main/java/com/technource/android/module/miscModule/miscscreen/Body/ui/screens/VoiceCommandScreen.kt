//package com.technource.android.module.miscModule.miscscreen.Body.ui.screens
//
//import android.Manifest
//import android.content.Intent
//import android.speech.RecognizerIntent
//import android.speech.SpeechRecognizer
//import androidx.activity.compose.rememberLauncherForActivityResult
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.compose.animation.core.*
//import androidx.compose.foundation.Canvas
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.drawscope.DrawScope
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.dp
//import androidx.navigation.NavController
//import com.technource.android.module.miscModule.miscscreen.Body.ui.viewmodels.MainViewModel
//import com.technource.android.module.miscModule.miscscreen.Body.utils.VoiceCommandProcessor
//
//@Composable
//fun VoiceCommandScreen(navController: NavController, viewModel: MainViewModel) {
//    val context = LocalContext.current
//    var isListening by remember { mutableStateOf(false) }
//    var recognizedText by remember { mutableStateOf("") }
//    var processingResult by remember { mutableStateOf("") }
//    var hasPermission by remember { mutableStateOf(false) }
//
//
//    val speechRecognizerLauncher = rememberLauncherForActivityResult(
//        ActivityResultContracts.StartActivityForResult()
//    ) { result ->
//        val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
//        spokenText?.firstOrNull()?.let { text ->
//            recognizedText = text
//            processVoiceCommand(text, viewModel) { result ->
//                processingResult = result
//            }
//        }
//        isListening = false
//    }
//
//    val permissionLauncher = rememberLauncherForActivityResult(
//        ActivityResultContracts.RequestPermission()
//    ) { granted ->
//        hasPermission = granted
//        if (granted) {
////            startVoiceRecognition()
//        }
//    }
//
//    fun startVoiceRecognition() {
//        if (!hasPermission) {
//            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
//            return
//        }
//        isListening = true
//        recognizedText = ""
//        processingResult = ""
//        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
//            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
//            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say your health command...")
//            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
//        }
//        speechRecognizerLauncher.launch(intent)
//    }
//
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(24.dp),
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.Center
//    ) {
//        Text(
//            text = "Voice Commands",
//            style = MaterialTheme.typography.headlineMedium,
//            fontWeight = FontWeight.Bold,
//            modifier = Modifier.padding(bottom = 32.dp)
//        )
//
//        // Voice visualization
//        VoiceVisualization(isListening = isListening)
//
//        Spacer(modifier = Modifier.height(32.dp))
//
//        // Voice command button
//        FloatingActionButton(
//            onClick = { startVoiceRecognition() },
//            modifier = Modifier.size(80.dp),
//            containerColor = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
//        ) {
//            Icon(
//                imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
//                contentDescription = if (isListening) "Stop listening" else "Start listening",
//                modifier = Modifier.size(32.dp)
//            )
//        }
//
//        Spacer(modifier = Modifier.height(24.dp))
//
//        // Status text
//        Text(
//            text = when {
//                isListening -> "Listening..."
//                recognizedText.isNotEmpty() -> "You said: \"$recognizedText\""
//                else -> "Tap the microphone to start"
//            },
//            style = MaterialTheme.typography.bodyLarge,
//            textAlign = TextAlign.Center,
//            modifier = Modifier.padding(horizontal = 16.dp)
//        )
//
//        if (processingResult.isNotEmpty()) {
//            Spacer(modifier = Modifier.height(16.dp))
//            Card(
//                modifier = Modifier.fillMaxWidth(),
//                colors = CardDefaults.cardColors(
//                    containerColor = MaterialTheme.colorScheme.primaryContainer
//                )
//            ) {
//                Text(
//                    text = processingResult,
//                    style = MaterialTheme.typography.bodyMedium,
//                    modifier = Modifier.padding(16.dp),
//                    textAlign = TextAlign.Center
//                )
//            }
//        }
//
//        Spacer(modifier = Modifier.height(32.dp))
//
//        // Example commands
//        Card(
//            modifier = Modifier.fillMaxWidth()
//        ) {
//            Column(
//                modifier = Modifier.padding(16.dp)
//            ) {
//                Text(
//                    text = "Try saying:",
//                    style = MaterialTheme.typography.titleMedium,
//                    fontWeight = FontWeight.Medium,
//                    modifier = Modifier.padding(bottom = 8.dp)
//                )
//
//                val examples = listOf(
//                    "Log 500ml of water",
//                    "I weigh 70 kilograms",
//                    "Start a workout",
//                    "I ate a banana",
//                    "Take a progress photo"
//                )
//
//                examples.forEach { example ->
//                    Text(
//                        text = "• $example",
//                        style = MaterialTheme.typography.bodyMedium,
//                        color = MaterialTheme.colorScheme.onSurfaceVariant,
//                        modifier = Modifier.padding(vertical = 2.dp)
//                    )
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun VoiceVisualization(isListening: Boolean) {
//    val infiniteTransition = rememberInfiniteTransition(label = "voice_animation")
//
//    val animatedRadius by infiniteTransition.animateFloat(
//        initialValue = 40f,
//        targetValue = 60f,
//        animationSpec = infiniteRepeatable(
//            animation = tween(1000, easing = EaseInOut),
//            repeatMode = RepeatMode.Reverse
//        ),
//        label = "radius_animation"
//    )
//
//    val animatedAlpha by infiniteTransition.animateFloat(
//        initialValue = 0.3f,
//        targetValue = 0.8f,
//        animationSpec = infiniteRepeatable(
//            animation = tween(800, easing = EaseInOut),
//            repeatMode = RepeatMode.Reverse
//        ),
//        label = "alpha_animation"
//    )
//
//    Box(
//        modifier = Modifier.size(120.dp),
//        contentAlignment = Alignment.Center
//    ) {
//        if (isListening) {
//            Canvas(modifier = Modifier.fillMaxSize()) {
//                drawVoiceWaves(animatedRadius, animatedAlpha)
//            }
//        }
//
//        Box(
//            modifier = Modifier
//                .size(80.dp)
//                .clip(CircleShape),
//            contentAlignment = Alignment.Center
//        ) {
//            Icon(
//                imageVector = Icons.Default.GraphicEq,
//                contentDescription = "Voice visualization",
//                modifier = Modifier.size(40.dp),
//                tint = MaterialTheme.colorScheme.primary
//            )
//        }
//    }
//}
//
//
//private fun DrawScope.drawVoiceWaves(radius: Float, alpha: Float) {
//    val center = this.center
//    val primaryColor = Color(0xFF6366F1)
//
//    // Draw multiple concentric circles for wave effect
//    for (i in 1..3) {
//        drawCircle(
//            color = primaryColor.copy(alpha = alpha / i),
//            radius = radius * i,
//            center = center
//        )
//    }
//}
//
//private fun processVoiceCommand(
//    command: String,
//    viewModel: MainViewModel,
//    onResult: (String) -> Unit
//) {
//    val processor = VoiceCommandProcessor()
//    val result = processor.processCommand(command)
//
//    when (result.action) {
//        "LOG_WATER" -> {
//            val amount = result.parameters["amount"] as? Int ?: 250
//            viewModel.logWater(amount)
//            onResult("Logged ${amount}ml of water!")
//        }
//        "LOG_WEIGHT" -> {
//            val weight = result.parameters["weight"] as? Float ?: 0f
//            if (weight > 0) {
//                viewModel.logWeight(weight)
//                onResult("Logged weight: ${weight}kg")
//            } else {
//                onResult("Please specify your weight")
//            }
//        }
//        "START_WORKOUT" -> {
//            viewModel.startWorkout()
//            onResult("Workout started!")
//        }
//        "LOG_FOOD" -> {
//            val food = result.parameters["food"] as? String ?: "food item"
//            viewModel.quickMealLog()
//            onResult("Logged: $food")
//        }
//        "TAKE_PHOTO" -> {
//            viewModel.takeProgressPhoto()
//            onResult("Taking progress photo...")
//        }
//        else -> {
//            onResult("Sorry, I didn't understand that command. Try again!")
//        }
//    }
//}
