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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import me.pushy.sdk.Pushy
import org.json.JSONObject
import java.security.KeyPairGenerator
import javax.crypto.Cipher

/**
 * This class manages device registration
 */
class CommunicationService(private val context: Context) {
    private val logTAG = "COMSERVICE"
    private val PUBLICKEY = "publicKey"
    private val PRIVATEKEY = "privateKey"
    private val ISREGISTERED = "isRegistered"
    private val PASSWORD = "password"
    private val sharedPref = context.getSharedPreferences("options", Context.MODE_PRIVATE)
    private lateinit var token: String
    private val serverURI = "https://locationserver.eu-gb.mybluemix.net/"
    private val testURI = "10.0.2.2:3000"
    private val queue = Volley.newRequestQueue(this.context)

    // On app start
    init {
        Log.d(logTAG, "Starting ComService")
        // Called when the app is installed for the first time
        if (!sharedPref.contains(ISREGISTERED)) {
            Thread(Runnable {
                registerDevice()
                Pushy.listen(context)
                Pushy.subscribe("online", context)
            }).start()
        }
        // App is started for a second time
        else {
            // Called when device is not registered on server
            if (sharedPref.getBoolean(ISREGISTERED, false)) {
                GlobalScope.launch {
                    registerDevice()
                }
            }
            // Update the server
            else {
                updateDevice()
            }
            Pushy.listen(context)
            Thread(Runnable {
                Pushy.subscribe("online", context)
            }).start()
        }
        // Just additional information for dev, that Pushy token is not saved because writing permission is not present
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                Log.e(logTAG, "You will loose token upon uninstall.")
            }
        }
    }

    /**
     * Pings the server to update the timestamp
     */
    private fun updateDevice() {
        Log.d(logTAG, "Pinging Server")
        token = Pushy.getDeviceCredentials(context).token

        val pingRequest = JSONObject()
        pingRequest.put("id", token)
        pingRequest.put(PASSWORD, sharedPref.getString(PASSWORD, "") )

        val res = Response.Listener<JSONObject> { response ->
            Log.d(logTAG, response.toString())
        }

        val err = Response.ErrorListener { response ->
            Log.e(logTAG, response.toString())
            sharedPref.edit().putBoolean(ISREGISTERED, false).apply()
        }

        val jsonRequest = JsonObjectRequest(Request.Method.PATCH, serverURI + "crowd/ping", pingRequest, res, err)
        queue.add(jsonRequest)
    }

    /**
     * Register the device on Pushy and Node server
     */
    private fun registerDevice() {
        Log.d(logTAG, "Register Device")
        token = if (!Pushy.isRegistered(context)) Pushy.register(context) else Pushy.getDeviceCredentials(context).token

        var publicKey = sharedPref.getString(PUBLICKEY, "")

        /**
         * Create RSA Keypair for encryption
         */
        if(sharedPref.contains(PUBLICKEY) || publicKey == "") {
            val keyGen = KeyPairGenerator.getInstance("RSA")
            keyGen.initialize(2048)
            val keyPair = keyGen.genKeyPair()
            publicKey = "-----BEGIN PUBLIC KEY-----\n" +
                    Base64.encodeToString(keyPair.public.encoded, Base64.DEFAULT) +
                    "-----END PUBLIC KEY-----"
            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipher.init(Cipher.ENCRYPT_MODE, keyPair.public)

            with(sharedPref.edit()) {
                putString(PUBLICKEY, Base64.encodeToString(keyPair.public.encoded, Base64.DEFAULT))
                putString("publicKeyRaw", publicKey)
                putString(PRIVATEKEY, Base64.encodeToString(keyPair.private.encoded, Base64.DEFAULT))
                apply()
            }
        }

        val request = JSONObject()
        request.put("id", token)
        request.put(PUBLICKEY, publicKey)

        val res = Response.Listener<JSONObject> { response ->
            Log.d(logTAG, response.toString())
            with(sharedPref.edit()) {
                putBoolean(ISREGISTERED, true)
                putString(PASSWORD, response.getString(PASSWORD))
                apply()
            }
        }

        val err = Response.ErrorListener {
            Log.e(logTAG, it.message.toString())
            sharedPref.edit().putBoolean(ISREGISTERED, false).apply()
        }

        val jsonRequest = JsonObjectRequest(Request.Method.POST, testURI + "crowd", request, res, err)
        queue.add(jsonRequest)
    }

}