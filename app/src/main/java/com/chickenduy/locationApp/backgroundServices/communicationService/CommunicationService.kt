package com.chickenduy.locationApp.backgroundServices.communicationService

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class CommunicationService: FirebaseMessagingService()  {
    private val TAG = "COMSERVICE"

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")
        // Check if message contains a data payload.
        if(remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: " + remoteMessage.data)
        }
    }

    override fun onDeletedMessages() {
        super.onDeletedMessages()
    }

}