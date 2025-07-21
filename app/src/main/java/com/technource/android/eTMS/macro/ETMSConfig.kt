package com.technource.android.eTMS.macro

// Configuration constants
object ETMSConfig {
    const val NOTIFICATION_CHANNEL_ID = "etms_service"
    const val NOTIFICATION_ID = 1
    const val SYNC_WORK_NAME = "BackEnd_Sync"
    const val SEVEN_DAYS_MILLIS = 7 * 24 * 60 * 60 * 1000L
    const val RETRY_ATTEMPTS = 3
    const val RETRY_DELAY_MS = 60_000L // 1 minute
    const val SYNC_HOUR = 2 // 2 AM
    const val SYNC_MINUTE = 55
    const val DEV_MODE = true // Enable development mode
}