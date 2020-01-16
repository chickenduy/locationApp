package com.chickenduy.locationApp.data.repository

import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import com.chickenduy.locationApp.data.database.dao.ActivitiesDao
import com.chickenduy.locationApp.data.database.entity.Activities

class ActivitiesRepository(
    private val activitiesDao: ActivitiesDao
) {
    val allActivities: LiveData<List<Activities>> = activitiesDao.getAll()

    @WorkerThread
    fun insert(activities: Activities): Long {
        return activitiesDao.insert(activities)
    }

    @WorkerThread
    fun insert(activities: List<Activities>) {
        activities.forEach {
            insert(it)
        }
    }

    @WorkerThread
    fun getByID(id: Long): Activities {
        return activitiesDao.getById(id)
    }

    @WorkerThread
    fun getByTimestamp(minTimestamp: Long, maxTimestamp: Long): List<Activities> {
        return activitiesDao.getByTimestamp(minTimestamp, maxTimestamp)
    }

    @WorkerThread
    fun get10Recent(): LiveData<List<Activities>> {
        return activitiesDao.get10Recent()
    }

    @WorkerThread
    fun getLatestEntered(): Activities {
        return activitiesDao.getLatestEntered()
    }

    @WorkerThread
    fun getAll(): LiveData<List<Activities>> {
        return activitiesDao.getAll()
    }

    @WorkerThread
    fun deleteAll() {
        return activitiesDao.deleteAll()
    }
}