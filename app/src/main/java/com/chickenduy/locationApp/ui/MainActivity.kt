package com.chickenduy.locationApp.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.chickenduy.locationApp.backgroundServices.BackgroundService
import com.chickenduy.locationApp.backgroundServices.MyExceptionHandler
import com.chickenduy.locationApp.ui.gps.GPSView


class MainActivity : AppCompatActivity() {

    var backgroundService: BackgroundService? = null
    private val PERMISSION_REQUEST_CODE = 200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.chickenduy.locationApp.R.layout.activity_main)

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSION_REQUEST_CODE
            )
        } else {
            startBackgroundService()
        }

        Thread.setDefaultUncaughtExceptionHandler(
            MyExceptionHandler(
                this
            )
        )
        if (intent.getBooleanExtra("crash", false)) {
            Toast.makeText(this, "App restarted after crash", Toast.LENGTH_SHORT).show()
        }
        if (intent.getBooleanExtra("reboot", false)) {
            Toast.makeText(this, "App restarted after reboot", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startBackgroundService() {
        val intent = Intent(this, BackgroundService::class.java)
        startService(intent)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        Log.d("backgroundService","start BackgroundService")
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val name = className.className
            if (name.endsWith("BackgroundLocationService")) {
                backgroundService = (service as BackgroundService.LocationServiceBinder).service
                Log.d("GPS","GPS Ready")
            }
        }

        override fun onServiceDisconnected(className: ComponentName) {
            if (className.className == "BackgroundLocationService") {
                backgroundService = null
            }
        }
    }

    fun viewGPS(view: View) {
        startActivity(Intent(this, GPSView::class.java))
    }

    fun crashMe(view: View) {
        throw NullPointerException()
    }
}
