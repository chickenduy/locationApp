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
import com.chickenduy.locationApp.backgroundServices.communicationService.model.entity.Coordinates
import com.chickenduy.locationApp.backgroundServices.communicationService.model.entity.Location
import com.chickenduy.locationApp.backgroundServices.communicationService.model.message.*
import com.chickenduy.locationApp.backgroundServices.communicationService.model.options.LocationOptions
import com.chickenduy.locationApp.backgroundServices.communicationService.model.options.PresenceOptions
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
import me.pushy.sdk.Pushy
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
import kotlin.math.*


/**
 * This class manages incoming requests and forwarding to next device or to server
 */
class CommunicationReceiver: BroadcastReceiver() {

    private val TAG = "COMRECEIVER"
    private val PASSWORD = "password"
    private val REQUESTHEADER = "requestHeader"
    private val REQUESTOPTIONS = "requestOptions"
    private val REQUESTDATA = "requestData"
    private val DATA = "data"

    private val ctx = MyApp.instance
    private val sharedPref = ctx.getSharedPreferences("options", Context.MODE_PRIVATE)
    private val testURI = "http://10.0.2.2:3000/"
    private val serverURI = "https://locationserver.eu-gb.mybluemix.net/"
    private val pushyURI = "https://api.pushy.me/push?api_key=cfd5f664afd97266ed8ec89ac697b9dcded0afced39635320fc5bfb7a950c705"
    private val queue = Volley.newRequestQueue(this.ctx)
    private val activitiesRepository: ActivitiesRepository = ActivitiesRepository(TrackingDatabase.getDatabase(MyApp.instance).activitiesDao())
    private val gpsRepository: GPSRepository = GPSRepository(TrackingDatabase.getDatabase(MyApp.instance).gPSDao())
    private val stepsRepository: StepsRepository = StepsRepository(TrackingDatabase.getDatabase(MyApp.instance).stepsDao())
    private val gson = Gson()
    private lateinit var job: Job

    private lateinit var requestHeader: RequestHeader
    private lateinit var requestOptions: RequestOptions

    private var locationOptions: LocationOptions? = null
    private var stepsOptions: StepsOptions? = null
    private var walkOptions: WalkOptions? = null
    private var presenceOptions: PresenceOptions? = null
    private var basicData: BasicData? = null

    /**
     * Function called upon receiving Pushy notification
     * @param context application context
     * @param intent intent carries data from push notification
     */
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received Notification")

        flushVariables()

        // Receive Confirmation
        // TODO: OnConfirmation -> cancel sleep thread
        val confirmation = intent.getBooleanExtra("confirmation", false)
        if (confirmation) {
            Log.d(TAG, "cancel timeout")
            job.cancel()
            return
        }

        // Receive Aggregation Request
        val encryptionKey = intent.getStringExtra("encryptionKey")
        val iv = intent.getStringExtra("iv")

        val requestHeader = intent.getStringExtra(REQUESTHEADER)
        val requestOptions = intent.getStringExtra(REQUESTOPTIONS)
        val requestData = intent.getStringExtra(REQUESTDATA)
        val decryptedData: String?
        if(requestHeader.isNullOrEmpty() || requestOptions.isNullOrEmpty() || requestData.isNullOrEmpty()) {
            Log.e(TAG, "Got wrong request format")
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

        // TODO: Start aggregation
        this.requestHeader = gson.fromJson(requestHeader, RequestHeader::class.java)
        this.requestOptions = gson.fromJson(requestOptions, RequestOptions::class.java)

        Log.d(TAG, this.requestOptions.from)

        // TODO: Send confirmation
        if(this.requestOptions.from.isNotEmpty()) {
            sendConfirmation(this.requestOptions.from)
        }

        when(this.requestHeader.type) {
            "steps" -> {
                this.stepsOptions = gson.fromJson(requestData, StepsOptions::class.java)
                this.basicData = gson.fromJson(decryptedData, BasicData::class.java)
                aggregateSteps()
            }
            "walk" -> {
                Log.d(TAG, "Starting aggregation for walking")
                this.walkOptions = gson.fromJson(requestData, WalkOptions::class.java)
                this.basicData = gson.fromJson(decryptedData, BasicData::class.java)
            }
            "location" -> {
                Log.d(TAG, "Starting aggregation for location")
                this.locationOptions = gson.fromJson(requestData, LocationOptions::class.java)
                this.basicData = gson.fromJson(decryptedData, BasicData::class.java)
                aggregateLocation()

            }
            "presence" -> {
                Log.d(TAG, "Starting aggregation for presence")
                this.presenceOptions = gson.fromJson(requestData, PresenceOptions::class.java)
                this.basicData = gson.fromJson(decryptedData, BasicData::class.java)
                aggregatePresence()
            }
            else -> throw Exception("error")
        }
        prepareForNextParticipant()
    }

