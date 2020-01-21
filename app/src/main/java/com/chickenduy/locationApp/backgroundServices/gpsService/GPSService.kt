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
class GPSService: Service() {
    private val TAG = "GPSSERVICE"

    private lateinit var locationProvider: FusedLocationProviderClient
    private lateinit var mPendingIntent: PendingIntent

    private val DEFAULTACTIVITY = DetectedActivity.STILL
    private val SECONDS = 1000L

    override fun onCreate() {
        Log.d(TAG, "Creating GPSService")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d(TAG, "Starting GPSService")
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            locationProvider = LocationServices.getFusedLocationProviderClient(applicationContext)
            val bIntent = Intent(applicationContext, GPSLogger::class.java)
            mPendingIntent = PendingIntent.getBroadcast(applicationContext, 0, bIntent, 0)
        }
        if(intent != null) {
            val interval = intent.extras?.getInt("activity")
            if(interval != null) {
                startTracking(interval)
            }
            else {
                startTracking(DEFAULTACTIVITY)
            }
        }
        else {
            startTracking(DEFAULTACTIVITY)
        }
        /*GlobalScope.launch {
            GPSRepository(TrackingDatabase.getDatabase(context).gPSDao()).deleteAll()
        }*/
        return START_STICKY
    }

    private fun startTracking(activity: Int) {
        Log.d(TAG, "startTracking")
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            val interval = when (activity) {
                DetectedActivity.STILL -> SECONDS * 5*60 //still
                DetectedActivity.WALKING -> SECONDS * 30 //walking
                DetectedActivity.RUNNING -> SECONDS * 5 //running
                DetectedActivity.ON_BICYCLE -> SECONDS //biking
                DetectedActivity.IN_VEHICLE -> SECONDS //vehicle
                else -> SECONDS
            }
            Log.d(TAG, "change interval speed for ${interval / SECONDS}s")
            val request = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(interval)
                .setFastestInterval(interval/2)
                .setMaxWaitTime(interval*10)
            locationProvider.flushLocations()
            val task = locationProvider.requestLocationUpdates(request, mPendingIntent)

            task.addOnFailureListener { e: Exception ->
                Log.e(TAG, "${e.message}")
            }
        } else
            Log.e("LOCATION_UPDATES", "We have no location permission")
    }


    override fun onBind(p0: Intent?): IBinder? {
        return null
    }
}

