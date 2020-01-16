package com.chickenduy.locationApp.data.repository

import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import com.chickenduy.locationApp.data.database.dao.ActivitiesDetailedDao
import com.chickenduy.locationApp.data.database.entity.ActivitiesDetailed

class ActivitiesDetailedRepository(
    private val activitiesDetailedDao: ActivitiesDetailedDao
) {
    val allActivities: LiveData<List<ActivitiesDetailed>> = activitiesDetailedDao.getAll()

    @WorkerThread
    fun insert(activitiesDetailed: ActivitiesDetailed): Long {
        return activitiesDetailedDao.insert(activitiesDetailed)
    }

    @WorkerThread
    fun insert(activitiesDetailed: List<ActivitiesDetailed>) {
        activitiesDetailed.forEach {
            insert(it)
        }
    }

    @WorkerThread
    fun getByID(id: Long): ActivitiesDetailed {
        return activitiesDetailedDao.getById(id)
    }

    @WorkerThread
    fun getByTimestamp(timestamp: Long): List<ActivitiesDetailed> {
        return activitiesDetailedDao.getByTimestamp(timestamp)
    }

    @WorkerThread
    fun get10Recent(): LiveData<List<ActivitiesDetailed>> {
        return activitiesDetailedDao.get10Recent()
    }

    @WorkerThread
    fun getAll(): LiveData<List<ActivitiesDetailed>> {
        return activitiesDetailedDao.getAll()
    }

    @WorkerThread
    fun deleteAll() {
        return activitiesDetailedDao.deleteAll()
    }
}