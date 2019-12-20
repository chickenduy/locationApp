package com.chickenduy.locationApp.backgroundServices.communicationService

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

//Pushy Notification Receiver
class CommunicationReceiver: BroadcastReceiver() {

    private val TAG = "COMRECEIVER"
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received Notification")
    }


}