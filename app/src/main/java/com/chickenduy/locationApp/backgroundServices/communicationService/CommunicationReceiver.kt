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
import com.chickenduy.locationApp.backgroundServices.communicationService.model.data.BasicData
import com.chickenduy.locationApp.backgroundServices.communicationService.model.data.LocationData
import com.chickenduy.locationApp.backgroundServices.communicationService.model.entity.Coordinates
import com.chickenduy.locationApp.backgroundServices.communicationService.model.entity.Location
import com.chickenduy.locationApp.backgroundServices.communicationService.model.message.PresenceOptions
import com.chickenduy.locationApp.backgroundServices.communicationService.model.message.RequestHeader
import com.chickenduy.locationApp.backgroundServices.communicationService.model.options.LocationOptions
import com.chickenduy.locationApp.backgroundServices.communicationService.model.options.RequestOptions
import com.chickenduy.locationApp.backgroundServices.communicationService.model.options.StepsOptions
import com.chickenduy.locationApp.backgroundServices.communicationService.model.options.WalkOptions
import com.chickenduy.locationApp.data.database.TrackingDatabase
import com.chickenduy.locationApp.data.repository.ActivitiesRepository
import com.chickenduy.locationApp.data.repository.GPSRepository
import com.chickenduy.locationApp.data.repository.StepsRepository
import com.google.gson.Gson
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import org.json.JSONException
import org.json.JSONObject
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * This class manages notifications and forwards to next device
 */
class CommunicationReceiver: BroadcastReceiver() {

    private val logTAG = "COMRECEIVER"
    private val PASSWORD = "password"
    private val ctx = MyApp.instance
    private val sharedPref = ctx.getSharedPreferences("options", Context.MODE_PRIVATE)
    private val serverURI = "https://locationserver.eu-gb.mybluemix.net/"
    private val pushyURI = "https://api.pushy.me/push?api_key=cfd5f664afd97266ed8ec89ac697b9dcded0afced39635320fc5bfb7a950c705"
    private val queue = Volley.newRequestQueue(this.ctx)
    private var waitForConfirmation = false
    private val activitiesRepository: ActivitiesRepository = ActivitiesRepository(TrackingDatabase.getDatabase(MyApp.instance).activitiesDao())
    private val gpsRepository: GPSRepository = GPSRepository(TrackingDatabase.getDatabase(MyApp.instance).gPSDao())
    private val stepsRepository: StepsRepository = StepsRepository(TrackingDatabase.getDatabase(MyApp.instance).stepsDao())
    private val gson = Gson()
    private lateinit var job: Job
    /**
     * Function called upon receiving Pushy notification
     * @param context application context
     * @param intent intent carries data from push notification
     */
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(logTAG, "Received Notification")

        // Receive Confirmation
        // TODO: OnConfirmation -> cancel sleep thread
        val confirmation = intent.getBooleanExtra("confirmation", false)
        if (confirmation) {
            waitForConfirmation = true
            Log.d(logTAG, "cancel timeout")
            job.cancel()
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
            Log.e(logTAG, "Got wrong request format")
            return
        }

        if (encryptionKey != null || iv  != null) {
            // TODO: Decrypt data
        }

        // TODO: Start aggregation
        val requestHeaderObj = gson.fromJson(requestHeader, RequestHeader::class.java)
        val requestOptionsObj = gson.fromJson(requestOptions, RequestOptions::class.java)

        Log.e(logTAG, requestOptionsObj.toString())

        // TODO: Send confirmation
        if(requestOptionsObj.from.isNotEmpty()) {
            sendConfirmation(requestOptionsObj.from)
        }

