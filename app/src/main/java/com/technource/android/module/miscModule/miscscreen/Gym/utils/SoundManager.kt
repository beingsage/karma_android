package com.technource.android.module.miscModule.miscscreen.Gym.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.technource.android.R

class SoundManager(private val context: Context) {

    private var soundPool: SoundPool? = null
    private var soundIds = mutableMapOf<String, Int>()
    private var isEnabled = true

    init {
        initializeSoundPool()
        loadSounds()
    }

    private fun initializeSoundPool() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()
    }

    private fun loadSounds() {
        soundPool?.let { pool ->
            soundIds["timer_start"] = pool.load(context, R.raw.timer_start, 1)
            soundIds["timer_pause"] = pool.load(context, R.raw.timer_pause, 1)
            soundIds["timer_resume"] = pool.load(context, R.raw.timer_resume, 1)
            soundIds["set_complete"] = pool.load(context, R.raw.set_complete, 1)
            soundIds["rest_complete"] = pool.load(context, R.raw.rest_complete, 1)
            soundIds["workout_complete"] = pool.load(context, R.raw.workout_complete, 1)
        }
    }

    fun playTimerStartSound() {
        playSound("timer_start")
    }

    fun playTimerPauseSound() {
        playSound("timer_pause")
    }

    fun playTimerResumeSound() {
        playSound("timer_resume")
    }

    fun playSetCompleteSound() {
        playSound("set_complete")
    }

    fun playRestCompleteSound() {
        playSound("rest_complete")
    }

    fun playWorkoutCompleteSound() {
        playSound("workout_complete")
    }

    private fun playSound(soundKey: String) {
        if (!isEnabled) return

        soundIds[soundKey]?.let { soundId ->
            soundPool?.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f)
        }
    }

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

    fun release() {
        soundPool?.release()
        soundPool = null
        soundIds.clear()
    }
}
