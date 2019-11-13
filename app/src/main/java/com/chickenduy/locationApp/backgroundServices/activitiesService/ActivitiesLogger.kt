package com.chickenduy.locationApp.backgroundServices.activitiesService

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.chickenduy.locationApp.MyApp
import com.chickenduy.locationApp.data.database.TrackingDatabase
import com.chickenduy.locationApp.data.database.entity.Activities
import com.chickenduy.locationApp.data.repository.ActivitiesRepository
import com.google.android.gms.location.ActivityTransitionEvent
import com.google.android.gms.location.ActivityTransitionResult
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

class ActivitiesLogger: BroadcastReceiver() {
    private val TAG = "ACTIVITIESLOGGER"

    private val activitiesRepository: ActivitiesRepository = ActivitiesRepository(TrackingDatabase.getDatabase(MyApp.instance).activitiesDao())


    override fun onReceive(context: Context, intent: Intent) {
        if(ActivityTransitionResult.hasResult(intent))
        {
            Log.d(TAG,"Received Activities broadcast")
            val result = ActivityTransitionResult.extractResult(intent)
            val detectedActivities = result?.transitionEvents as List<ActivityTransitionEvent>
            Log.d(TAG, detectedActivities.toString())

            detectedActivities.forEach{
                val now = Date().time
                val activities = Activities(
                    0L,
                    now,
                    it.transitionType,
                    it.activityType
                )
                Log.d(TAG, "Saving activities")
                GlobalScope.launch {
                    activitiesRepository.insert(activities)
                    Log.d(TAG, "Saved activities")
                }
            }
        }
        else
            Log.e(TAG,"something is missing")
    }
}