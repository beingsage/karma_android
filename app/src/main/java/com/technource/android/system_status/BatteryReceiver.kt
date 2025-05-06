package com.technource.android.system_status

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BatteryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BATTERY_LOW) {
            SystemStatus.logEvent("System", "Low battery detected")
        }
    }
}