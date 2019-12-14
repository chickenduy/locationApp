package com.chickenduy.locationApp.backgroundServices

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.chickenduy.locationApp.backgroundServices.activitiesService.ActivitiesService
import com.chickenduy.locationApp.backgroundServices.communicationService.ComService
import com.chickenduy.locationApp.backgroundServices.communicationService.CommunicationService
import com.chickenduy.locationApp.backgroundServices.gpsService.GPSService
import com.chickenduy.locationApp.backgroundServices.stepsService.StepsLogger

class BackgroundService : Service() {

    private val TAG = "BACKGROUNDSERVICE"

    private lateinit var notification: Notification
    private lateinit var gpsService: GPSService

    override fun onCreate() {
        Log.d(TAG, "Starting BackgroundSerivce")
        if (!this::notification.isInitialized) {
            notification = buildNotification(this)
            startForeground(1, notification)
        }
        // Start GPS Tracking
        gpsService = GPSService(applicationContext)
        // Start Activities Tracking
        ActivitiesService(applicationContext)
        // Start Steps Tracking
        val stepsLogger = StepsLogger(applicationContext)
        stepsLogger.run()
        // Start Listening to Communication
        val communicationService = ComService(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val newInterval = intent?.extras?.getInt("interval")
        if(newInterval != null)
            gpsService.startTracking(newInterval)
        return START_STICKY
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
                "my_service"
            }

        val pendingIntent = PendingIntent.getActivity(context, 0, Intent(), 0)

        return NotificationCompat.Builder(context, channelId)
            .setContentTitle("Ongoing Tracking")
            .setContentText("Please don't force quit the app")
            //.setSmallIcon(R.drawable.ic_launcher_background)
            .setContentIntent(pendingIntent)
            .setTicker("Test")
            .setOngoing(true)
            .build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String {
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
            TAG,
            "Service unexpectedly destroyed while GPSService was running. Will send broadcast to RestartReceiver."
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
            TAG,
            "Service unexpectedly destroyed while GPSService was running. Will send broadcast to RestartReceiver."
        )
        sendBroadcast(Intent(applicationContext, BootUpReceiver::class.java))
    }

    /**
     * This is required for a Service
     * */
    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}