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
import com.chickenduy.locationApp.R
import com.chickenduy.locationApp.backgroundServices.activitiesService.ActivitiesService
import com.chickenduy.locationApp.backgroundServices.communicationService.CommunicationService
import com.chickenduy.locationApp.backgroundServices.gpsService.GPSService
import com.chickenduy.locationApp.backgroundServices.stepsService.StepsService

/**
 * This class handles all logic, it starts all location related services
 */
class BackgroundService : Service() {

    private val TAG = "BACKGROUNDSERVICE"

    private lateinit var notification: Notification
    private lateinit var gpsService: GPSService
    private lateinit var activitiesService: ActivitiesService
    private lateinit var communicationService: CommunicationService
    private lateinit var stepsService: StepsService

    override fun onCreate() {
        Log.d(TAG, "Starting BackgroundService")
        Toast.makeText(this, "Starting BackgroundService", Toast.LENGTH_SHORT).show()

        gpsService = GPSService(applicationContext)
        // Start Activities Tracking
        activitiesService = ActivitiesService(applicationContext)
        // Start Steps Tracking
        stepsService = StepsService(applicationContext)
        // Start the Communication Service (Server, next Device)
        communicationService = CommunicationService(applicationContext)

//        (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
//            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::MyWakelockTag").apply {
//                acquire()
//            }
//        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.d(TAG, "Starting Command")
        super.onStartCommand(intent, flags, startId)

        if (!this::notification.isInitialized) {
            notification = buildNotification(this)
        }
        startForeground(1, notification)


        val newInterval = intent.extras?.getInt("activity")
        if(newInterval != null) {
            gpsService.startTracking(newInterval)
        }

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

        val notificationIntent = Intent(applicationContext, BackgroundService::class.java)
        notificationIntent.action = "BackgroundService" // A string containing the action name
        notificationIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Ongoing Tracking")
            .setContentText("Please don't force quit the app")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setTicker("Test")
            .setOngoing(true)
            .build()
        notification.flags = notification.flags or Notification.FLAG_NO_CLEAR
        return notification
    }

    /**
     * If API level is Oreo or above, a Notification Channel is required
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String {
        val channel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        channel.lightColor = Color.BLUE
        channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(channel)
        return channelId
    }

    /**
     * Either onTaskRemoved or onDestroy is called when the application is closed
     * The periodic work requests are de-registered and an intent is sent
     * to a service that restarts the BackgroundService.
     */
    override fun onDestroy() {
        super.onDestroy()
        Toast.makeText(this, "Application has been closed, trying to restart BackgroundService", Toast.LENGTH_SHORT).show()
        Log.e(TAG, "Service unexpectedly destroyed while BackgroundService was running. Will send broadcast to RestartReceiver.")
        sendBroadcast(Intent(applicationContext, BootUpReceiver::class.java))
    }

    /**
     * Either onTaskRemoved or onDestroy is called when the application is closed
     * The periodic work requests are de-registered and an intent is sent
     * to a service that restarts the BackgroundService.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Toast.makeText(this, "Application has been closed, trying to restart BackgroundService", Toast.LENGTH_SHORT).show()
        Log.e(TAG, "Service unexpectedly destroyed while BackgroundService was running. Will send broadcast to RestartReceiver.")
        sendBroadcast(Intent(applicationContext, BootUpReceiver::class.java))
    }

    /**
     * This is required for a Service
     */
    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}