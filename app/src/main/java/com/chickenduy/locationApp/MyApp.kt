package com.chickenduy.locationApp

import android.app.Application
//import androidx.multidex.MultiDexApplication

class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: MyApp
            private set
    }

}