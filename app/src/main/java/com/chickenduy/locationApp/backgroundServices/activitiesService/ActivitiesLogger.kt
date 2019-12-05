package com.chickenduy.locationApp.backgroundServices.activitiesService

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.chickenduy.locationApp.MyApp
import com.chickenduy.locationApp.backgroundServices.BackgroundService
import com.chickenduy.locationApp.data.database.TrackingDatabase
import com.chickenduy.locationApp.data.database.entity.Activities
import com.chickenduy.locationApp.data.repository.ActivitiesRepository
import com.google.android.gms.location.ActivityTransitionEvent
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
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

            val activities = detectedActivities
                .filter { it.transitionType == 0 }
                .maxBy { it.elapsedRealTimeNanos }
                ?.activityType
            changeInterval(context, activities)

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
        /*if(ActivityRecognitionResult.hasResult(intent)) {
            Log.d(TAG,"Received Activities broadcast")
            val result = ActivityRecognitionResult.extractResult(intent)
            val detectedActivity = result.mostProbableActivity
            Log.d(TAG, "$detectedActivity")
            changeInterval(context, detectedActivity.type)
        }*/
        else
            Log.e(TAG,"something is missing")
    }

    /**
     * Sends a notification to the @see BackgroundService in order to
     * change the frequency of GPS updates.
     * The frequency itself is set in the GPS logging service, this method sends an extra
     * along the intent specifying the frequency (granularity) as one of 0,1,2.
     */
    private fun changeInterval(context: Context, activity: Int?) {
        Log.d("ACTIVITY_RECOGNITION", "Got activity for change: $activity")

        val i = Intent(context, BackgroundService::class.java)

        when (activity) {
            DetectedActivity.STILL -> {
                Log.d(TAG, "change to still")
                i.putExtra("interval", 60)
            }
            DetectedActivity.WALKING -> {
                Log.d(TAG, "change to walking")
                i.putExtra("interval", 5)
            }
            DetectedActivity.RUNNING -> {
                Log.d(TAG, "change to running")
                i.putExtra("interval", 2)
            }
            DetectedActivity.ON_BICYCLE -> {
                Log.d(TAG, "change to running")
                i.putExtra("interval", 2)
            }
            else -> {
                Log.d(TAG, "change to else")
                i.putExtra("interval", 1)
            }
        }
        context.startService(i)
    }
}