package com.chickenduy.locationApp.backgroundServices.communicationService

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Base64
import android.util.Log
import com.chickenduy.locationApp.MyApp
import com.chickenduy.locationApp.backgroundServices.communicationService.model.entity.BasicData
import com.chickenduy.locationApp.backgroundServices.communicationService.model.entity.Location
import com.chickenduy.locationApp.backgroundServices.communicationService.model.request.options.LocationOptions
import com.chickenduy.locationApp.backgroundServices.communicationService.model.request.options.PresenceOptions
import com.chickenduy.locationApp.backgroundServices.communicationService.model.request.options.StepsOptions
import com.chickenduy.locationApp.backgroundServices.communicationService.model.request.options.WalkOptions
import com.chickenduy.locationApp.backgroundServices.communicationService.model.request.RequestHeader
import com.chickenduy.locationApp.backgroundServices.communicationService.model.request.RequestOptions
import com.chickenduy.locationApp.data.database.TrackingDatabase
import com.chickenduy.locationApp.data.repository.ActivitiesRepository
import com.chickenduy.locationApp.data.repository.GPSRepository
import com.chickenduy.locationApp.data.repository.StepsRepository
import com.google.gson.Gson
import kotlinx.coroutines.Job
import org.joda.time.DateTime
import java.lang.Math.toDegrees
import java.lang.Math.toRadians
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import javax.crypto.Cipher
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

    private val ctx = MyApp.instance
    private val sharedPref = ctx.getSharedPreferences("options", Context.MODE_PRIVATE)

    private val activitiesRepository: ActivitiesRepository = ActivitiesRepository(TrackingDatabase.getDatabase(MyApp.instance).activitiesDao())
    private val gpsRepository: GPSRepository = GPSRepository(TrackingDatabase.getDatabase(MyApp.instance).gPSDao())
    private val stepsRepository: StepsRepository = StepsRepository(TrackingDatabase.getDatabase(MyApp.instance).stepsDao())

    private val gson = Gson()

    private var communicationHandlerSteps: CommunicationHandler<StepsOptions>? = null
    private var communicationHandlerWalk: CommunicationHandler<WalkOptions>? = null
    private var communicationHandlerLocation: CommunicationHandler<LocationOptions>? = null
    private var communicationHandlerPresence: CommunicationHandler<PresenceOptions>? = null

    /**
     * Function called upon receiving Pushy notification
     * @param context application context
     * @param intent intent carries data from push notification
     */
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received Notification")

        // Receive Confirmation and cancel timeout
        val confirmation = intent.getStringExtra("confirmation")
        if (!confirmation.isNullOrEmpty()) {
            Log.d(TAG, "cancel timeout")
            if(communicationHandlerSteps != null) {
                communicationHandlerSteps!!.cancel()
            }
            if(communicationHandlerWalk != null) {
                communicationHandlerWalk!!.cancel()
            }
            if(communicationHandlerLocation != null) {
                communicationHandlerLocation!!.cancel()
            }
            if(communicationHandlerPresence != null) {
                communicationHandlerPresence!!.cancel()
            }
            return
        }

        // Receive Aggregation Request
        val encryptionKey = intent.getStringExtra("encryptionKey")
        val iv = intent.getStringExtra("iv")

        val requestHeaderString = intent.getStringExtra(REQUESTHEADER)
        val requestOptionsString = intent.getStringExtra(REQUESTOPTIONS)
        val requestDataString = intent.getStringExtra(REQUESTDATA)
        val decryptedDataString: String?
        if(requestHeaderString.isNullOrEmpty() || requestOptionsString.isNullOrEmpty() || requestDataString.isNullOrEmpty()) {
            Log.e(TAG, "Got wrong request format")
            return
        }

        decryptedDataString = if (encryptionKey != null && iv  != null) {
            val data = intent.getStringExtra("data")
            val key = decryptKey(encryptionKey)
            val ivSpec = Base64.decode(iv, Base64.NO_WRAP)
            val dataBytes = Base64.decode(data, Base64.NO_WRAP)
            decrypt(key, ivSpec, dataBytes)
        } else {
            intent.getStringExtra("data")
        }

        val requestHeader = gson.fromJson(requestHeaderString, RequestHeader::class.java)
        val requestOptions = gson.fromJson(requestOptionsString, RequestOptions::class.java)

        when(requestHeader.type) {
            "steps" -> {
                val stepsOptions = gson.fromJson(requestDataString, StepsOptions::class.java)
                var basicData = gson.fromJson(decryptedDataString, BasicData::class.java)
                basicData = aggregateSteps(stepsOptions, basicData)
                communicationHandlerSteps = CommunicationHandler(
                    requestHeader,
                    requestOptions,
                    stepsOptions,
                    basicData
                )
                communicationHandlerSteps!!.send()
            }
            "walk" -> {
                Log.d(TAG, "Starting aggregation for walking")
                val walkOptions = gson.fromJson(requestDataString, WalkOptions::class.java)
                var basicData = gson.fromJson(decryptedDataString, BasicData::class.java)
                basicData = aggregateWalk(walkOptions, basicData)
                communicationHandlerWalk = CommunicationHandler(
                    requestHeader,
                    requestOptions,
                    walkOptions,
                    basicData
                )
                communicationHandlerWalk!!.send()
            }
            "location" -> {
                Log.d(TAG, "Starting aggregation for location")
                val locationOptions = gson.fromJson(requestDataString, LocationOptions::class.java)
                var basicData = gson.fromJson(decryptedDataString, BasicData::class.java)
                basicData = aggregateLocation(locationOptions, basicData)
                communicationHandlerLocation = CommunicationHandler(
                    requestHeader,
                    requestOptions,
                    locationOptions,
                    basicData
                )
                communicationHandlerLocation!!.send()

            }
            "presence" -> {
                Log.d(TAG, "Starting aggregation for presence")
                val presenceOptions = gson.fromJson(requestDataString, PresenceOptions::class.java)
                var basicData = gson.fromJson(decryptedDataString, BasicData::class.java)
                basicData = aggregatePresence(presenceOptions, basicData)
                communicationHandlerPresence = CommunicationHandler(
                    requestHeader,
                    requestOptions,
                    presenceOptions,
                    basicData
                )
                communicationHandlerPresence!!.send()

            }
            else -> throw Exception("error")
        }
        //prepareForNextParticipant()
    }

    /**
     * Start internal aggregation of activity
     */
    private fun aggregateSteps(stepsOptions: StepsOptions, basicData: BasicData): BasicData {
        val date = DateTime(stepsOptions.date)
        val startDate = date.withTimeAtStartOfDay().millis
        val endDate = date.plusDays(1).withTimeAtStartOfDay().millis
        var steps = 0
        val stepsArray = stepsRepository.getByTimestamp(startDate, endDate)
        stepsArray.forEach {
            steps += it.steps
        }
        basicData.raw.add(steps)
        basicData.n++
        return basicData
    }

    /**
     * Start internal aggregation of activity
     */
    private fun aggregateWalk(walkOptions: WalkOptions, basicData: BasicData): BasicData {
        val startDate = DateTime(walkOptions.start).millis
        val endDate = DateTime(walkOptions.end).millis
        return basicData
    }

    private fun aggregateLocation(locationOptions: LocationOptions, basicData: BasicData): BasicData {
        val locations = gpsRepository.getByTimestamp(locationOptions.date)
        if(abs(locations.timestamp - locationOptions.date) < 120000) {
            val accuracy = 10.0.pow(locationOptions.accuracy).toInt()
            val blCorner = Location(floorToDecimal(locations.longitude, accuracy), floorToDecimal(locations.latitude, accuracy))
            val trCorner = Location(ceilToDecimal(locations.longitude, accuracy), ceilToDecimal(locations.latitude, accuracy))
            val midpoint = haversineMidpoint(blCorner, trCorner)
            val location = Location(midpoint.lat, midpoint.long)
            basicData.raw.add(location)
        }
        basicData.n++
        return basicData
    }

    /**
     * Start internal aggregation of activity
     */
    private fun aggregatePresence(presenceOptions: PresenceOptions, basicData: BasicData): BasicData {
        val start = DateTime(presenceOptions.start).millis
        val end = DateTime(presenceOptions.end).millis
        val locations = gpsRepository.getByTimestamps(start, end)
        Log.d(TAG, "Looking for lat: ${presenceOptions.lat}, long: ${presenceOptions.long}")
        Log.d(TAG, locations.toString())
        for (location in locations) {
            Log.d(TAG, "Saved location with lat: ${location.latitude}, long: ${location.longitude}")
            val distance = haversineKm(presenceOptions.lat, presenceOptions.long, location.latitude.toDouble(), location.longitude.toDouble())
            if (distance <= presenceOptions.radius) {
                Log.d(TAG, "Distance was $distance")
                basicData.addRaw(1)
                basicData.n++
                return basicData
            }
        }
        basicData.addRaw(0)
        basicData.n++
        return basicData
    }

    private fun floorToDecimal(number: Float, accuracy: Int): Float {
        return floor(number * accuracy) / accuracy
    }

    private fun ceilToDecimal(number: Float, accuracy: Int): Float {
        return ceil(number * accuracy) / accuracy
    }

    /**
     * formula from https://www.movable-type.co.uk/scripts/latlong.html
     */
    private fun haversineKm(lat1: Double, long1: Double, lat2: Double, long2: Double): Double {
        val d2r = 0.0174532925199433
        val long = (long2 - long1) * d2r
        val lat = (lat2 - lat1) * d2r
        val a1 = sin(lat / 2.0) *sin(lat / 2.0)
        val a2 = cos(lat1 * d2r) *cos(lat2 * d2r)
        val a3 = sin(long / 2.0) *sin(long / 2.0)
        val a = a1 + a2 * a3
        val c = 2 *atan2(sqrt(a),sqrt(1 - a))
        return 6367 * c
    }

    /**
     * formula from https://stackoverflow.com/questions/4656802/midpoint-between-two-latitude-and-longitude
     */
    private fun haversineMidpoint(blCorner: Location, trCorner: Location): Location {
        val dLon = toRadians(trCorner.long.toDouble() - blCorner.long.toDouble())
        //convert to radians
        val lat1 = toRadians(blCorner.lat.toDouble())
        val lat2 = toRadians(trCorner.lat.toDouble())
        val lon1 = toRadians(blCorner.long.toDouble())
        val bx = cos(lat2) * cos(dLon)
        val by = cos(lat2) * sin(dLon)
        val lat3 = atan2(
            sin(lat1) + sin(lat2),
            sqrt((cos(lat1) + bx) * (cos(lat1) + bx) + by * by)
        )
        val long3: Double = lon1 + atan2(by, cos(lat1) + bx)
        return Location(toDegrees(lat3).toFloat(), toDegrees(long3).toFloat())
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