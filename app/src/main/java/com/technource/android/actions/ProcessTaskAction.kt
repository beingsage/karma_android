//package com.technource.android.actions
//
//import android.content.Context
//import android.content.SharedPreferences
//import android.util.Log
//
//class ProcessTaskAction : Action {
//    override fun execute(context: Context, taskName: String?) {
//        if (taskName != null) {
//            val prefs = context.getSharedPreferences("MyTaskAppPrefs", Context.MODE_PRIVATE)
//            with(prefs.edit()) {
//                putString("current_task", taskName)
//                apply()
//            }
//            Log.d("MyTaskApp", "Processed task: $taskName")
//        }
//    }
//
//    override fun getResponse(): String {
//        return "Task processed in my app"
//    }
//}