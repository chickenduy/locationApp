package com.chickenduy.locationApp.backgroundServices

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * This class restarts the background service in case the user reboots his phone.
 */
class BootUpReceiver : BroadcastReceiver() {
    private val logTAG = "RESTART"

    override fun onReceive(context: Context, intent: Intent) {
        try {
            if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
                Log.d(logTAG, "Phone was restarted, starting service ...")
            } else {
                Log.d(logTAG, "Service was killed, try restarting ...")
            }
            ContextCompat.startForegroundService(context, Intent(context, BackgroundService::class.java))
        } catch (e:Exception) {
            Log.e(logTAG, "Error: $e")
        }
    }

}