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
import me.pushy.sdk.Pushy
import org.json.JSONObject
import java.security.KeyPairGenerator
import javax.crypto.Cipher


class CommunicationService(private val ctx: Context) {
    private val TAG = "COMSERVICE"
    private val PUBLICKEY = "publicKey"
    private val PRIVATEKEY = "privateKey"
    private val ISREGISTERED = "isRegistered"
    private val sharedPref = ctx.getSharedPreferences("options", Context.MODE_PRIVATE)
    private var isRegistered: Boolean = false
    private lateinit var token: String
    private val serverLink = "https://locationserver.eu-gb.mybluemix.net/"
    private val queue = Volley.newRequestQueue(this.ctx)

    // On app start
    init {
        Log.d(TAG, "init coms")
        // First time app start
        if (!sharedPref.contains(ISREGISTERED)) {
            Thread(Runnable {
                registerDevice()
            }).start()
        }
        else {
            // registered on node
            if (sharedPref.getBoolean(ISREGISTERED, false)) {
                pingNode()
            }
            // registered device but registration on node failed
            else {
                Thread(Runnable {
                    registerDevice()
                }).start()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "You will loose token upon uninstall.")
            }
        }
    }

    private fun pingNode() {
        Log.d(TAG, "Pinging Server")
        token = Pushy.getDeviceCredentials(ctx).token

        val pingRequest = JSONObject()
        pingRequest.put("request", "find")
        pingRequest.put("id", token)

        val res = Response.Listener<JSONObject> { response ->
            Log.d(TAG, response.toString())
            val responseCode = response.get("status")
        }

        val err = Response.ErrorListener {
            Log.e(TAG, "test")
            Log.e(TAG, it.message.toString())
        }

        val jsonRequest = JsonObjectRequest(Request.Method.POST, serverLink + "user", pingRequest, res, err)
        queue.add(jsonRequest)
    }

    private fun registerDevice() {
        Log.d(TAG, "Register Device")
        token = if (!Pushy.isRegistered(ctx)) Pushy.register(ctx) else Pushy.getDeviceCredentials(ctx).token

        var publicKey = sharedPref.getString(PUBLICKEY, "")

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

        Log.d(TAG, publicKey)

        val request = JSONObject()
        request.put("request", "create")
        request.put("id", token)
        request.put(PUBLICKEY, publicKey)

        Log.d(TAG, request.toString())

        val res = Response.Listener<JSONObject> { response ->
            Log.d(TAG, response.toString())
            if (response.get("status") == "success") {
                sharedPref.edit().putBoolean("isRegistered", true).apply()
            }
            else {
                sharedPref.edit().putBoolean("isRegistered", false).apply()
            }
        }

        val err = Response.ErrorListener {
            Log.e(TAG, it.message.toString())
        }

        val jsonRequest = JsonObjectRequest(Request.Method.POST, serverLink + "user", request, res, err)
        queue.add(jsonRequest)
    }

}