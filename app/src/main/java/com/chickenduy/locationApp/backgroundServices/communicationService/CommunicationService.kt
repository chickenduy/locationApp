package com.chickenduy.locationApp.backgroundServices.communicationService

import android.content.Context
import com.chickenduy.locationApp.MyApp
import io.textile.pb.Model.Thread
import io.textile.pb.View
import io.textile.textile.Textile
import java.io.File


class CommunicationService {
    private val TAG = "COMSERVICE"
    private lateinit var communicationReceiver: CommunicationReceiver

    init {
        initTextile()
    }

    private fun initTextile() {
        try {
            communicationReceiver = CommunicationReceiver()

            val ctx: Context = MyApp.instance
            val filesDir: File = ctx.filesDir
            val path = File(filesDir, "textile-repo").absolutePath
            if (!Textile.isInitialized(path)) {
                val phrase =
                    Textile.initializeCreatingNewWalletAndAccount(
                        path,
                        true,
                        false
                    )
                println(phrase)
            }
            Textile.launch(ctx, path, true)
            Textile.instance().addEventListener(communicationReceiver)
        } catch (e: java.lang.Exception) {
            println(e.message)
        }
    }

    fun createThread() {
        val schema = View.AddThreadConfig.Schema.newBuilder()
            .setPreset(View.AddThreadConfig.Schema.Preset.BLOB)
            .build()

        val config = View.AddThreadConfig.newBuilder()
            .setKey("your.bundle.id.version.Basic")
            .setName("Basic")
            .setSchema(schema)
            .setType(Thread.Type.PRIVATE)
            .setSharing(Thread.Sharing.NOT_SHARED)
            .build()
        Textile.instance().threads.add(config)
    }


    fun addToThread(account: String) {
    }


}