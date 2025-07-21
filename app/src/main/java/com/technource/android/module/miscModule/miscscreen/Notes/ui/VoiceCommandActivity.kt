package com.technource.android.module.miscModule.miscscreen.Notes.ui

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.appcompat.app.AppCompatActivity
import com.mindkeep.service.VoiceCommandProcessor
import com.technource.android.databinding.ActivityVoiceCommandBinding
import java.util.*

class VoiceCommandActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityVoiceCommandBinding
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var voiceCommandProcessor: VoiceCommandProcessor
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVoiceCommandBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        voiceCommandProcessor = VoiceCommandProcessor(this)
        setupSpeechRecognizer()
        startListening()
        
        binding.cancelVoiceButton.setOnClickListener {
            finish()
        }
    }
    
    private fun setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                binding.voiceCommandStatus.text = "Listening..."
            }
            
            override fun onBeginningOfSpeech() {
                binding.voiceCommandStatus.text = "Speaking..."
            }
            
            override fun onRmsChanged(rmsdB: Float) {}
            
            override fun onBufferReceived(buffer: ByteArray?) {}
            
            override fun onEndOfSpeech() {
                binding.voiceCommandStatus.text = "Processing..."
            }
            
            override fun onError(error: Int) {
                binding.voiceCommandStatus.text = "Error occurred. Try again."
            }
            
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.let { 
                    if (it.isNotEmpty()) {
                        processVoiceCommand(it[0])
                    }
                }
            }
            
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.let {
                    if (it.isNotEmpty()) {
                        binding.voiceCommandText.text = it[0]
                    }
                }
            }
            
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }
    
    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say your command...")
        }
        speechRecognizer.startListening(intent)
    }
    
    private fun processVoiceCommand(command: String) {
        binding.voiceCommandText.text = command
        binding.voiceCommandStatus.text = "Processing command..."
        
        voiceCommandProcessor.processCommand(command) { success ->
            runOnUiThread {
                if (success) {
                    binding.voiceCommandStatus.text = "Command executed successfully!"
                    finish()
                } else {
                    binding.voiceCommandStatus.text = "Could not understand command. Try again."
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
    }
}
