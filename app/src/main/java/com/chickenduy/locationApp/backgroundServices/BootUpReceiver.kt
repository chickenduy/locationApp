package com.chickenduy.locationApp.backgroundServices

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat


class BootUpReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        try {
            if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
                Log.d("RESTART", "Phone was restarted, starting service ...")
            } else {
                Log.d("RESTART", "Service was killed, try restarting ...")
            }
            ContextCompat.startForegroundService(context, Intent(context, BackgroundService::class.java))
        } catch (e:Exception) {
            Log.e("RESTART", "Error: $e")
        }
    }
}