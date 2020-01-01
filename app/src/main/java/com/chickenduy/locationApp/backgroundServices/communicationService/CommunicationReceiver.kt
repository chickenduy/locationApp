package com.chickenduy.locationApp.backgroundServices.communicationService

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.android.volley.toolbox.Volley
import com.chickenduy.locationApp.MyApp
import me.pushy.sdk.Pushy
import org.json.JSONException
import org.json.JSONObject
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

//Pushy Notification Receiver
class CommunicationReceiver: BroadcastReceiver() {

    private val TAG = "COMRECEIVER"
    private val PASSWORD = "password"
    private val ctx = MyApp.instance
    private val sharedPref = ctx.getSharedPreferences("options", Context.MODE_PRIVATE)
    private val serverURI = "https://locationserver.eu-gb.mybluemix.net/"
    private val queue = Volley.newRequestQueue(this.ctx)
    private var waitForConfirmation = false

    /**
     * Function called upon receiving Pushy notification
     * @param context application context
     * @param intent intent carries data from push notification
     */
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received Notification")

        // Receive Confirmation
        // TODO: OnConfirmation -> cancel sleep thread
        val confirmation = intent.getBooleanExtra("confirmation", false)
        if (confirmation) {
            waitForConfirmation = true
            return
        }

        // Receive Aggregation Request
        val encryptionKey = intent.getStringExtra("encryptionKey")
        val iv = intent.getStringExtra("iv")

        val requestHeader = intent.getStringExtra("requestHeader")
        val requestOptions = intent.getStringExtra("requestOptions")
        val data = intent.getStringExtra("data")

        if(requestHeader.isNullOrEmpty() || requestOptions.isNullOrEmpty() || data.isNullOrEmpty()) {
            Log.e(TAG, "Got wrong request format")
            return
        }

        if (encryptionKey != null || iv  != null) {
            // TODO: Decrypt data
        }

        val requestHeaderObj = JSONObject(requestHeader)
        val requestOptionsObj = JSONObject(requestOptions)
        val dataObj = JSONObject(data)

        when(requestOptionsObj.getString("type")) {
            "activity" -> {
                Log.d(TAG, "Starting first aggregation for activity")
                aggregateActivity(requestHeaderObj, dataObj)
            }
            "location" -> {
                Log.d(TAG, "Starting first aggregation for location")
            }
            "presence" -> {
                Log.d(TAG, "Starting first aggregation for presence")
            }
        }
    }

    /**
     * Start internal aggregation of activity
     * @param data
     * @param request
     */
    private fun aggregateActivity(request: JSONObject, data: JSONObject) {

        val test = JSONObject()
        // TODO: Aggregate activity into data
        forwardToNextParticipant(test, data)
    }

    /**
     * Forward newData to the next participant in the group
     * @param request
     * @param newData
     */
    private fun forwardToNextParticipant(request: JSONObject, newData: JSONObject) {
        val group = request.getJSONArray("group")
        group.remove(0)
        if (group.length() == 0) {
            Log.d(TAG, "Send to server")
        }

        val newRequest = setJSONAttribute(request, "from", Pushy.getDeviceCredentials(ctx).token)

        val keygen = KeyGenerator.getInstance("AES")
        val iv = ByteArray(16)

//        val message = JSONObject()
//        message.put("to", request.getJSONArray("group")[0])
//        message.put("time_to_live", 120)
//
//        val newRequest = setJSONAttribute(request, "group", group)
//
//        message.put("request", newRequest)
//        message.put("data", newData)
//
//        val res = Response.Listener<JSONObject> { response ->
//            Log.d(TAG, response.toString())
//
//        }
//
//        val err = Response.ErrorListener {
//            Log.e(TAG, it.message.toString())
//        }
//
//        val jsonRequest = JsonObjectRequest(Request.Method.POST, serverURI + "user", request, res, err)
//        queue.add(jsonRequest)

        // Start waiting for confirmation
        waitForConfirmation = false
        val test = Thread(Runnable {


            // TODO: timeout until get response
        })
        val test1 = test

    }

    private fun setJSONAttribute(obj: JSONObject, id: String, value: Any): JSONObject {
        if (obj.has(id))
        {
            try {
                obj.remove(id)
                obj.put(id, value)
            }
            catch (error: JSONException) {
                Log.e(TAG, error.toString())
            }
        }
        else {
            try {
                obj.put(id, value)
            }
            catch (error: JSONException) {
                Log.e(TAG, error.toString())
            }
        }
        return obj
    }

    fun generateSecretKey(): SecretKey? {
        val secureRandom = SecureRandom()
        val keyGenerator = KeyGenerator.getInstance("AES")
        //generate a key with secure random
        keyGenerator?.init(128, secureRandom)
        return keyGenerator?.generateKey()
    }

    fun encrypt(yourKey: SecretKey, fileData: ByteArray): ByteArray {
        val data = yourKey.encoded
        val skeySpec = SecretKeySpec(data, 0, data.size, "AES")
        val cipher = Cipher.getInstance("AES", "BC")
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, IvParameterSpec(ByteArray(cipher.blockSize)))
        return cipher.doFinal(fileData)
    }

    fun decrypt(yourKey: SecretKey, fileData: ByteArray): ByteArray {
        val decrypted: ByteArray
        val cipher = Cipher.getInstance("AES", "BC")
        cipher.init(Cipher.DECRYPT_MODE, yourKey, IvParameterSpec(ByteArray(cipher.blockSize)))
        decrypted = cipher.doFinal(fileData)
        return decrypted
    }

}