    /**
     * Start internal aggregation of activity
     */
    private fun aggregateSteps() {
        val date = DateTime(this.stepsOptions!!.date)
        val startDate = date.withTimeAtStartOfDay().millis
        val endDate = date.plusDays(1).withTimeAtStartOfDay().millis
        var steps = 0

        val stepsArray = stepsRepository.getByTimestamp(startDate, endDate)
        stepsArray.forEach {
            steps += it.steps
        }
        this.basicData!!.addRaw(steps)
        // TODO: Aggregate activity into data
    }

    /**
     * Start internal aggregation of activity
     */
    private fun aggregateWalk() {
        val date = DateTime(this.stepsOptions!!.date)
        val startDate = date.withTimeAtStartOfDay().millis
        val endDate = date.plusDays(1).withTimeAtStartOfDay().millis
        var steps = 0

        val stepsArray = stepsRepository.getByTimestamp(startDate, endDate)
        stepsArray.forEach {
            steps += it.steps
        }
        this.basicData!!.addRaw(steps)
        // TODO: Aggregate activity into data
    }

    private fun aggregateLocation() {
        val locations = gpsRepository.getByTimestamp(this.locationOptions!!.timestamp)
        val accuracy = 10.0.pow(this.locationOptions!!.accuracy).toInt()
        val location = Location(
            Coordinates(floorToDecimal(locations.longitude, accuracy), floorToDecimal(locations.latitude, accuracy)),
            Coordinates(ceilToDecimal(locations.longitude, accuracy), ceilToDecimal(locations.latitude, accuracy))
        )
        this.basicData!!.raw.add(location)
        this.basicData!!.n++
    }

    /**
     * Start internal aggregation of activity
     */
    private fun aggregatePresence() {
        val start = DateTime(this.presenceOptions!!.start).millis
        val end = DateTime(this.presenceOptions!!.end).millis
        val locations = gpsRepository.getByTimestamps(start, end)
        Log.d(TAG, "Looking for lat: ${this.presenceOptions!!.lat}, long: ${this.presenceOptions!!.long}")
        for (location in locations) {
            Log.d(TAG, "Saved location with lat: ${location.latitude}, long: ${location.longitude}")
            val distance = haversine_km(this.presenceOptions!!.lat, this.presenceOptions!!.long, location.latitude.toDouble(), location.longitude.toDouble())
            if (distance <= presenceOptions!!.radius) {
                Log.d(TAG, "Distance was $distance")
                this.basicData!!.addRaw(1)
                this.basicData!!.n++
                return
            }
        }
        this.basicData!!.addRaw(0)
        this.basicData!!.n++
        // TODO: Aggregate activity into data
    }

    private fun floorToDecimal(number: Float, accuracy: Int): Float {
        return floor(number * accuracy) / accuracy
    }

    private fun ceilToDecimal(number: Float, accuracy: Int): Float {
        return ceil(number * accuracy) / accuracy
    }

    private fun prepareForNextParticipant() {

        this.requestOptions.from = Pushy.getDeviceCredentials(ctx).token
        this.requestOptions.group.removeAt(0)

        if (this.requestOptions.group.size == 0) {
            Log.d(TAG, "Last in group, send results to server")
            sendToServer()
            return
        }

        val key = generateSecretKey()
        val ivParameterSpec = generateIV()

        val dataToEncrypt = gson.toJson(basicData, BasicData::class.java)
        val encryptedData = encrypt(key, ivParameterSpec.iv, dataToEncrypt.toByteArray())

        forwardToNextParticipant(
            encryptKey(key.encoded, requestOptions.group[0].publicKey)!!,
            Base64.encodeToString(ivParameterSpec.iv, Base64.NO_WRAP),
            encryptedData
        )
    }

