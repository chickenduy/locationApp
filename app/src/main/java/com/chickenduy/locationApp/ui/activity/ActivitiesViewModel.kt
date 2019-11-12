package com.chickenduy.locationApp.ui.activity

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.chickenduy.locationApp.data.database.TrackingDatabase
import com.chickenduy.locationApp.data.database.entity.Activities
import com.chickenduy.locationApp.data.database.entity.GPS
import com.chickenduy.locationApp.data.repository.ActivitiesRepository
import com.chickenduy.locationApp.data.repository.GPSRepository
import kotlinx.coroutines.launch

class ActivitiesViewModel(application: Application) : AndroidViewModel(application) {


    private val activitiesRepository: ActivitiesRepository
    val allActivities: LiveData<List<Activities>>

    init {
        val activitiesDao = TrackingDatabase.getDatabase(application).activitiesDao()
        activitiesRepository = ActivitiesRepository(activitiesDao)
        allActivities = activitiesRepository.allActivities
    }
}