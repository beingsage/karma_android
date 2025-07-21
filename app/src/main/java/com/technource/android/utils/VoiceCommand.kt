package com.technource.android.utils

import ai.picovoice.porcupine.PorcupineManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.technource.android.R
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.Locale

class VoiceAssistantManager(
    private val context: Context,
    private val wakeWord: String = "karma"
) {

    private var porcupineManager: PorcupineManager? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var tempKeywordFile: File? = null
    private var lastTriggerTime: Long = 0L
    private val TAG = "VoiceAssistantManager"
    private val TRIGGER_DELAY_MS = 2000L

    private val commandMap: Map<String, () -> Unit> = mapOf(
        "complete task" to { onCommandDetected("Task marked as complete.") },
        "skip task" to { onCommandDetected("Task skipped.") },
        "what's next" to { onCommandDetected("Next task is study.") }
    )

    fun startWakeWordDetection() {
        try {
            if (tempKeywordFile == null) {
                val inputStream = context.resources.openRawResource(R.raw.karma)
                tempKeywordFile = createTempFileFromInputStream(inputStream, "karma.ppn")
                inputStream.close()
                Log.d(TAG, "Created temporary file for karma.ppn at ${tempKeywordFile?.absolutePath}")
            }

            val keywordPaths = listOf(tempKeywordFile?.absolutePath ?: "")
            val sensitivities = floatArrayOf(0.9f)

            porcupineManager = PorcupineManager.Builder()
//                .setAccessKey("eoAAzd/BUiOOP1C41KoLLzaxBVlCWoXV4UkOzjKmQI7m2Ugt44m4jg==")
                .setAccessKey("cDMG+UrMfHTzYOTufM9GSFYebOhf/9Qgw7Dkqrzcgao28L5kbHXO/Q==")
                .setKeywordPaths(keywordPaths.toTypedArray())
                .setSensitivities(sensitivities)
                .setModelPath(null)
                .build(context) {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastTriggerTime > TRIGGER_DELAY_MS) {
                        lastTriggerTime = currentTime
                        Log.d(TAG, "Wake word detected!")
                        triggerVoiceCommand()
                    } else {
                        Log.d(TAG, "Wake word ignored due to recent trigger")
                    }
                }

            porcupineManager?.start()
            Toast.makeText(context, "Voice Assistant is listening for '$wakeWord'...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Porcupine setup failed: ${e.message}", e)
            Toast.makeText(context, "Porcupine setup failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    @Throws(IOException::class)
    private fun createTempFileFromInputStream(inputStream: InputStream, fileName: String): File {
        val tempFile = File.createTempFile("temp_", "_$fileName", context.cacheDir)
        inputStream.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        return tempFile
    }

    fun stopWakeWordDetection() {
        try {
            porcupineManager?.stop()
            porcupineManager?.delete()
            porcupineManager = null
            speechRecognizer?.destroy()
            speechRecognizer = null
            tempKeywordFile?.delete()
            tempKeywordFile = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping Porcupine: ${e.message}")
        }
    }

    fun triggerVoiceCommand() {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                context as android.app.Activity,
                arrayOf(android.Manifest.permission.RECORD_AUDIO),
                1001
            )
            return
        }

        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            if (speechRecognizer == null) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onResults(results: Bundle?) {
                        val spokenText = results
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.joinToString("\n") ?: "No results"
                        Log.d(TAG, "Recognized text: $spokenText")
                        val firstMatch = results
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                            ?.lowercase(Locale.getDefault()) ?: ""
                        Log.d(TAG, "First match: $firstMatch")

                        val matched = commandMap.entries.firstOrNull { firstMatch.contains(it.key) }
                        if (matched != null) {
                            matched.value.invoke()
                        } else {
                            onCommandDetected("Command not recognized. Try 'complete task' or 'skip task'.")
                        }
                    }

                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onError(error: Int) {
                        Log.e(TAG, "SpeechRecognizer Error: $error")
                        onCommandDetected("Failed to process command.")
                    }

                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000)
            }
            speechRecognizer?.startListening(intent)
            Toast.makeText(context, "Listening for your command...", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Speech recognition not available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onCommandDetected(response: String) {
        Toast.makeText(context, response, Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Command action: $response")
    }
}


//suspend fun handleVoiceCommands(task: Task) {
//    SystemStatus.logEvent("TaskIterator", "Voice command window started for: ${task.title}")
//    val startTime = System.currentTimeMillis()
//    var lastCommand = ""
//
//    while (System.currentTimeMillis() - startTime < 60000 && scope.isActive) {
//        val command = "" // Placeholder for SpeechRecognizer
//        if (command.isNotEmpty()) {
//            lastCommand = processVoiceCommand(task, command)
//            withContext(Dispatchers.Main) {
//                ttsManager.speakHinglish("Logged: $lastCommand")
//            }
//        }
//        delay(100)
//    }
//    saveLastCommand(task, lastCommand)
//    SystemStatus.logEvent("TaskIterator", "Voice command window ended for: ${task.title}")
//}
//
//
//
//fun processVoiceCommand(task: Task, command: String): String {
//    return when {
//        command.contains("log 94%") -> "94% completion".also {
//            task.completionStatus = 0.94f
//        }
//        command.contains("log yes") -> "task completed".also {
//            task.completionStatus = 1f
//        }
//        command.contains("log all") -> "all subtasks".also {
//            task.subtasks?.forEach { it.completionStatus = 1.0f }
//        }
//        command.contains("log") -> parseSubTaskCommand(command, task)
//        else -> ""
//    }
//}
//
//
//fun parseSubTaskCommand(command: String, task: Task): String {
//    val numbers = command.split(" ").mapNotNull { it.toIntOrNull() }
//    if (numbers.isNotEmpty() && task.subtasks != null) {
//        numbers.forEach { index ->
//            task.subtasks.getOrNull(index - 1)?.completionStatus = 1.0f
//        }
//        return "logged subtasks ${numbers.joinToString()}"
//    }
//    return ""
//}
//
//
//fun saveLastCommand(task: Task, command: String) {
//    SystemStatus.logEvent("TaskIterator", "Saved last command: $command for ${task.title}")
//}



//DDry5r+VNFy0LL3l+ZXw9SSku/jhN5ak6n0SgCwkAYNuJdydFFTiTQ==

//srisujal ui
//eoAAzd/BUiOOP1C41KoLLzaxBVlCWoXV4UkOzjKmQI7m2Ugt44m4jg==




//srisujalui srisujalsri
//cDMG+UrMfHTzYOTufM9GSFYebOhf/9Qgw7Dkqrzcgao28L5kbHXO/Q==