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

/**
 * This class tracks GPS data in a certain interval
 */
class GPSService(private val context: Context) {
    private val logTAG = "GPSSERVICE"

    private lateinit var locationProvider: FusedLocationProviderClient
    private lateinit var mPendingIntent: PendingIntent

    private val DEFAULTACTIVITY = 1
    private val SECONDS = 1000L

    init {
        Log.d(logTAG, "Starting GPSService")
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
        startTracking(DEFAULTACTIVITY)
    }

    fun startTracking(activity: Int) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            val interval = when(activity){
                0 -> SECONDS*60*10 //still
                1 -> SECONDS*30 //walking
                2 -> SECONDS*15 //running
                3 -> SECONDS*5 //biking
                4 -> SECONDS //vehicle
                else -> SECONDS
            }

            Log.d(logTAG, "change interval speed for ${interval/SECONDS}s")
            val request = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(interval)
                .setMaxWaitTime(SECONDS*60)

            locationProvider.flushLocations()
            val task = locationProvider.requestLocationUpdates(request, mPendingIntent)

            task.addOnFailureListener { e: Exception ->
                Log.e(logTAG, "${e.message}")
            }
        }
        else
            Log.e("LOCATION_UPDATES", "We have no location permission")
    }

}

