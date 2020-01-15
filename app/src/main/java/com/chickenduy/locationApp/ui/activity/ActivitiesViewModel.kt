package com.chickenduy.locationApp.ui.activity

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.chickenduy.locationApp.data.database.TrackingDatabase
import com.chickenduy.locationApp.data.database.entity.Activities
import com.chickenduy.locationApp.data.database.entity.ActivitiesDetailed
import com.chickenduy.locationApp.data.repository.ActivitiesDetailedRepository
import com.chickenduy.locationApp.data.repository.ActivitiesRepository

class ActivitiesViewModel(application: Application) : AndroidViewModel(application) {
    private val activitiesRepository: ActivitiesRepository
    //val allActivities: LiveData<List<Activities>>
    private val activitiesDetailedRepository: ActivitiesDetailedRepository
    val allActivitiesDetailed: LiveData<List<ActivitiesDetailed>>

    init {
        val activitiesDao = TrackingDatabase.getDatabase(application).activitiesDao()
        activitiesRepository = ActivitiesRepository(activitiesDao)
        //allActivities = activitiesRepository.allActivities
        val activitiesDetailedDao =
            TrackingDatabase.getDatabase(application).activitiesDetailedDao()
        activitiesDetailedRepository = ActivitiesDetailedRepository(activitiesDetailedDao)
        allActivitiesDetailed = activitiesDetailedRepository.allActivities
    }
}