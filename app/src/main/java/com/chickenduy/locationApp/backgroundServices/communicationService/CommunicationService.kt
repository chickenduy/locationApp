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

    init {
        Thread(Runnable {
            registerDevice()
        }).start()
        Log.d(TAG, "init coms")
        isRegistered = sharedPref.getBoolean(ISREGISTERED, false) || sharedPref.contains(ISREGISTERED)
        if (!isRegistered) {
            if (Pushy.getDeviceCredentials(ctx).token != null) {
                token = Pushy.getDeviceCredentials(ctx).token
            }
            else {
                Thread(Runnable {
                    registerDevice()
                }).start()
            }
        }
        else {
            token = Pushy.getDeviceCredentials(ctx).token
            Pushy.listen(ctx)
            Thread(Runnable {
                Pushy.subscribe("online", ctx)
                Log.d(TAG, "subscribed to online")
            }).start()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "You will loose token upon uninstall.")
            }
        }
    }

    private fun registerDevice() {
        token = Pushy.register(ctx)
        if (token.isEmpty()) {
            sharedPref.edit().putBoolean(ISREGISTERED, true).apply()
        }
        val queue = Volley.newRequestQueue(this.ctx)
        val publicKey: String

        if(!sharedPref.contains(PUBLICKEY)) {
            val keyGen = KeyPairGenerator.getInstance("RSA")
            keyGen.initialize(2048)
            val keyPair = keyGen.genKeyPair()
            publicKey = "-----BEGIN PUBLIC KEY-----\n" +
                    Base64.encodeToString(keyPair.public.encoded, Base64.DEFAULT) +
                    "-----END PUBLIC KEY-----"
            Log.d(TAG, publicKey)
            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipher.init(Cipher.ENCRYPT_MODE, keyPair.public)
            with(sharedPref.edit()) {
                putString(PUBLICKEY, Base64.encodeToString(keyPair.public.encoded, Base64.DEFAULT))
                putString("publicKeyRaw", publicKey)
                putString(PRIVATEKEY, Base64.encodeToString(keyPair.private.encoded, Base64.DEFAULT))
            }
            sharedPref.edit().putString(PUBLICKEY, publicKey).apply()
        }
        else {
            publicKey = sharedPref.getString(PUBLICKEY, "").toString()
        }

        val request = JSONObject()
        request.put("id", token)
        request.put(PUBLICKEY, publicKey)

        val res = Response.Listener<JSONObject> { response ->
            Log.d(TAG, response.toString())
            val responseCode = response.get("status")
            Log.d(TAG, responseCode.toString())
            if (responseCode == "success") {
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