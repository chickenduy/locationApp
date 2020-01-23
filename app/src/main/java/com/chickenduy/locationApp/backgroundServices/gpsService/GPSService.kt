package com.chickenduy.locationApp.backgroundServices.gpsService

import android.Manifest
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices

/**
 * This class tracks GPS data in a certain interval
 */
class GPSService(private val context: Context) {
    private val TAG = "GPSSERVICE"

    private val locationProvider: FusedLocationProviderClient
    private var mPendingIntent: PendingIntent

    private val DEFAULTACTIVITY = DetectedActivity.STILL
    private val SECONDS = 1000L

    init {
        Log.d(TAG, "Starting GPSService")
        locationProvider = LocationServices.getFusedLocationProviderClient(context)
        val intent = Intent(context, GPSLogger::class.java)
        mPendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0)

        /*GlobalScope.launch {
            GPSRepository(TrackingDatabase.getDatabase(context).gPSDao()).deleteAll()
        }*/
        startTracking(DEFAULTACTIVITY)
    }

    fun startTracking(activity: Int) {
        Log.d(TAG, "startTracking")
        locationProvider.flushLocations()
        locationProvider.removeLocationUpdates(mPendingIntent)

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            val interval = when (activity) {
                DetectedActivity.STILL -> SECONDS * 5*60 //still
                DetectedActivity.WALKING -> SECONDS * 30 //walking
                DetectedActivity.RUNNING -> SECONDS * 15 //running
                DetectedActivity.ON_BICYCLE -> SECONDS * 5 //biking
                DetectedActivity.IN_VEHICLE -> SECONDS //vehicle
                else -> SECONDS
            }

            Log.d(TAG, "change interval speed for ${interval / SECONDS}s")
            val request = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(interval)
                .setFastestInterval(interval/2)
                .setMaxWaitTime(interval*5)
            val task = locationProvider.requestLocationUpdates(request, mPendingIntent)

            task.addOnFailureListener { e: Exception ->
                Log.e(TAG, "${e.message}")
            }
        } else
            Log.e("LOCATION_UPDATES", "We have no location permission")
    }

}

