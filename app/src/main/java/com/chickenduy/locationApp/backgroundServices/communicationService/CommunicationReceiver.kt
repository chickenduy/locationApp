package com.chickenduy.locationApp.backgroundServices.communicationService

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Base64
import android.util.Log
import com.chickenduy.locationApp.MyApp
import com.chickenduy.locationApp.backgroundServices.communicationService.model.entity.BasicData
import com.chickenduy.locationApp.backgroundServices.communicationService.model.entity.Location
import com.chickenduy.locationApp.backgroundServices.communicationService.model.request.RequestHeader
import com.chickenduy.locationApp.backgroundServices.communicationService.model.request.RequestOptions
import com.chickenduy.locationApp.backgroundServices.communicationService.model.request.options.LocationOptions
import com.chickenduy.locationApp.backgroundServices.communicationService.model.request.options.PresenceOptions
import com.chickenduy.locationApp.backgroundServices.communicationService.model.request.options.StepsOptions
import com.chickenduy.locationApp.backgroundServices.communicationService.model.request.options.ActivityOptions
import com.chickenduy.locationApp.data.database.TrackingDatabase
import com.chickenduy.locationApp.data.repository.ActivitiesDetailedRepository
import com.chickenduy.locationApp.data.repository.ActivitiesRepository
import com.chickenduy.locationApp.data.repository.GPSRepository
import com.chickenduy.locationApp.data.repository.StepsRepository
import com.google.gson.Gson
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
class CommunicationReceiver : BroadcastReceiver() {

    private val TAG = "COMRECEIVER"
    private val REQUESTHEADER = "requestHeader"
    private val REQUESTOPTIONS = "requestOptions"
    private val REQUESTDATA = "requestData"
    private val DATA = "data"

    private val ctx = MyApp.instance
    private val sharedPref = ctx.getSharedPreferences("options", Context.MODE_PRIVATE)

    private val activitiesRepository: ActivitiesDetailedRepository =
        ActivitiesDetailedRepository(TrackingDatabase.getDatabase(MyApp.instance).activitiesDetailedDao())
    private val gpsRepository: GPSRepository =
        GPSRepository(TrackingDatabase.getDatabase(MyApp.instance).gPSDao())
    private val stepsRepository: StepsRepository =
        StepsRepository(TrackingDatabase.getDatabase(MyApp.instance).stepsDao())

    private val gson = Gson()

