//package com.technource.android.actions
//
//import android.content.Context
//import android.content.Intent
//
//class OpenTaskAction : Action {
//    override fun execute(context: Context, taskName: String?) {
//        val intent = Intent(context, TaskActivity::class.java)
//        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//        context.startActivity(intent)
//    }
//
//    override fun getResponse(): String {
//        return "Opening task screen in my app"
//    }
//}