package com.chickenduy.locationApp.backgroundServices.gpsService

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices

class GPSService(private val context: Context) {
    private val TAG = "GPSSERVICE"

    private lateinit var locationProvider: FusedLocationProviderClient
    private lateinit var request: LocationRequest
    private lateinit var mPendingIntent: PendingIntent

    private val DEFAULTINTERVAL = 60
    private val MILSECONDS = 1000L

    init {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            locationProvider = LocationServices.getFusedLocationProviderClient(context)
            val intent = Intent(context, GPSLogger::class.java)
            mPendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0)
        }

        /*GlobalScope.launch {
            GPSRepository(TrackingDatabase.getDatabase(context).gPSDao()).deleteAll()
        }*/
        startTracking(DEFAULTINTERVAL)
    }

    fun startTracking(activity: Int) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            var interval = 0L
            interval = when(activity){
                0 -> 1000*60*10 //still
                1 -> 1000*60 //walking
                2 -> 1000*30 //running
                3 -> 1000*10 //biking
                4 -> 1000 //vehicle
                else -> 1000
            }

            Log.d(TAG, "change interval speed for ${interval/1000}s")
            request = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(interval)
                .setMaxWaitTime(1000*60*10)

            //locationProvider.flushLocations()
            val task = locationProvider.requestLocationUpdates(request, mPendingIntent)

            task.addOnFailureListener { e: Exception ->
                Log.e(TAG, "${e.message}")
            }
        }
        else
            Log.e("LOCATION_UPDATES", "We have no location permission")
    }

}

