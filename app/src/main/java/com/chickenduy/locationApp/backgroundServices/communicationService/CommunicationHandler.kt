package com.chickenduy.locationApp.backgroundServices.communicationService

import android.content.Context
import android.util.Base64
import android.util.Log
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.chickenduy.locationApp.MyApp
import com.chickenduy.locationApp.backgroundServices.communicationService.model.entity.BasicData
import com.chickenduy.locationApp.backgroundServices.communicationService.model.message.PushyData
import com.chickenduy.locationApp.backgroundServices.communicationService.model.message.PushyMessage
import com.chickenduy.locationApp.backgroundServices.communicationService.model.message.ServerMessage
import com.chickenduy.locationApp.backgroundServices.communicationService.model.request.RequestHeader
import com.chickenduy.locationApp.backgroundServices.communicationService.model.request.RequestOptions
import com.google.gson.Gson
import kotlinx.coroutines.*
import org.json.JSONObject
import java.security.KeyFactory
import java.security.SecureRandom
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

class CommunicationHandler<T>(
    private val requestHeader: RequestHeader,
    private val requestOptions: RequestOptions,
    private val requestData: T,
    private val basicData: BasicData
) {
    private val TAG = "COMHANDLER"
    private val PASSWORD = "password"

    private val ctx = MyApp.instance
    private val sharedPref = ctx.getSharedPreferences("options", Context.MODE_PRIVATE)

    private val testURI = "http://10.0.2.2:3000/"
    /**
     * Add URL for the server
     */
    private val serverURI = ""
    /**
     * Add URL for the push service (Pushy.me)
     */
    private val pushyURI = ""
    private val queue = Volley.newRequestQueue(this.ctx)

    private lateinit var job: Thread
    private val gson = Gson()

    fun send() {
        prepareForNextParticipant()
    }

    fun cancel() {
        job.interrupt()
    }

    private fun prepareForNextParticipant() {
        val from = this.requestOptions.from
        this.requestOptions.from = this.requestOptions.group[0].id
        this.requestOptions.group.removeAt(0)

        if (this.requestOptions.group.size == 0) {
            Log.d(TAG, "Last in group, send results to server")
            sendToServer(from)
            return
        }

        val key = generateSecretKey()
        val ivParameterSpec = generateIV()

        val dataToEncrypt = gson.toJson(basicData, BasicData::class.java)
        val encryptedData = encrypt(key, ivParameterSpec.iv, dataToEncrypt.toByteArray())

        forwardToNextParticipant(
            encryptKey(key.encoded, requestOptions.group[0].publicKey)!!,
            Base64.encodeToString(ivParameterSpec.iv, Base64.NO_WRAP),
            encryptedData,
            from
        )
    }

    /**
     * Forward newData to the next participant in the group
     * @param key encryption key for encrypted data
     * @param iv iv for AES encrypted data
     * @param encrypted data
     * @param from previous participant
     */
    private fun forwardToNextParticipant(key: String, iv: String, encrypted: String, from: String) {

        val res = Response.Listener<JSONObject> {
            Log.d(TAG, "Sent to next participant")
            sendConfirmation(from)
        }

        val err = Response.ErrorListener {
            Log.e(TAG, "Failed to send next participant")
        }

        val d = when (this.requestHeader.type) {
            "steps" -> PushyData(
                key,
                iv,
                this.requestHeader,
                this.requestOptions,
                this.requestData,
                encrypted
            )
            "activity" -> PushyData(
                key,
                iv,
                this.requestHeader,
                this.requestOptions,
                this.requestData,
                encrypted
            )
            "location" -> PushyData(
                key,
                iv,
                this.requestHeader,
                this.requestOptions,
                this.requestData,
                encrypted
            )
            "presence" -> PushyData(
                key,
                iv,
                this.requestHeader,
                this.requestOptions,
                this.requestData,
                encrypted
            )
            else -> throw Exception("error")
        }
        val m = PushyMessage(this.requestOptions.group[0].id, 120, d)

        val message = JSONObject(gson.toJson(m, PushyMessage::class.java))
        Log.e(TAG, message.toString(2))
        val jsonRequest = JsonObjectRequest(
            Request.Method.POST,
            pushyURI,
            message,
            res,
            err
        )
        jsonRequest.setShouldCache(false)
        queue.add(jsonRequest)

        //Start waiting for confirmation
        this.job = Thread(Runnable {
            try {
                Thread.sleep(3*60*1000)
                prepareForNextParticipant()
            }
            catch (e: InterruptedException) {
                Log.d(TAG, "Cancelled skip participant")
            }
        })
        this.job.start()

    }

    private fun sendToServer(from: String) {
        val res = Response.Listener<JSONObject> {
            Log.d(TAG, "Send result successfully")
            Log.d(TAG, it.toString(2))
            sendConfirmation(from)
        }

        val err = Response.ErrorListener {
            Log.e(TAG, "Couldn't send result")
        }

        val m = when (this.requestHeader.type) {
            "steps" -> ServerMessage(
                sharedPref.getString(PASSWORD, "")!!,
                this.requestHeader,
                this.requestOptions,
                this.requestData,
                this.basicData
            )
            "activity" -> ServerMessage(
                sharedPref.getString(PASSWORD, "")!!,
                this.requestHeader,
                this.requestOptions,
                this.requestData,
                this.basicData
            )
            "location" -> ServerMessage(
                sharedPref.getString(PASSWORD, "")!!,
                this.requestHeader,
                this.requestOptions,
                this.requestData,
                this.basicData
            )
            "presence" -> ServerMessage(
                sharedPref.getString(PASSWORD, "")!!,
                this.requestHeader,
                this.requestOptions,
                this.requestData,
                this.basicData
            )
            else -> throw Exception("test")
        }
        val message = JSONObject(gson.toJson(m, ServerMessage::class.java))
        Log.d(TAG, message.toString(2))
        val jsonRequest = JsonObjectRequest(
            Request.Method.POST,
            serverURI + "aggregation${requestHeader.type}",
            message,
            res,
            err
        )
        jsonRequest.setShouldCache(false)
        queue.add(jsonRequest)
    }

    private fun sendConfirmation(to: String) {
        if (to.isEmpty()) {
            Log.d(TAG, "first target for aggregation")
            return
        }

        val res = Response.Listener<JSONObject> {
            Log.d(TAG, "Send confirmation successfully")
        }

        val err = Response.ErrorListener {
            Log.e(TAG, "Couldn't send confirmation")
        }

        val confirmationData = JSONObject()
        confirmationData.put("confirmation", "true")
        val message = JSONObject()
        message.put("to", to)
        message.put("time_to_live", 120)
        message.put("data", confirmationData)
        val jsonRequest = JsonObjectRequest(
            Request.Method.POST,
            pushyURI,
            message,
            res,
            err
        )
        jsonRequest.setShouldCache(false)
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

}
