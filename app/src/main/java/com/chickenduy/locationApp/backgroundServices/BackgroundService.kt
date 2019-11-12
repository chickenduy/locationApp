package com.chickenduy.locationApp.backgroundServices

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModelProviders
import com.chickenduy.locationApp.data.database.TrackingDatabase
import com.chickenduy.locationApp.data.database.entity.GPS
import com.chickenduy.locationApp.data.repository.GPSRepository
import com.chickenduy.locationApp.ui.gps.GPSViewModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

class BackgroundService : Service() {
    private lateinit var notification: Notification
    private val binder = LocationServiceBinder()
    private var mLocationListener: LocationListener? = null
    private var mLocationManager: LocationManager? = null
    private val LOCATION_INTERVAL = 1000
    private val LOCATION_DISTANCE = 1
    private var gpsRepository: GPSRepository? = null

    private lateinit var wordViewModel: GPSViewModel

    override fun onCreate() {
        println("created the backgroundService")
        if (!this::notification.isInitialized) {
            notification = buildNotification(this)
            startForeground(1, notification)
        }
        gpsRepository = GPSRepository(TrackingDatabase.getDatabase(applicationContext).gPSDao())
        startTracking()
    }

    /**
     * Build permanent notification
     */
    private fun buildNotification(context: Context): Notification {
        /**
         * Depending on the Android Version, select channelId
         * https://developer.android.com/reference/android/support/v4/app/NotificationCompat.Builder.html#NotificationCompat.Builder(android.content.Context)
         */
        val channelId =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel("my_service", "My Background Service")
            } else {
                ""
            }

        val pendingIntent = PendingIntent.getActivity(context, 0, Intent(), 0)

        return NotificationCompat.Builder(context, channelId)
            .setContentTitle("Test")
            .setContentText("Test")
            //.setSmallIcon(R.drawable.ic_launcher_background)
            .setContentIntent(pendingIntent)
            .setTicker("Test")
            .setOngoing(true)
            .build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(
        channelId: String,
        channelName: String
    ): String {
        val chan = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

    private fun initializeLocationManager() {
        if (mLocationManager == null) {
            mLocationManager =
                applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        }
    }

    fun startTracking() {
        println("starting to track")
        initializeLocationManager()
        this.mLocationListener = LocationListener(LocationManager.GPS_PROVIDER)

        try {
            mLocationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                LOCATION_INTERVAL.toLong(),
                LOCATION_DISTANCE.toFloat(),
                this.mLocationListener
            )

        } catch (ex: SecurityException) {
            // Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (ex: IllegalArgumentException) {
            // Log.d(TAG, "gps provider does not exist " + ex.getMessage());
        }

    }

    /**
     * Either onTaskRemoved or onDestroy is called when the application is closed
     * The periodic work requests are de-registered and an intent is sent
     * to a service that restarts the BackgroundService.
     */
    override fun onDestroy() {
        super.onDestroy()
        Toast.makeText(
            this,
            "logging service done",
            Toast.LENGTH_SHORT
        ).show()
        Log.e(
            "LOGGINGDESTROY",
            "Service unexpectedly destroyed while GPSLogger was running. Will send broadcast to RestartReceiver."
        )
        sendBroadcast(Intent(applicationContext, BootUpReceiver::class.java))
    }

    /**
     * Either onTaskRemoved or onDestroy is called when the application is closed
     * The periodic work requests are de-registered and an intent is sent
     * to a service that restarts the BackgroundService.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Toast.makeText(
            this,
            "logging service done with onTaskRemoved",
            Toast.LENGTH_SHORT
        ).show()
        Log.e(
            "LOGGINGREMOVED",
            "Service unexpectedly destroyed while GPSLogger was running. Will send broadcast to RestartReceiver."
        )
        sendBroadcast(Intent(applicationContext, BootUpReceiver::class.java))
    }

    /**
     * This is required for a Service
     * */
    override fun onBind(intent: Intent): IBinder? {
        return binder
    }


    inner class LocationServiceBinder : Binder() {
        val service: BackgroundService
            get() = this@BackgroundService
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
                    timestamp = Date().time,
                    latitude = location.latitude.toFloat(),
                    longitude = location.longitude.toFloat())
                GlobalScope.launch {
                    gpsRepository?.insert(loc)
                    Log.e(TAG, "GPS Data saved")
                }
            }
            Toast.makeText(this@BackgroundService,"LAT: " + location.latitude + "\n LONG: " + location.longitude, Toast.LENGTH_SHORT).show()
        }

        override fun onProviderDisabled(provider: String) {
            Log.e(TAG, "onProviderDisabled: $provider")
        }

        override fun onProviderEnabled(provider: String) {
            Log.e(TAG, "onProviderEnabled: $provider")
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
            Log.e(TAG, "onStatusChanged: $status")
        }
    }

}