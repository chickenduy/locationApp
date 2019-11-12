package com.chickenduy.locationApp.backgroundServices

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.google.android.gms.location.ActivityTransitionEvent
import com.google.android.gms.location.ActivityTransitionResult

class ActivityService: BroadcastReceiver() {
    val TAG = "ActivityService"

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received something")
        Toast.makeText(
            context,
            "Received something",
            Toast.LENGTH_SHORT
        ).show()
        if(ActivityTransitionResult.hasResult(intent))
        {
            Log.d(TAG,"received broadcast")
            val result = ActivityTransitionResult.extractResult(intent)
            val detectedActivities = result?.transitionEvents as List<ActivityTransitionEvent>
            Log.d(TAG, detectedActivities.toString())
        }
        else
            Log.e(TAG,"something is missing")
    }
}