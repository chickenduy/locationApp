package com.chickenduy.locationApp.data.repository

import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import com.chickenduy.locationApp.data.database.dao.StepsDao
import com.chickenduy.locationApp.data.database.entity.Steps

class StepsRepository(
    private val stepsDao: StepsDao
) {
    val allsteps: LiveData<List<Steps>> = getAll()

    @WorkerThread
    fun insert(steps: Steps): Long {
        return stepsDao.insert(steps)
    }

    @WorkerThread
    fun insert(steps: List<Steps>) {
        steps.forEach {
            insert(it)
        }
    }

    @WorkerThread
    fun getByID(id: Long): Steps {
        return stepsDao.getById(id)
    }

    @WorkerThread
    fun  getByTimestamps(minTimestamp: Long, maxTimestamp: Long): List<Steps> {
        return stepsDao.getByTimestamps(minTimestamp, maxTimestamp)
    }

    @WorkerThread
    fun get10Recent(): LiveData<List<Steps>> {
        return stepsDao.get10Recent()
    }

    @WorkerThread
    fun getAll(): LiveData<List<Steps>> {
        return stepsDao.getAll()
    }

    @WorkerThread
    fun deleteAll() {
        return stepsDao.deleteAll()
    }


}