        when(requestHeaderObj.type) {
            "steps" -> {
                val requestDataObj = gson.fromJson(requestData, StepsOptions::class.java)
                val dataObj = gson.fromJson(data, BasicData::class.java)
                val newDataObj = aggregateSteps(requestDataObj, dataObj)
                prepareForNextParticipant(requestHeaderObj, requestOptionsObj, requestDataObj, newDataObj)
            }
            "walk" -> {
                Log.d(logTAG, "Starting aggregation for walking")
                val requestDataObj = gson.fromJson(requestData, WalkOptions::class.java)
                val dataObj = gson.fromJson(data, BasicData::class.java)
                prepareForNextParticipant(requestHeaderObj, requestOptionsObj, requestDataObj, dataObj)

            }
            "location" -> {
                Log.d(logTAG, "Starting aggregation for location")
                val requestDataObj = gson.fromJson(requestData, LocationOptions::class.java)
                val dataObj = gson.fromJson(data, LocationData::class.java)
                val newDataObj = aggregateLocation(requestDataObj, dataObj)
                prepareForNextParticipant(requestHeaderObj, requestOptionsObj, requestDataObj, newDataObj)
            }
            "presence" -> {
                Log.d(logTAG, "Starting aggregation for presence")
                val requestDataObj = gson.fromJson(requestData, PresenceOptions::class.java)
                val dataObj = gson.fromJson(data, BasicData::class.java)
                prepareForNextParticipant(requestHeaderObj, requestOptionsObj, requestDataObj, dataObj)
            }
        }
    }

    /**
     * Start internal aggregation of activity
     */
    private fun aggregateSteps(options: StepsOptions, data: BasicData): BasicData {
        val date = DateTime(options.date)
        val startDate = date.withTimeAtStartOfDay().millis
        val endDate = date.plusDays(1).withTimeAtStartOfDay().millis
        var steps = 0

        val stepsArray = stepsRepository.getByTimestamp(startDate, endDate)
        stepsArray.forEach {
            steps += it.steps
        }
        data.addRaw(steps)

        return data
        // TODO: Aggregate activity into data
    }

    private fun aggregateLocation(options: LocationOptions, data: LocationData): LocationData {
        val locations = gpsRepository.getByTimestamp(options.timestamp)
        val accuracy = 10.0.pow(options.accuracy).toInt()
        val location = Location(
            Coordinates(floorToDecimal(locations.longitude, accuracy), floorToDecimal(locations.latitude, accuracy)),
            Coordinates(ceilToDecimal(locations.longitude, accuracy), ceilToDecimal(locations.latitude, accuracy))
        )

        data.raw.add(location)
        data.n++
        return data
    }

    private fun floorToDecimal(number: Float, accuracy: Int): Float {
        return floor(number * accuracy) / accuracy
    }

    private fun ceilToDecimal(number: Float, accuracy: Int): Float {
        return ceil(number * accuracy) / accuracy
    }

    private fun prepareForNextParticipant(
        requestHeader: RequestHeader,
        requestOptions: RequestOptions,
        requestData: StepsOptions,
        basicData: BasicData) {
    }



    private fun prepareForNextParticipant(requestHeader: RequestHeader,
                                          requestOptions: RequestOptions,
                                          requestData: WalkOptions,
                                          basicData: BasicData) {
    }

    private fun prepareForNextParticipant(requestHeader: RequestHeader,
                                          requestOptions: RequestOptions,
                                          requestData: LocationOptions,
                                          locationData: LocationData) {
        requestOptions.from = requestOptions.group[0].id
        requestOptions.group.removeAt(0)
        if (requestOptions.group.size == 0) {
            Log.d(logTAG, "Last in group, send results to server")
            // TODO: Send aggregated results to server
            return
        }

        val message = JSONObject()
        message.put("to", requestOptions.group[0].id)
        message.put("time_to_live", 120)

        val data = JSONObject()
        data.put("encryptionKey", null)
        data.put("iv", null)
        data.put("requestHeader", gson.toJson(requestHeader, RequestHeader::class.java))
        data.put("requestOptions", gson.toJson(requestOptions, RequestOptions::class.java))
        data.put("requestData", gson.toJson(requestData, LocationOptions::class.java))

        // TODO: Encrypt
        val dataToEncrypt = gson.toJson(locationData, LocationData::class.java)
        data.put("data", dataToEncrypt)

        message.put("data", data)
        forwardToNextParticipant(message)
    }

    private fun prepareForNextParticipant(requestHeader: RequestHeader,
                                          requestOptions: RequestOptions,
                                          requestData: PresenceOptions,
                                          basicData: BasicData) {
    }

    /**
     * Forward newData to the next participant in the group
     * @param message containing next target and group ids
     */
    private fun forwardToNextParticipant(message: JSONObject) {

        val res = Response.Listener<JSONObject> {
            Log.d(logTAG, "Sent to next participant")
        }

        val err = Response.ErrorListener {
            Log.e(logTAG, "Failed to send next participant")
        }

        val jsonRequest = JsonObjectRequest(Request.Method.POST, pushyURI, message, res, err)
        queue.add(jsonRequest)

        //Start waiting for confirmation
        waitForConfirmation = false
        this.job = GlobalScope.launch {
            // TODO: send to next participant
            delay(120000)
            Log.e(logTAG, "SEND TO SOMEONE ELSE")
        }
    }

    private fun sendConfirmation(to: String) {
        val message = JSONObject()
        message.put("to", to)
        val confirmationData = JSONObject()
        confirmationData.put("confirmation", true)
        message.put("data", confirmationData)
        message.put("time_to_live", 120)

        val res = Response.Listener<JSONObject> {
            Log.d(logTAG, "Send confirmation successfully")
        }

        val err = Response.ErrorListener {
            Log.e(logTAG, "Couldn't send confirmation")
        }

        val jsonRequest = JsonObjectRequest(Request.Method.POST, pushyURI, message, res, err)
        queue.add(jsonRequest)
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