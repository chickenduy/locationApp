package com.chickenduy.locationApp.backgroundServices.communicationService

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.android.volley.toolbox.Volley
import com.chickenduy.locationApp.MyApp
import com.chickenduy.locationApp.data.database.TrackingDatabase
import com.chickenduy.locationApp.data.repository.ActivitiesRepository
import com.chickenduy.locationApp.data.repository.GPSRepository
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

    private val activitiesRepository: ActivitiesRepository = ActivitiesRepository(
        TrackingDatabase.getDatabase(MyApp.instance).activitiesDao())
    private val gpsRepository: GPSRepository = GPSRepository(TrackingDatabase.getDatabase(MyApp.instance).gPSDao())


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
        val requestData = intent.getStringExtra("requestData")
        val data = intent.getStringExtra("data")

        if(requestHeader.isNullOrEmpty() || requestOptions.isNullOrEmpty() || requestData.isNullOrEmpty() || data.isNullOrEmpty()) {
            Log.e(TAG, "Got wrong request format")
            return
        }

        if (encryptionKey != null || iv  != null) {
            // TODO: Decrypt data
        }

        val requestHeaderObj = JSONObject(requestHeader)
        val requestOptionsObj = JSONObject(requestOptions)
        val requestDataObj = JSONObject(requestData)
        val dataObj = JSONObject(data)

        val request = JSONObject()
        request.put("requestHeader", requestHeaderObj)
        request.put("requestOptions", requestOptionsObj)
        request.put("requestData", requestDataObj)
        request.put("data", dataObj)

        when(requestHeaderObj.getString("type")) {
            "steps" -> {
                Log.d(TAG, "Starting aggregation for steps")
                aggregateSteps(requestDataObj, dataObj)
            }
            "walk" -> {
                Log.d(TAG, "Starting aggregation for walking")

            }
            "location" -> {
                Log.d(TAG, "Starting aggregation for location")

            }
            "presence" -> {
                Log.d(TAG, "Starting aggregation for presence")
                Log.d(TAG, requestData)
                Log.d(TAG, requestDataObj.getLong("start").toString())
                Log.d(TAG, requestDataObj.getLong("end").toString())
                Log.d(TAG, requestDataObj.getDouble("long").toString())
                Log.d(TAG, requestDataObj.getDouble("lat").toString())
                Log.d(TAG, requestDataObj.getDouble("radius").toString())

            }
        }
    }

    /**
     * Start internal aggregation of activity
     * @param requestData containing desired data
     * @param data containing aggregated data
     */
    private fun aggregateSteps(requestData: JSONObject, data: JSONObject) {
        // TODO: Aggregate activity into data
        val result = setJSONAttribute(data,"","")
        forwardToNextParticipant(result)
    }

    private fun aggregateLocations() {

    }

    /**
     * Forward newData to the next participant in the group
     * @param requestOptions containing next target and group ids
     */
    private fun forwardToNextParticipant(requestOptions: JSONObject) {
        val group = requestOptions.getJSONArray("group")
        val from = group[0]
        group.remove(0)
        if (group.length() == 0) {
            Log.d(TAG, "Send to server")
            // TODO: Send aggregated results to server
        }

        val newRequestOptions = setJSONAttribute(requestOptions, "from", from)

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