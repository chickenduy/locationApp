package com.chickenduy.locationApp.data.repository

import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import com.chickenduy.locationApp.data.database.dao.StepsDao
import com.chickenduy.locationApp.data.database.entity.GPS
import com.chickenduy.locationApp.data.database.entity.Steps
import java.util.*

class StepsRepository (
    private val stepsDao: StepsDao
) {
    val allsteps: LiveData<List<Steps>> = getAll()

    @WorkerThread
    suspend fun insert(steps: Steps): Long {
        return stepsDao.insert(steps)
    }

    @WorkerThread
    suspend fun insert(steps: List<Steps>) {
        steps.forEach {
            insert(it)
        }
    }

    @WorkerThread
    suspend fun getByID(id: Long): Steps {
        return stepsDao.getById(id)
    }

    @WorkerThread
    fun getByTimestamp(minTimestamp: Long, maxTimestamp: Long): List<Steps> {
        return stepsDao.getByTimestamp(minTimestamp, maxTimestamp)
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
    suspend fun deleteAll() {
        return stepsDao.deleteAll()
    }


}