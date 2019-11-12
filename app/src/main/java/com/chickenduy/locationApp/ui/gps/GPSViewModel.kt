package com.chickenduy.locationApp.ui.gps

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.chickenduy.locationApp.data.database.TrackingDatabase
import com.chickenduy.locationApp.data.database.entity.GPS
import com.chickenduy.locationApp.data.repository.GPSRepository
import kotlinx.coroutines.launch

class GPSViewModel(application: Application) : AndroidViewModel(application) {

    private val gpsRepository: GPSRepository
    val allGPS: LiveData<List<GPS>>

    init {
        val gPSDao = TrackingDatabase.getDatabase(application).gPSDao()
        gpsRepository = GPSRepository(gPSDao)
        allGPS = gpsRepository.allGPS
    }
}