    /**
     * Forward newData to the next participant in the group
     * @param m containing next target and group ids
     */
    private fun forwardToNextParticipant(key: String, iv: String, encrypted: String) {

        val res = Response.Listener<JSONObject> {
            Log.d(TAG, "Sent to next participant")
        }

        val err = Response.ErrorListener {
            Log.e(TAG, "Failed to send next participant")
        }

        val d = when(this.requestHeader.type) {
            "steps" -> Data(key, iv, this.requestHeader, this.requestOptions, this.stepsOptions, this.basicData)
            "walk" -> Data(key, iv, this.requestHeader, this.requestOptions, this.walkOptions, this.basicData)
            "location" -> Data(key, iv, this.requestHeader, this.requestOptions, this.locationOptions, this.basicData)
            "presence" -> Data(key, iv, this.requestHeader, this.requestOptions, this.presenceOptions, this.basicData)
            else -> throw Exception("error")
        }
        val m = Message(this.requestOptions.group[0].id, 120, d)

        val message = JSONObject(gson.toJson(m, Message::class.java))
        val jsonRequest = JsonObjectRequest(Request.Method.POST, pushyURI, message, res, err)
        queue.add(jsonRequest)

        //Start waiting for confirmation
        this.job = GlobalScope.launch {
            // TODO: send to next participant
            delay(150000)
            prepareForNextParticipant()
        }
    }

    private fun sendToServer() {
        val res = Response.Listener<JSONObject> {
            Log.d(TAG, "Send result successfully")
        }

        val err = Response.ErrorListener {
            Log.e(TAG, "Couldn't send result")
        }

        val m = when(this.requestHeader.type) {
            "steps" ->  ServerMessage(
                sharedPref.getString(PASSWORD, "")!!,
                this.requestHeader,
                this.requestOptions,
                this.stepsOptions!!,
                this.basicData!!
            )
            "walk" ->  ServerMessage(
                sharedPref.getString(PASSWORD, "")!!,
                this.requestHeader,
                this.requestOptions,
                this.walkOptions!!,
                this.basicData!!
            )
            "location" ->  ServerMessage(
                sharedPref.getString(PASSWORD, "")!!,
                this.requestHeader,
                this.requestOptions,
                this.locationOptions!!,
                this.basicData!!
            )
            "presence" ->  ServerMessage(
                sharedPref.getString(PASSWORD, "")!!,
                this.requestHeader,
                this.requestOptions,
                this.presenceOptions!!,
                this.basicData!!
            )
            else -> throw Exception("test")
        }
        val message = JSONObject(gson.toJson(m, ServerMessage::class.java))
        val jsonRequest = JsonObjectRequest(Request.Method.POST, testURI + "aggregation${requestHeader.type}", message, res, err)
        queue.add(jsonRequest)
    }

    private fun sendConfirmation(to: String) {

        val res = Response.Listener<JSONObject> {
            Log.d(TAG, "Send confirmation successfully")
        }

        val err = Response.ErrorListener {
            Log.e(TAG, "Couldn't send confirmation")
        }

        val confirmationData = JSONObject()
        confirmationData.put("confirmation", true)
        val m = PushyMessage(to, 120, confirmationData)
        val message = JSONObject(gson.toJson(m, PushyMessage::class.java))
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

    private fun flushVariables() {
        this.basicData = null
        this.presenceOptions = null
        this.stepsOptions = null
        this.walkOptions = null
    }

    private fun haversine_km(lat1: Double, long1: Double, lat2: Double, long2: Double): Double {
        val d2r = 0.0174532925199433
        val long = (long2 - long1) * d2r
        val lat = (lat2 - lat1) * d2r

        val a1 = sin(lat / 2.0) * sin(lat / 2.0)
        val a2 = cos(lat1 * d2r) * cos(lat2 * d2r)
        val a3 = sin(long / 2.0) * sin(long / 2.0)
        val a = a1 + a2 * a3
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return 6367 * c
    }
}