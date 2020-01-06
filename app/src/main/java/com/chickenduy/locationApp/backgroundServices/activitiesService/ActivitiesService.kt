package com.chickenduy.locationApp.backgroundServices.activitiesService

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*

/**
 * This class registers tracking of activities
 */
class ActivitiesService(context: Context) {
    private val logTAG = "ACTIVITIESSERVICE"

    private lateinit var mPendingIntent: PendingIntent
    private lateinit var activitiesProvider: ActivityRecognitionClient
    private val transitions = ArrayList<ActivityTransition>()

    init {
        Log.d(logTAG, "Starting ActivitiesService")
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            activitiesProvider = ActivityRecognition.getClient(context)
            val intent = Intent(context, ActivitiesLogger::class.java)
            mPendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0)
        }
        /*GlobalScope.launch {
            ActivitiesRepository(TrackingDatabase.getDatabase(context).activitiesDao()).deleteAll()
        }*/
        launchTransitionsTracker()
    }

    /**
     * Registers for updates that concern the activities
     * STILL, WALKING, RUNNING, ON_BICYCLE, IN_VEHICLE
     */
    private fun launchTransitionsTracker() {
        transitions.add(
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build()
        )

        transitions.add(
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build()
        )

        transitions.add(
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.WALKING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build()
        )

        transitions.add(
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.WALKING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build()
        )

        transitions.add(
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build()
        )

        transitions.add(
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build()
        )

        transitions.add(
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.ON_BICYCLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build()
        )

        transitions.add(
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.ON_BICYCLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build()
        )

        transitions.add(
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.RUNNING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build()
        )

        transitions.add(
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.RUNNING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build()
        )

        val request = ActivityTransitionRequest(transitions)

        val task = activitiesProvider.requestActivityTransitionUpdates(request, mPendingIntent)
        //val task = activitiesProvider.requestActivityUpdates(1000L, mPendingIntent)

        task.addOnFailureListener { e: Exception ->
            Log.e(logTAG, e.message!!)
        }
    }
}