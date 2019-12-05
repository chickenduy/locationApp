package com.chickenduy.locationApp.backgroundServices.gpsService

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.chickenduy.locationApp.MyApp
import com.chickenduy.locationApp.data.database.TrackingDatabase
import com.chickenduy.locationApp.data.database.entity.GPS
import com.chickenduy.locationApp.data.repository.GPSRepository
import com.google.android.gms.location.LocationResult
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class GPSLogger: BroadcastReceiver() {
    private val TAG = "GPSLOGGER"

    private val gpsRepository: GPSRepository = GPSRepository(TrackingDatabase.getDatabase(MyApp.instance).gPSDao())

    override fun onReceive(p0: Context?, intent: Intent?) {
        if (LocationResult.hasResult(intent)) {
            Log.d(TAG, "Received GPS Broadcast")
            val result = LocationResult.extractResult(intent)
            Log.d(TAG, "${result.locations.size}")
            result.locations.forEach{
                val gps = GPS(0,
                    it.time,
                    it.longitude.toFloat(),
                    it.latitude.toFloat()
                )
                GlobalScope.launch {
                    gpsRepository.insert(gps)
                }
            }
        }
    }
}