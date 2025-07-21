//package com.technource.android.actions
//
//import android.content.BroadcastReceiver
//import android.content.Context
//import android.content.Intent
//import android.speech.tts.TextToSpeech
//import android.util.Log
//import com.technource.android.TaskApplication
//import java.util.Locale
//
//class TaskBroadcastReceiver : BroadcastReceiver(), TextToSpeech.OnInitListener {
//    private var tts: TextToSpeech? = null
//    private var taskName: String? = null
//
//    override fun onReceive(context: Context, intent: Intent) {
//        taskName = intent.getStringExtra("taskName") ?: getCurrentTask(context)
//        Log.d("TaskBroadcastReceiver", "Received task: $taskName")
//
//        // Initialize Text-to-Speech
//        tts = TextToSpeech(context, this)
//    }
//
//    override fun onInit(status: Int) {
//        if (status == TextToSpeech.SUCCESS) {
//            tts?.language = Locale.US
//            val response = "Your current task is $taskName in  Karma"
//            tts?.speak(response, TextToSpeech.QUEUE_FLUSH, null, null)
//        } else {
//            Log.e("TaskBroadcastReceiver", "TTS Initialization Failed")
//        }
//    }
//
//    private fun getCurrentTask(context: Context): String {
////        val viewModel = TaskApplication.instance.viewModel
////        val currentTask = viewModel.getCurrentActiveTask()
////        return currentTask?.title ?: "No task is currently running"
//        return "its tatti time"
//    }
//
//    private fun shutdownTTS() {
//        tts?.stop()
//        tts?.shutdown()
//        tts = null
//    }
//}