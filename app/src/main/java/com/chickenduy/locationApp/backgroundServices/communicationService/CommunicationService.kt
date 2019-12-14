package com.chickenduy.locationApp.backgroundServices.communicationService

import android.content.Context
import android.util.Log
import com.chickenduy.locationApp.BuildConfig
import com.chickenduy.locationApp.MyApp
import io.textile.ipfslite.Peer
import java.io.File


class CommunicationService {
    private val TAG = "COMSERVICE"
    private lateinit var communicationReceiver: CommunicationReceiver
    private lateinit var litePeer: Peer


    init {
        initTextile()
    }

    private fun initTextile() {
        try {

            val ctx: Context = MyApp.instance
            val filesDir = ctx.filesDir
            val path = File(filesDir, "ipfslite").absolutePath
            litePeer = Peer(path, BuildConfig.DEBUG)

            Log.d(TAG, litePeer.started().toString())

            communicationReceiver = CommunicationReceiver()

        } catch (e: Exception) {
            Log.e(TAG, "$e")
        }
    }


    fun addToThread(account: String) {
    }


}
