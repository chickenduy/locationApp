package com.chickenduy.locationApp.backgroundServices.activitiesService

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.chickenduy.locationApp.data.database.TrackingDatabase
import com.chickenduy.locationApp.data.repository.ActivitiesRepository
import com.chickenduy.locationApp.data.repository.GPSRepository
import com.google.android.gms.location.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ActivitiesService(private val context: Context) {
    private val TAG = "ACTIVITIESSERVICE"

    private lateinit var mPendingIntent: PendingIntent
    private lateinit var activtiesProvider: ActivityRecognitionClient
    val transitions = ArrayList<ActivityTransition>()

    init {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            activtiesProvider = ActivityRecognition.getClient(context)
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

        val task = activtiesProvider.requestActivityTransitionUpdates(request, mPendingIntent)
        //val task = activtiesProvider.requestActivityUpdates(1000L, mPendingIntent)

        task.addOnFailureListener { e: Exception ->
            Log.e("TAG", e.message!!)
        }
    }
}