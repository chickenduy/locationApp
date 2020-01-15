package com.chickenduy.locationApp.backgroundServices

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * This class restarts the background service in case the user reboots his phone.
 */
class BootUpReceiver : BroadcastReceiver() {
    private val TAG = "RESTART"

    override fun onReceive(context: Context, intent: Intent) {
        val serviceLauncher = Intent(context, BackgroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceLauncher)
        } else {
            context.startService(serviceLauncher)
        }
    }

}