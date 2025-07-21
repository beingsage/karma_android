package com.technource.android.eTMS.macro

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.technource.android.eTMS.DashboardActivity
import com.technource.android.system_status.SystemStatus
import com.technource.android.utils.DateFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date

class HeartbeatReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == "com.technource.android.HEARTBEAT_CHECK") {
            context?.let {
                CoroutineScope(Dispatchers.Default).launch {
                    if (!PersistentStore.checkHeartbeat(it) || !it.isServiceRunning(EternalTimeTableUnitService::class.java)) {
                        val serviceIntent = Intent(it, EternalTimeTableUnitService::class.java)
                        ContextCompat.startForegroundService(it, serviceIntent)
                        SystemStatus.logEvent("HeartbeatReceiver", "Restarted EternalTimeTableUnitService due to heartbeat or service failure")
                        DashboardActivity.logEventToWebView(
                            "HeartbeatReceiver",
                            "Restarted EternalTimeTableUnitService due to heartbeat or service failure",
                            "INFO",
                            DateFormatter.formatDateTime(Date())
                        )
                        SystemStatus.logEvent("EternalTaskManagementService", "Service restarted by HeartbeatReceiver")
                        DashboardActivity.logEventToWebView(
                            "EternalTaskManagementService",
                            "Service restarted by HeartbeatReceiver",
                            "INFO",
                            DateFormatter.formatDateTime(Date())
                        )
                    }
                }
            }
        }
    }
}