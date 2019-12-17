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
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.chickenduy.locationApp.backgroundServices.BackgroundService
import com.chickenduy.locationApp.backgroundServices.MyExceptionHandler
import com.chickenduy.locationApp.ui.activity.ActivitiesView
import com.chickenduy.locationApp.ui.gps.GPSView
import com.chickenduy.locationApp.ui.steps.StepsView
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private val ACCESS_FINE_LOCATION_REQUEST_CODE = 1
    private val EXTERNAL_STORAGE_REQUEST_CODE = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.chickenduy.locationApp.R.layout.activity_main)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                ACCESS_FINE_LOCATION_REQUEST_CODE
            )
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                EXTERNAL_STORAGE_REQUEST_CODE
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            ACCESS_FINE_LOCATION_REQUEST_CODE -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted, yay! Do the
                // contacts-related task you need to do.
                Toast.makeText(applicationContext, "Permission granted", Toast.LENGTH_SHORT).show()
                startBackgroundService()
            } else {
                // permission denied, boo! Disable the
                // functionality that depends on this permission.
                Toast.makeText(applicationContext, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
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
        val queue = Volley.newRequestQueue(this)
        val url = "https://api.pushy.me/push?api_key=cfd5f664afd97266ed8ec89ac697b9dcded0afced39635320fc5bfb7a950c705"
        val message = JSONObject()
        message.put("to","657c59e3f5faeaf7005e04")
        val data = JSONObject()
        data.put("test","test")
        message.put("data", data)
        val res = Response.Listener<JSONObject> { response ->
            Log.d("COMS", response.toString())
            //sharedPref.edit().putBoolean("isRegistered", true).commit()
        }
        val err = Response.ErrorListener {
            Log.e("COMS", it.message)
        }
        val jsonRequest = JsonObjectRequest(Request.Method.POST, url, message, res, err)
        queue.add(jsonRequest)
    }
}
