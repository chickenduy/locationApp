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

/**
 * This class saves GPS data to a database
 */
class GPSLogger : BroadcastReceiver() {
    private val TAG = "GPSLOGGER"
    private val gpsRepository: GPSRepository =
        GPSRepository(TrackingDatabase.getDatabase(MyApp.instance).gPSDao())


    override fun onReceive(p0: Context?, intent: Intent?) {
//        GlobalScope.launch {
//            gpsRepository.deleteAll()
//        }
        if (LocationResult.hasResult(intent)) {
            //Log.d(TAG, "Received GPS Broadcast")
            Thread(Runnable {
                val result = LocationResult.extractResult(intent)
                result.locations.forEach {
                    val gps = GPS(
                        0,
                        it.time,
                        it.latitude,
                        it.longitude
                    )
                    Log.d(TAG, "Saved GPS data")
                    gpsRepository.insert(gps)
                }
            }).start()

        }
    }
}
