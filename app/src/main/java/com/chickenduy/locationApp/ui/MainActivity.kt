package com.chickenduy.locationApp.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.chickenduy.locationApp.backgroundServices.BackgroundService
import com.chickenduy.locationApp.backgroundServices.MyExceptionHandler
import com.chickenduy.locationApp.ui.activity.ActivitiesView
import com.chickenduy.locationApp.ui.gps.GPSView
import com.chickenduy.locationApp.ui.steps.StepsView
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.iid.FirebaseInstanceId

class MainActivity : AppCompatActivity() {
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

        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) { // Request both READ_EXTERNAL_STORAGE and WRITE_EXTERNAL_STORAGE so that the
// Pushy SDK will be able to persist the device token in the external storage
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                0
            )
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

        FirebaseInstanceId.getInstance().instanceId
            .addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w("COMSERVICE", "getInstanceId failed", task.exception)
                    return@OnCompleteListener
                }
                // Get new Instance ID token
                val token = task.result?.token
                // Log and toast
                Log.d("MAIN", token)
            })
    }

    private fun startBackgroundService() {
        val intent = Intent(this, BackgroundService::class.java)
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            startForegroundService(intent)
        }
        else {
            startService(intent)
        }
        //bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
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

    }
}
