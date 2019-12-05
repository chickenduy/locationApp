package com.chickenduy.locationApp.ui.steps

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.chickenduy.locationApp.data.database.TrackingDatabase
import com.chickenduy.locationApp.data.database.entity.Steps
import com.chickenduy.locationApp.data.repository.StepsRepository

class StepsViewModel(application: Application) : AndroidViewModel(application) {

    private val stepsRepository: StepsRepository
    val allSteps: LiveData<List<Steps>>

    init {
        val stepsDao = TrackingDatabase.getDatabase(application).stepsDao()
        stepsRepository = StepsRepository(stepsDao)
        allSteps = stepsRepository.allsteps
    }
}