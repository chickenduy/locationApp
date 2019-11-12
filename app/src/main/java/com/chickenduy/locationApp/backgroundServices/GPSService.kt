package com.chickenduy.locationApp.backgroundServices

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.chickenduy.locationApp.data.database.TrackingDatabase
import com.chickenduy.locationApp.data.database.entity.GPS
import com.chickenduy.locationApp.data.repository.GPSRepository
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

class GPSService(private val context: Context, private var interval: Long = 1000): BroadcastReceiver() {
    val TAG = "GPSService"
    private var mLocationListener: LocationListener? = null
    private var mLocationManager: LocationManager? = null
    private var gpsRepository: GPSRepository? = null

    init {
        GlobalScope.launch {
            GPSRepository(TrackingDatabase.getDatabase(context).gPSDao()).deleteAll()
        }
        gpsRepository = GPSRepository(TrackingDatabase.getDatabase(context).gPSDao())
        startTracking()
    }


    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "changeInterval")
        val interval = intent.extras?.getLong("changeInterval")
        if(interval != null)
            changeInterval(interval)
    }

    fun changeInterval(newInterval: Long) {
        mLocationManager?.removeUpdates(mLocationListener)
        this.interval = newInterval
        Log.d(TAG,"$interval")
        startTracking()
    }

    private fun initializeLocationManager() {
        if (mLocationManager == null) {
            mLocationManager =
                context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        }
    }

    private fun startTracking() {
        initializeLocationManager()
        this.mLocationListener = LocationListener(LocationManager.GPS_PROVIDER)
        try {
            Log.d(TAG, "$interval")
            mLocationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                interval,
                1F,
                this.mLocationListener
            )
        } catch (ex: SecurityException) {
            Log.e(TAG, "fail to request location update, ignore", ex)
        } catch (ex: IllegalArgumentException) {
            Log.e(TAG, "gps provider does not exist " + ex.message)
        }
    }

    private inner class LocationListener(provider: String) : android.location.LocationListener {
        private val TAG = "LocationListener"
        private var mLastLocation: Location? = null

        init {
            mLastLocation = Location(provider)
        }

        override fun onLocationChanged(location: Location) {
            mLastLocation = location
            Log.i(TAG, "LocationChanged: $location")
            if (gpsRepository != null) {
                val loc = GPS(
                    id=0,
                    timestamp = Date().time,
                    latitude = location.latitude.toFloat(),
                    longitude = location.longitude.toFloat())
                GlobalScope.launch {
                    gpsRepository?.insert(loc)
                    Log.d(TAG, "GPS Data saved")
                }
            }
            Toast.makeText(context,"LAT: " + location.latitude + "\n LONG: " + location.longitude, Toast.LENGTH_SHORT).show()
        }

        override fun onProviderDisabled(provider: String) {
            Log.d(TAG, "onProviderDisabled: $provider")
        }

        override fun onProviderEnabled(provider: String) {
            Log.d(TAG, "onProviderEnabled: $provider")
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
            Log.d(TAG, "onStatusChanged: $status")
        }
    }

}

