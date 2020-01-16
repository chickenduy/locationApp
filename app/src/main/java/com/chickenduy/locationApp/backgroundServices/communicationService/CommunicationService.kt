package com.chickenduy.locationApp.backgroundServices.communicationService

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import android.util.Log
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.pushy.sdk.Pushy
import org.json.JSONObject
import java.security.KeyPairGenerator

/**
 * This class manages device registration
 */
class CommunicationService(private val context: Context) {
    private val TAG = "COMSERVICE"
    private val PUBLICKEY = "publicKey"
    private val PRIVATEKEY = "privateKey"
    private val ISREGISTERED = "isRegistered"
    private val PASSWORD = "password"

    private val sharedPref = context.getSharedPreferences("options", Context.MODE_PRIVATE)
    private val serverURI = "https://locationserver.eu-gb.mybluemix.net/"
    private val testURI = "http://10.0.2.2:3000/"
    private val queue = Volley.newRequestQueue(this.context)

    // On app start
    init {
        Log.d(TAG, "Starting ComService")
        CoroutineScope(Dispatchers.Default).launch {
            updateDevice()
            Pushy.listen(context)
            Pushy.subscribe("online", context)
        }
        // Just additional information for dev, that Pushy token is not saved because writing permission is not present
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.e(TAG, "You will loose token upon uninstall.")
            }
        }
    }

    /**
     * Pings the server to update the timestamp
     */
    private fun updateDevice() {
        Log.d(TAG, "Pinging Server")

        val res = Response.Listener<JSONObject> { response ->
            Log.d(TAG, response.toString())
        }

        val err = Response.ErrorListener {
            Log.e(TAG, it.toString())
            sharedPref.edit().putBoolean(ISREGISTERED, false).apply()
            registerDevice()
            Log.e(TAG, "Failed to update")
        }

        try {
            val token = Pushy.getDeviceCredentials(context).token
            val pingRequest = JSONObject()
            pingRequest.put("id", token)
            pingRequest.put(PASSWORD, sharedPref.getString(PASSWORD, ""))

            val jsonRequest =
                JsonObjectRequest(Request.Method.POST, serverURI + "crowd/ping", pingRequest, res, err)
            Log.d(TAG, pingRequest.toString(2))
            jsonRequest.setShouldCache(false)
            queue.add(jsonRequest)
        }
        catch (e: Exception) {
            registerDevice()
        }
    }

    /**
     * Register the device on Pushy and Node server
     */
    private fun registerDevice() {
        Log.d(TAG, "Register Device")

        val res = Response.Listener<JSONObject> { response ->
            Log.d(TAG, response.toString())
            with(sharedPref.edit()) {
                putBoolean(ISREGISTERED, true)
                putString(PASSWORD, response.getString(PASSWORD))
                apply()
            }
        }

        val err = Response.ErrorListener {
            Log.e(TAG, it.message.toString())
            sharedPref.edit().putBoolean(ISREGISTERED, false).apply()
            Log.e(TAG, "Failed to register")
        }

        val token =
            if (!Pushy.isRegistered(context)) Pushy.register(context) else Pushy.getDeviceCredentials(
                context
            ).token
        var publicKey = sharedPref.getString(PUBLICKEY, "")

        /**
         * Create RSA Keypair for encryption
         */
        if (!sharedPref.contains(PUBLICKEY) || publicKey == "") {
            val keyGen = KeyPairGenerator.getInstance("RSA")
            keyGen.initialize(2048)
            val keyPair = keyGen.genKeyPair()
            publicKey = Base64.encodeToString(keyPair.public.encoded, Base64.NO_WRAP)
            with(sharedPref.edit()) {
                putString(PUBLICKEY, publicKey)
                putString(
                    PRIVATEKEY,
                    Base64.encodeToString(keyPair.private.encoded, Base64.NO_WRAP)
                )
                apply()
            }
        }

        val request = JSONObject()
        request.put("id", token)
        request.put(PUBLICKEY, publicKey)

        val jsonRequest =
            JsonObjectRequest(Request.Method.POST, serverURI + "crowd", request, res, err)
        Log.d(TAG, request.toString())
        jsonRequest.setShouldCache(false)
        queue.add(jsonRequest)
    }

}