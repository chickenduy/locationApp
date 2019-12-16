package com.chickenduy.locationApp.backgroundServices.communicationService

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.util.Log
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject


class CommunicationService(private val ctx: Context)  {
    private val TAG = "COMSERVICE"
    private var isRegistered = false
    private val sharedPref = ctx.getSharedPreferences("options", MODE_PRIVATE)

    init {
        Log.d(TAG, "Init coms")
        registerDevice()

        if (sharedPref.contains("isRegistered")) {
            isRegistered = sharedPref.getBoolean("isRegistered", false)
            Log.d(TAG, isRegistered.toString())
        }
        else {
            sharedPref.edit().putBoolean("isRegistered", false).commit()
            registerDevice()
        }
    }

    private fun registerDevice() {
        val queue = Volley.newRequestQueue(this.ctx)
        val url = "http://10.0.2.2:8000/test"
        val jsonBody = JSONObject()
        jsonBody.put("test", 1)
        val res = Response.Listener<JSONObject> { response ->
            Log.d(TAG, response.toString())
            //sharedPref.edit().putBoolean("isRegistered", true).commit()
        }
        val err = Response.ErrorListener {
            Log.e(TAG, it.message)
        }
        val jsonRequest = JsonObjectRequest(Request.Method.POST, url, jsonBody, res, err)
        queue.add(jsonRequest)
    }
}