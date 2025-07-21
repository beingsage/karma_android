//package com.technource.android.actions
//
//import android.content.Context
//import android.content.SharedPreferences
//
//class ExecuteTaskAction : Action {
//    override fun execute(context: Context, taskName: String?) {
//        if (taskName != null) {
//            val prefs = context.getSharedPreferences("MyTaskAppPrefs", Context.MODE_PRIVATE)
//            with(prefs.edit()) {
//                putString("current_task", taskName)
//                apply()
//            }
//        }
//    }
//
//    override fun getResponse(): String {
//        return "Your current task has been set from my app"
//    }
//}