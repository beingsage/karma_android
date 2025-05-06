package com.technource.android.system_status

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent


class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            SystemStatus.initialize(context)
            SystemStatus.logEvent("System", "Device rebooted, service restarting")
            context.startForegroundService(Intent(context, SystemStatusService::class.java))
        }
    }
}