    private var communicationHandlerSteps: CommunicationHandler<StepsOptions>? = null
    private var communicationHandlerActivity: CommunicationHandler<ActivityOptions>? = null
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
            Log.d(TAG, "cancel jobs")
            cancelJobs()
            return
        }

        // Receive Aggregation Request
        val encryptionKey = intent.getStringExtra("encryptionKey")
        val iv = intent.getStringExtra("iv")

        val requestHeaderString = intent.getStringExtra(REQUESTHEADER)
        val requestOptionsString = intent.getStringExtra(REQUESTOPTIONS)
        val requestDataString = intent.getStringExtra(REQUESTDATA)
        val decryptedDataString: String?
        if (requestHeaderString.isNullOrEmpty() || requestOptionsString.isNullOrEmpty() || requestDataString.isNullOrEmpty()) {
            Log.e(TAG, "Got wrong request format")
            return
        }

        decryptedDataString = if (encryptionKey != null && iv != null) {
            val data = intent.getStringExtra(DATA)
            val key = decryptKey(encryptionKey)
            val ivSpec = Base64.decode(iv, Base64.NO_WRAP)
            val dataBytes = Base64.decode(data, Base64.NO_WRAP)
            decrypt(key, ivSpec, dataBytes)
        } else {
            intent.getStringExtra(DATA)
        }

        val requestHeader = gson.fromJson(requestHeaderString, RequestHeader::class.java)
        val requestOptions = gson.fromJson(requestOptionsString, RequestOptions::class.java)

        when (requestHeader.type) {
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
            "activity" -> {
                Log.d(TAG, "Starting aggregation for walking")
                val activityOptions = gson.fromJson(requestDataString, ActivityOptions::class.java)
                var basicData = gson.fromJson(decryptedDataString, BasicData::class.java)
                basicData = aggregateActivity(activityOptions, basicData)
                communicationHandlerActivity = CommunicationHandler(
                    requestHeader,
                    requestOptions,
                    activityOptions,
                    basicData
                )
                communicationHandlerActivity!!.send()
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
        val steps = stepsRepository.getByTimestamps(startDate, endDate)
        if(steps.isEmpty()) {
            Log.d(TAG, "missing data")
            return basicData
        }
        val locations = gpsRepository.getByTimestamps(startDate, endDate)
        var inRange = false
        locations.forEach {
            if(inRange(it.lat, it.lon, stepsOptions.lat, stepsOptions.lon, stepsOptions.radius)) {
                inRange = true
                return@forEach
            }
        }
        if(!inRange) {
            Log.d(TAG, "not in desired area")
            return basicData
        }
        var stepsResult = 0
        steps.forEach {
            stepsResult += it.steps
        }
        basicData.raw.add(stepsResult)
        basicData.n++
        return basicData
    }

    /**
     * Start internal aggregation of activity
     */
    private fun aggregateActivity(activityOptions: ActivityOptions, basicData: BasicData): BasicData {
        val startDate = DateTime(activityOptions.start).millis
        val endDate = DateTime(activityOptions.end).millis
        val activities = activitiesRepository.getByTimestamp(startDate, endDate)
        if(activities.isEmpty()) {
            Log.d(TAG, "missing data")
            return basicData
        }
        val locations = gpsRepository.getByTimestamps(startDate, endDate)
        var inRange = false
        locations.forEach {
            if(inRange(it.lat, it.lon, activityOptions.lat, activityOptions.lon, activityOptions.radius)) {
                inRange = true
                return@forEach
            }
        }
        if(!inRange) {
            Log.d(TAG, "not in desired area")
            return basicData
        }
        var time = 0L
        activities.forEach {activity ->
            if(activity.type == activityOptions.type) {
                time += (activity.end-activity.start)
            }
        }
        basicData.n++
        basicData.addRaw(time)
        return basicData
    }

    private fun aggregateLocation(locationOptions: LocationOptions, basicData: BasicData): BasicData {
        val location = gpsRepository.getByTimestamp(locationOptions.date)
        // not in desired time
        if(abs(location.timestamp - locationOptions.date) > 10*60*1000) {
            Log.d(TAG, "location: ${location.timestamp}, locationOptions: ${locationOptions.date}")
            Log.d(TAG, "missing data")
            return basicData
        }
        // in desired radius
        if(inRange(location.lat, location.lon, locationOptions.lat, locationOptions.lon, locationOptions.radius)) {
            val accuracy = 10.0.pow(locationOptions.accuracy).toInt()
            val blCorner = Location(
                floorToDecimal(location.lat, accuracy),
                floorToDecimal(location.lon, accuracy)
            )
            val trCorner = Location(
                ceilToDecimal(location.lat, accuracy),
                ceilToDecimal(location.lon, accuracy)
            )
            Log.d(TAG, "bl: (${blCorner.lat},${blCorner.lon}), tr: (${trCorner.lat},${trCorner.lon}) ")
            val midpoint = haversineMidpoint(blCorner, trCorner)
            val midPointLocation = Location(midpoint.lat, midpoint.lon)
            basicData.n++
            basicData.raw.add(midPointLocation)
            return basicData
        }
        Log.d(TAG, "not in desired area")
        return basicData
    }

    /**
     * Start internal aggregation of activity
     */
    private fun aggregatePresence(presenceOptions: PresenceOptions, basicData: BasicData): BasicData {
        val locations = gpsRepository.getByTimestamps(presenceOptions.start, presenceOptions.end)
        if(locations.isEmpty()) {
            Log.d(TAG, "missing data")
            return basicData
        }
        Log.d(TAG, "Looking for lat: ${presenceOptions.lat}, lon: ${presenceOptions.lon}")
        locations.forEach {
            if(inRange(it.lat, it.lon, presenceOptions.lat, presenceOptions.lon, presenceOptions.radius)) {
                Log.d(TAG, "Found location with lat: ${it.lat}, lon: ${it.lon}")
                basicData.n++
                basicData.addRaw(1)
                return basicData
            }
        }
        Log.d(TAG, "not in desired area")
        basicData.addRaw(0)
        basicData.n++
        return basicData
    }

    private fun floorToDecimal(number: Double, accuracy: Int): Double {
        return floor(number * accuracy) / accuracy
    }

    private fun ceilToDecimal(number: Double, accuracy: Int): Double {
        return ceil(number * accuracy) / accuracy
    }

    private fun inRange(lat1: Double, long1: Double, lat2: Double, long2: Double, range: Double): Boolean {
        return haversineKm(lat1, long1, lat2, long2) <= range
    }

    /**
     * formula from https://www.movable-type.co.uk/scripts/latlong.html
     */
    private fun haversineKm(lat1: Double, long1: Double, lat2: Double, long2: Double): Double {
        val d2r = 0.0174532925199433
        val lon = (long2 - long1) * d2r
        val lat = (lat2 - lat1) * d2r
        val a1 = sin(lat / 2.0) * sin(lat / 2.0)
        val a2 = cos(lat1 * d2r) * cos(lat2 * d2r)
        val a3 = sin(lon / 2.0) * sin(lon / 2.0)
        val a = a1 + a2 * a3
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return 6367 * c
    }

    /**
     * formula from https://stackoverflow.com/questions/4656802/midpoint-between-two-latitude-and-longitude
     */
    private fun haversineMidpoint(blCorner: Location, trCorner: Location): Location {
        val dLon = toRadians(trCorner.lon - blCorner.lon)
        //convert to radians
        val lat1 = toRadians(blCorner.lat)
        val lat2 = toRadians(trCorner.lat)
        val lon1 = toRadians(blCorner.lon)
        val bx = cos(lat2) * cos(dLon)
        val by = cos(lat2) * sin(dLon)
        val lat3 = atan2(
            sin(lat1) + sin(lat2),
            sqrt((cos(lat1) + bx) * (cos(lat1) + bx) + by * by)
        )
        val long3: Double = lon1 + atan2(by, cos(lat1) + bx)
        return Location(toDegrees(lat3), toDegrees(long3))
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
        return keyFact.generatePrivate(keySpec)
    }

    private fun cancelJobs() {
        if (communicationHandlerSteps != null) {
            communicationHandlerSteps!!.cancel()
            communicationHandlerSteps = null
        }
        if (communicationHandlerActivity != null) {
            communicationHandlerActivity!!.cancel()
            communicationHandlerActivity = null
        }
        if (communicationHandlerLocation != null) {
            communicationHandlerLocation!!.cancel()
            communicationHandlerLocation = null
        }
        if (communicationHandlerPresence != null) {
            communicationHandlerPresence!!.cancel()
            communicationHandlerPresence = null
        }
    }
}