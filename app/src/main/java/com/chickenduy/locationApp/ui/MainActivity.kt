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
import com.google.android.gms.tasks.OnCompleteListener
import org.json.JSONObject

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
        val queue = Volley.newRequestQueue(applicationContext)
        val url = "http://10.0.2.2:8000/user"
        val jsonBody = JSONObject()
        jsonBody.put("test", 1)
        val res = Response.Listener<JSONObject> { response ->
            Log.d("COMS", response.toString())
            //sharedPref.edit().putBoolean("isRegistered", true).commit()
        }
        val err = Response.ErrorListener {
            Log.e("COMS", it.message)
        }
        val jsonRequest = JsonObjectRequest(Request.Method.POST, url, jsonBody, res, err)
        queue.add(jsonRequest)
    }
}
