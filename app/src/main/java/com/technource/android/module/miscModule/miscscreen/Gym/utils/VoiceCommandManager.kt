package com.technource.android.module.miscModule.miscscreen.Gym.utils

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.*

class VoiceCommandManager(
    private val context: Context,
    private val onCommandReceived: (String) -> Unit
) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    init {
        initializeSpeechRecognizer()
    }

    private fun initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    isListening = false
                }

                override fun onError(error: Int) {
                    isListening = false
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    matches?.firstOrNull()?.let { command ->
                        processCommand(command.lowercase(Locale.getDefault()))
                    }
                    isListening = false
                }

                override fun onPartialResults(partialResults: Bundle?) {}

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    fun startListening() {
        if (!isListening && speechRecognizer != null) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }

            speechRecognizer?.startListening(intent)
            isListening = true
        }
    }

    fun stopListening() {
        if (isListening) {
            speechRecognizer?.stopListening()
            isListening = false
        }
    }

    private fun processCommand(command: String) {
        when {
            command.contains("start workout") || command.contains("begin workout") -> {
                onCommandReceived("start")
            }
            command.contains("pause workout") || command.contains("pause timer") -> {
                onCommandReceived("pause")
            }
            command.contains("resume workout") || command.contains("continue workout") -> {
                onCommandReceived("resume")
            }
            command.contains("complete set") || command.contains("finish set") -> {
                onCommandReceived("complete set")
            }
            command.contains("next exercise") || command.contains("move to next") -> {
                onCommandReceived("next exercise")
            }
            command.contains("skip rest") || command.contains("skip break") -> {
                onCommandReceived("skip rest")
            }
        }
    }

    fun isListening(): Boolean = isListening

    fun release() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}
