package com.technource.android.ETMS.micro

import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.technource.android.system_status.SystemStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale


class TTSManager(private val context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isSpeaking = false
    private var useGoogleTTS = false
    private val ttsScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        initializeTTS()
    }

    private fun initializeTTS() {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val isGoogleTTSInstalled = tts?.engines?.any { it.name == GOOGLE_TTS_PACKAGE } ?: false

            if (isGoogleTTSInstalled) {
                tts = TextToSpeech(context, null, GOOGLE_TTS_PACKAGE)
                useGoogleTTS = true
                configureJarvisVoice()
                SystemStatus.logEvent("TTS", "Google TTS initialized (Online)")
            } else {
                promptInstallGoogleTTS()
                fallbackToDefaultTTS()
            }
        } else {
            fallbackToDefaultTTS()
        }
    }

    private fun promptInstallGoogleTTS() {
        val intent = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    private fun fallbackToDefaultTTS() {
        useGoogleTTS = false
        SystemStatus.logEvent("TTS", "Falling back to default TTS")
        tts?.apply {
            setLanguage(Locale.US)
            setPitch(1.0f)  // Neutral pitch
            setSpeechRate(1.0f)  // Normal speed
        }
    }

    private fun configureJarvisVoice() {
        tts?.apply {
            setLanguage(Locale("hi", "IN"))
            setPitch(1.0f)  // Slightly higher pitch for clarity
            setSpeechRate(0.775f)  // Normal speed
            voice?.let { if (!it.isNetworkConnectionRequired) setVoice(it) }
        }
    }

    fun speak(text: String, language: Locale = Locale.US) {
        if (isSpeaking) return

        isSpeaking = true
        ttsScope.launch {
            tts?.apply {
                stop()
                setLanguage(language)
                setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) { this@TTSManager.isSpeaking = false }
                    override fun onError(utteranceId: String?) { this@TTSManager.isSpeaking = false }
                })
                speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts_${System.currentTimeMillis()}")
            }
        }
    }

    fun preprocessText(text: String): String {
        return text
            // Handle numbers first to prevent breaking
            .replace(Regex("(\\d+)")) { "${it.value} " }  // Add space after numbers

            // Then handle other replacements
            .replace("'", " ")       // don't -> don t
            .replace("\"", " ")      // Remove quotes
            .replace("’", " ")       // Handle different apostrophe types

            // Time formatting (modified to preserve numbers)
            .replace(Regex("(\\d{1,2}):(\\d{2})")) { match ->
                val (hours, mins) = match.destructured
                "$hours $mins"  // Remove AM/PM conversion to prevent breaks
            }

            // Punctuation handling (keep minimal spaces)
            .replace("!", "! ")
            .replace("?", "? ")
            .replace(".", ". ")
            .replace(",", ", ")

            // Final cleanup
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun splitHinglishText(text: String): Pair<List<String>, List<String>> {
        val englishParts = mutableListOf<String>()
        val hindiParts = mutableListOf<String>()

        // Modified regex to better handle numbers
        val regex = """([a-zA-Z0-9\s,!?.]+)|([\p{IsDevanagari}\s,!?.]+)""".toRegex()

        // Split while preserving number contexts
        var current = StringBuilder()
        var lastWasEnglish = false

        text.forEach { char ->
            when {
                char.toString().matches(Regex("[a-zA-Z0-9,!?. ]")) -> {
                    if (!lastWasEnglish && current.isNotEmpty()) {
                        hindiParts.add(current.toString())
                        current = StringBuilder()
                    }
                    current.append(char)
                    lastWasEnglish = true
                }
                char.toString().matches(Regex("[\\p{IsDevanagari},!?. ]")) -> {
                    if (lastWasEnglish && current.isNotEmpty()) {
                        englishParts.add(current.toString())
                        current = StringBuilder()
                    }
                    current.append(char)
                    lastWasEnglish = false
                }
                else -> current.append(char)
            }
        }

        // Add remaining
        if (current.isNotEmpty()) {
            if (lastWasEnglish) englishParts.add(current.toString())
            else hindiParts.add(current.toString())
        }

        return Pair(
            englishParts.map { it.trim() }.filter { it.isNotEmpty() },
            hindiParts.map { it.trim() }.filter { it.isNotEmpty() }
        )
    }

    fun speakHinglish(hinglishText: String) {
        val processedText = preprocessText(hinglishText)
        val (englishParts, hindiParts) = splitHinglishText(processedText)

        ttsScope.launch {
            // Speak numbers in consistent locale
            englishParts.forEach { part ->
                val locale = if (part.matches(Regex(".*\\d.*"))) Locale.US else Locale("en", "IN")
                speak(part, locale)
                delay(150) // Reduced pause for numbers
            }

            hindiParts.forEach { part ->
                speak(part, Locale("hi", "IN"))
                delay(200)
            }
        }
    }

    fun stop() {
        tts?.stop()
        tts?.shutdown()
    }

    companion object {
        private const val GOOGLE_TTS_PACKAGE = "com.google.android.tts"
    }
}