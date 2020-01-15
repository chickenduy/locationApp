package com.chickenduy.locationApp.backgroundServices

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.*
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.chickenduy.locationApp.R
import com.chickenduy.locationApp.backgroundServices.activitiesService.ActivitiesService
import com.chickenduy.locationApp.backgroundServices.communicationService.CommunicationService
import com.chickenduy.locationApp.backgroundServices.gpsService.GPSService
import com.chickenduy.locationApp.backgroundServices.stepsService.StepsService
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * This class handles all logic, it starts all location related services
 */
class BackgroundService : Service() {

    private val TAG = "BACKGROUNDSERVICE"

    private var serviceLooper: Looper? = null
    private var serviceHandler: ServiceHandler? = null

    private lateinit var notification: Notification
    private lateinit var gpsService: GPSService
    private lateinit var activitiesService: ActivitiesService
    private lateinit var communicationService: CommunicationService
    private lateinit var stepsService: StepsService

    private var serviceStarterAlarmManager: AlarmManager? = null

    private inner class ServiceHandler(looper: Looper) : Handler(looper) {

        override fun handleMessage(msg: Message) {
            if (msg.arg2 != -1) {
                gpsService.startTracking(msg.arg2)
            }
        }
    }

    override fun onCreate() {
        Log.d(TAG, "Starting BackgroundService")

        GlobalScope.launch {
            setUpApp()
        }

        HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_FOREGROUND).apply {
            start()
            // Get the HandlerThread's Looper and use it for our Handler
            serviceLooper = looper
            serviceHandler = ServiceHandler(looper)
        }
        (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::MyWakelockTag").apply {
                acquire()
            }
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.d(TAG, "Starting Command")
        super.onStartCommand(intent, flags, startId)

        serviceHandler?.obtainMessage()?.also { msg ->
            msg.arg1 = startId
            msg.arg2 = intent.getIntExtra("activity", -1)
            serviceHandler?.sendMessage(msg)
        }

        if (!this::notification.isInitialized) {
            notification = buildNotification(this)
            startForeground(1337, notification)
        }
        return START_STICKY
    }

    private fun setUpApp() {
        if (!this::notification.isInitialized) {
            notification = buildNotification(this)
            startForeground(1337, notification)
        } else {
            startForeground(1337, notification)
        }

        // Start GPS Tracking
        gpsService = GPSService(applicationContext)
        // Start Activities Tracking
        activitiesService = ActivitiesService(applicationContext)
        // Start Steps Tracking
        stepsService = StepsService(applicationContext)
        // Start the Communication Service (Server, next Device)
        communicationService = CommunicationService(applicationContext)
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
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setTicker("Test")
            .setOngoing(true)
            .build()
    }

    /**
     * If API level is Oreo or above, a Notification Channel is required
     */
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
            "Application has been closed, trying to restart BackgroundService",
            Toast.LENGTH_SHORT
        ).show()
        Log.e(
            TAG,
            "Service unexpectedly destroyed while BackgroundService was running. Will send broadcast to RestartReceiver."
        )

        val intent = Intent(this, BootUpReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 1, intent, 0)

        if (pendingIntent == null) {
            Toast.makeText(this, "Some problems with creating of PendingIntent", Toast.LENGTH_LONG)
                .show()
        } else {
            if (serviceStarterAlarmManager == null) {
                serviceStarterAlarmManager = (getSystemService(ALARM_SERVICE) as AlarmManager)
                serviceStarterAlarmManager!!.setRepeating(
                    AlarmManager.ELAPSED_REALTIME,
                    SystemClock.elapsedRealtime() + 5 * 1000,
                    1 * 1000,
                    pendingIntent
                )
            }
        }

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
            "Application has been closed, trying to restart BackgroundService",
            Toast.LENGTH_SHORT
        ).show()
        Log.e(
            TAG,
            "Service unexpectedly destroyed while BackgroundService was running. Will send broadcast to RestartReceiver."
        )
        sendBroadcast(Intent(applicationContext, BootUpReceiver::class.java))
    }


    /**
     * This is required for a Service
     */
    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}