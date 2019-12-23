package com.chickenduy.locationApp.backgroundServices.communicationService

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.chickenduy.locationApp.MyApp
import me.pushy.sdk.Pushy
import org.json.JSONObject

//Pushy Notification Receiver
class CommunicationReceiver: BroadcastReceiver() {

    private val TAG = "COMRECEIVER"
    private val PASSWORD = "password"
    private val ctx = MyApp.instance
    private val sharedPref = ctx.getSharedPreferences("options", Context.MODE_PRIVATE)
    private val serverURI = "https://locationserver.eu-gb.mybluemix.net/"
    private val queue = Volley.newRequestQueue(this.ctx)


    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received Notification")
        updateDevice()
    }

    private fun updateDevice() {
        Log.d(TAG, "Pinging Server")
        val token = Pushy.getDeviceCredentials(ctx).token

        val pingRequest = JSONObject()
        pingRequest.put("id", token)
        pingRequest.put(PASSWORD, sharedPref.getString(PASSWORD, "") )

        val res = Response.Listener<JSONObject> { response ->
            Log.d(TAG, response.toString())
        }

        val err = Response.ErrorListener { response ->
            Log.e(TAG, response.toString())
        }

        val jsonRequest = JsonObjectRequest(Request.Method.PATCH, serverURI + "user", pingRequest, res, err)
        queue.add(jsonRequest)
    }

}