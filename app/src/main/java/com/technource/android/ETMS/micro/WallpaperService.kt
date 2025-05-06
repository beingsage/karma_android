package com.technource.android.ETMS.micro

import android.app.Service
import android.content.Intent
import android.os.IBinder
import java.util.Timer

class WallpaperService : Service() {
    private val timer = Timer()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        DynamicWallpaperTaskIterator.startTaskWallpaperCycle(this)
        return START_STICKY
    }

    override fun onDestroy() {
        timer.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
