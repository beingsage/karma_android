//package com.technource.android.module.miscModule.miscscreen.Body.ui.widgets
//
//import android.app.PendingIntent
//import android.appwidget.AppWidgetManager
//import android.appwidget.AppWidgetProvider
//import android.content.Context
//import android.content.Intent
//import android.widget.RemoteViews
//import com.technource.android.R
//import com.technource.android.module.miscModule.miscscreen.Body.BodyActivity
//
//class QuickLogWidgetProvider : AppWidgetProvider() {
//
//    override fun onUpdate(
//        context: Context,
//        appWidgetManager: AppWidgetManager,
//        appWidgetIds: IntArray
//    ) {
//        for (appWidgetId in appWidgetIds) {
//            updateAppWidget(context, appWidgetManager, appWidgetId)
//        }
//    }
//
//    private fun updateAppWidget(
//        context: Context,
//        appWidgetManager: AppWidgetManager,
//        appWidgetId: Int
//    ) {
//        val views = RemoteViews(context.packageName, R.layout.quick_log_widget)
//
//        // Set up click listeners for each button
//        setupButtonClickListener(context, views, R.id.btn_log_water, "LOG_WATER")
//        setupButtonClickListener(context, views, R.id.btn_log_weight, "LOG_WEIGHT")
//        setupButtonClickListener(context, views, R.id.btn_take_photo, "TAKE_PHOTO")
//        setupButtonClickListener(context, views, R.id.btn_start_workout, "START_WORKOUT")
//
//        // Update the widget
//        appWidgetManager.updateAppWidget(appWidgetId, views)
//    }
//
//    private fun setupButtonClickListener(
//        context: Context,
//        views: RemoteViews,
//        buttonId: Int,
//        action: String
//    ) {
//        val intent = Intent(context, BodyActivity::class.java).apply {
//            putExtra("WIDGET_ACTION", action)
//            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
//        }
//
//        val pendingIntent = PendingIntent.getActivity(
//            context,
//            buttonId,
//            intent,
//            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
//
//        views.setOnClickPendingIntent(buttonId, pendingIntent)
//    }
//}
