package com.chickenduy.locationApp.backgroundServices.communicationService

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Base64
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
import org.json.JSONObject
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.pow


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
        val decryptedData: String?
        if(requestHeader.isNullOrEmpty() || requestOptions.isNullOrEmpty() || requestData.isNullOrEmpty()) {
            Log.e(logTAG, "Got wrong request format")
            return
        }

        decryptedData = if (encryptionKey != null && iv  != null) {
            val data = intent.getStringExtra("data")
            val key = decryptKey(encryptionKey)
            val ivSpec = Base64.decode(iv, Base64.NO_WRAP)
            val dataBytes = Base64.decode(data, Base64.NO_WRAP)
            decrypt(key, ivSpec, dataBytes)
        } else {
            intent.getStringExtra("data")
        }

        Log.e(logTAG, "Decrypted Data")
        Log.e(logTAG, decryptedData)

        // TODO: Start aggregation
        val requestHeaderObj = gson.fromJson(requestHeader, RequestHeader::class.java)
        val requestOptionsObj = gson.fromJson(requestOptions, RequestOptions::class.java)

        // TODO: Send confirmation
        if(requestOptionsObj.from.isNotEmpty()) {
            sendConfirmation(requestOptionsObj.from)
        }

        when(requestHeaderObj.type) {
            "steps" -> {
                val requestDataObj = gson.fromJson(requestData, StepsOptions::class.java)
                val dataObj = gson.fromJson(decryptedData, BasicData::class.java)
                val newDataObj = aggregateSteps(requestDataObj, dataObj)
                prepareForNextParticipant(requestHeaderObj, requestOptionsObj, requestDataObj, newDataObj)
            }
            "walk" -> {
                Log.d(logTAG, "Starting aggregation for walking")
                val requestDataObj = gson.fromJson(requestData, WalkOptions::class.java)
                val dataObj = gson.fromJson(decryptedData, BasicData::class.java)
                prepareForNextParticipant(requestHeaderObj, requestOptionsObj, requestDataObj, dataObj)
            }
            "location" -> {
                Log.d(logTAG, "Starting aggregation for location")
                val requestDataObj = gson.fromJson(requestData, LocationOptions::class.java)
                val dataObj = gson.fromJson(decryptedData, LocationData::class.java)
                val newDataObj = aggregateLocation(requestDataObj, dataObj)
                prepareForNextParticipant(requestHeaderObj, requestOptionsObj, requestDataObj, newDataObj)
            }
            "presence" -> {
                Log.d(logTAG, "Starting aggregation for presence")
                val requestDataObj = gson.fromJson(requestData, PresenceOptions::class.java)
                val dataObj = gson.fromJson(decryptedData, BasicData::class.java)
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

        val key = generateSecretKey()
        val ivParameterSpec = generateIV()

        val message = JSONObject()
        message.put("to", requestOptions.group[0].id)
        message.put("time_to_live", 120)

        val data = JSONObject()
        data.put("encryptionKey", encryptKey(key.encoded, requestOptions.group[0].publicKey))
        data.put("iv", Base64.encodeToString(ivParameterSpec.iv, Base64.NO_WRAP))
        data.put("requestHeader", gson.toJson(requestHeader, RequestHeader::class.java))
        data.put("requestOptions", gson.toJson(requestOptions, RequestOptions::class.java))
        data.put("requestData", gson.toJson(requestData, LocationOptions::class.java))

        // TODO: Encrypt
        val dataToEncrypt = gson.toJson(locationData, LocationData::class.java)
        val encryptedData = encrypt(key, ivParameterSpec.iv, dataToEncrypt.toByteArray())
        data.put("data", encryptedData)

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

        Log.e(logTAG, message.toString(1))

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

    /**
     * Generate a random 256 bit AES symmetric key
     */
    private fun generateSecretKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(256)
        return keyGenerator.generateKey()
    }

    /**
     * Generate a random 16 Byte Initialization Vector
     */
    private fun generateIV(): IvParameterSpec {
        val ivRandom = SecureRandom()
        val iv = ByteArray(16)
        ivRandom.nextBytes(iv)
        return IvParameterSpec(iv)
    }

    /**
     * Encrypt a String using AES and returns Base64 String
     * @param key used to encrypt
     * @param iv used for encryption
     * @param decrypted String to encrypt
     */
    private fun encrypt(key: SecretKey, iv: ByteArray, decrypted: ByteArray): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
        val ivSpec = IvParameterSpec(iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec)
        val result = cipher.doFinal(decrypted)
        return Base64.encodeToString(result, Base64.NO_WRAP)
    }

    /**
     * Decrypt a ByteArray using AES and returns Base64 String
     * @param key used to decrypt
     * @param iv used for decryption
     * @param encrypted ByteArray to decrypt
     */
    private fun decrypt(key: SecretKey, iv: ByteArray, encrypted: ByteArray): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS7PADDING")
        val ivSpec = IvParameterSpec(iv)
        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec)
        cipher.doFinal(encrypted).toString()
        return String(cipher.doFinal(encrypted))
    }

    /**
     * Encrypt the AES symmetric key using the provided RSA public key
     * @param key to encrypt
     * @param publicKeyString used to encrypt the key
     */
    private fun encryptKey(key: ByteArray, publicKeyString: String): String? {
        val decodedPublicKey = Base64.decode(publicKeyString, Base64.NO_WRAP)
        val x509KeySpec = X509EncodedKeySpec(decodedPublicKey)
        val keyFact = KeyFactory.getInstance("RSA")
        val publicKey = keyFact.generatePublic(x509KeySpec)
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        return Base64.encodeToString(cipher.doFinal(key), Base64.NO_WRAP)
    }

    /**
     * Decrypt the AES symmetric key using the stored RSA private key in shared preferences
     * @param key to decrypt in Base64 String Format
     */
    private fun decryptKey(key: String): SecretKey {
        val encodedKey = Base64.decode(key, Base64.NO_WRAP)
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.DECRYPT_MODE, getPrivateKey())
        val keyBytes = cipher.doFinal(encodedKey)
        return SecretKeySpec(keyBytes, 0, keyBytes.size, "AES")
    }

    /**
     * Retrieve the RSA private key from shared preferences
     */
    private fun getPrivateKey(): PrivateKey {
        val privateBytes = Base64.decode(sharedPref.getString("privateKey", "")!!, Base64.NO_WRAP)
        val keySpec = PKCS8EncodedKeySpec(privateBytes)
        val keyFact = KeyFactory.getInstance("RSA")
        return  keyFact.generatePrivate(keySpec)
    }
}