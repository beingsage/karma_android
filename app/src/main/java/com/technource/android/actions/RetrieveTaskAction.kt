//package com.technource.android.actions
//
//import android.content.Context
//import android.content.SharedPreferences
//
//class RetrieveTaskAction : Action {
//    override fun execute(context: Context, taskName: String?) {
//        // No action needed, just retrieve
//    }
//
//    override fun getResponse(): String {
//        val prefs = context.getSharedPreferences("MyTaskAppPrefs", Context.MODE_PRIVATE)
//        val task = prefs.getString("current_task", "No task set")
//        return "Your current task is $task from my app"
//    }
//}