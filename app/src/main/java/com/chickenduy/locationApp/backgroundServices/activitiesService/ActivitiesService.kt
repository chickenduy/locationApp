package com.chickenduy.locationApp.backgroundServices.activitiesService

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.chickenduy.locationApp.data.database.TrackingDatabase
import com.chickenduy.locationApp.data.database.entity.ActivitiesDetailed
import com.chickenduy.locationApp.data.repository.ActivitiesDetailedRepository
import com.google.android.gms.location.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * This class registers tracking of activities
 */
class ActivitiesService(private val context: Context) {
    private val TAG = "ACTIVITIESSERVICE"
    private lateinit var mPendingIntent: PendingIntent
    private lateinit var activitiesProvider: ActivityRecognitionClient
    private val transitions = ArrayList<ActivityTransition>()

    init {
        Log.d(TAG, "Starting ActivitiesService")
        activitiesProvider = ActivityRecognition.getClient(context)
        val intent = Intent(context, ActivitiesLogger::class.java)
        mPendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0)
        GlobalScope.launch {
            val activitiesDetailedRepository = ActivitiesDetailedRepository(TrackingDatabase.getDatabase(context).activitiesDetailedDao())
            activitiesDetailedRepository.deleteAll()
            val activitiesList: List<ActivitiesDetailed> = listOf(
                ActivitiesDetailed(0,1577836800000,1577836860000, DetectedActivity.STILL),
                ActivitiesDetailed(0,1577836860000,1577836920000, DetectedActivity.WALKING),
                ActivitiesDetailed(0,1577836920000,1577836980000, DetectedActivity.STILL),
                ActivitiesDetailed(0,1577836980000,1577837040000, DetectedActivity.WALKING),
                ActivitiesDetailed(0,1577837040000,1577837100000, DetectedActivity.IN_VEHICLE),
                ActivitiesDetailed(0,1577837100000,1577837160000, DetectedActivity.STILL),
                ActivitiesDetailed(0,1577837160000,1577837220000, DetectedActivity.IN_VEHICLE),
                ActivitiesDetailed(0,1577837220000,1577837280000, DetectedActivity.WALKING),
                ActivitiesDetailed(0,1577837280000,1577837340000, DetectedActivity.WALKING),
                ActivitiesDetailed(0,1577837340000,1577837400000, DetectedActivity.WALKING)
            )
            activitiesDetailedRepository.insert(activitiesList)

        }
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
            Log.e(TAG, e.message!!)
        }
    }

    private fun generateRandomEntries() {
//        Thread(Runnable {
//            val activitiesDetailedRepository = ActivitiesDetailedRepository(TrackingDatabase.getDatabase(context).activitiesDetailedDao())
//            val activitiesList: List<ActivitiesDetailed> = listOf(
//                ActivitiesDetailed(0,1577836800000,1577836860000, DetectedActivity.STILL),
//                ActivitiesDetailed(0,1577836860000,1577836920000, DetectedActivity.WALKING),
//                ActivitiesDetailed(0,1577836920000,1577836980000, DetectedActivity.STILL),
//                ActivitiesDetailed(0,1577836980000,1577837040000, DetectedActivity.WALKING),
//                ActivitiesDetailed(0,1577837040000,1577837100000, DetectedActivity.IN_VEHICLE),
//                ActivitiesDetailed(0,1577837100000,1577837160000, DetectedActivity.STILL),
//                ActivitiesDetailed(0,1577837160000,1577837220000, DetectedActivity.IN_VEHICLE),
//                ActivitiesDetailed(0,1577837220000,1577837280000, DetectedActivity.WALKING),
//                ActivitiesDetailed(0,1577837280000,1577837340000, DetectedActivity.WALKING),
//                ActivitiesDetailed(0,1577837340000,1577837400000, DetectedActivity.WALKING)
//            )
//            activitiesDetailedRepository.insert(activitiesList)
//        }).start()
    }
}