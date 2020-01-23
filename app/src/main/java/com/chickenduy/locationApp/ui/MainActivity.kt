package com.chickenduy.locationApp.ui

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.chickenduy.locationApp.R
import com.chickenduy.locationApp.backgroundServices.BackgroundService
import com.chickenduy.locationApp.ui.activity.ActivitiesView
import com.chickenduy.locationApp.ui.gps.GPSView
import com.chickenduy.locationApp.ui.steps.StepsView


class MainActivity : AppCompatActivity() {
    private val ACCESS_FINE_LOCATION_REQUEST_CODE = 1
    private lateinit var mServiceIntent: Intent


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermissions()
        // Restarts app on crash
//        Thread.setDefaultUncaughtExceptionHandler(
//            MyExceptionHandler(
//                this
//            )
//        )
//        if (intent.getBooleanExtra("crash", false)) {
//            Toast.makeText(this, "App restarted after crash", Toast.LENGTH_SHORT).show()
//        }
        if (intent.getBooleanExtra("reboot", false)) {
            Toast.makeText(this, "App restarted after reboot", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val permissionsApproved = ActivityCompat
                .checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat
                .checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat
                .checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
            if (!permissionsApproved) {
                ActivityCompat.requestPermissions(this, arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                        Manifest.permission.ACTIVITY_RECOGNITION),
                    ACCESS_FINE_LOCATION_REQUEST_CODE
                )
            }
            else {
                Toast.makeText(applicationContext, "Permission granted", Toast.LENGTH_SHORT).show()
                if(!isMyServiceRunning(BackgroundService::class.java)) {
                    startBackgroundService()
                }
                else {
                    Toast.makeText(applicationContext, "Background Service is already running", Toast.LENGTH_SHORT).show()
                }
            }
        }
        else {
            val permissionAccessFineLocationApproved = ActivityCompat
                .checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (!permissionAccessFineLocationApproved) {
                ActivityCompat.requestPermissions(this, arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION),
                    ACCESS_FINE_LOCATION_REQUEST_CODE
                )
            }
            else {
                Toast.makeText(applicationContext, "Permission granted", Toast.LENGTH_SHORT).show()
                if(!isMyServiceRunning(BackgroundService::class.java)) {
                    startBackgroundService()
                }
                else {
                    Toast.makeText(applicationContext, "Background Service is already running", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            ACCESS_FINE_LOCATION_REQUEST_CODE -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(applicationContext, "Permission granted", Toast.LENGTH_SHORT).show()
                if(!isMyServiceRunning(BackgroundService::class.java)) {
                    startBackgroundService()
                }
                else {
                    Toast.makeText(applicationContext, "Background Service is already running", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(applicationContext, "Need permissions to run this app", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startBackgroundService() {
        mServiceIntent = Intent(this, BackgroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(mServiceIntent)
        } else {
            startService(mServiceIntent)
        }
        //bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    override fun onDestroy() {
        Log.i("MAINACTIVITY", "onDestroy!");
        stopService(mServiceIntent)
        super.onDestroy()
    }

    fun viewSteps(view: View) {
        startActivity(Intent(this, StepsView::class.java))
    }

    fun viewGPS(view: View) {
        startActivity(Intent(this, GPSView::class.java))
    }

    fun viewActivities(view: View) {
        startActivity(Intent(this, ActivitiesView::class.java))
    }

    fun crashMe(view: View) {
        checkPermissions()
    }
}
