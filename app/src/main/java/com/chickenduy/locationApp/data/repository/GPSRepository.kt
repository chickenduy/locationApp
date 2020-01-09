package com.chickenduy.locationApp.data.repository

import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import com.chickenduy.locationApp.data.database.dao.GPSDao
import com.chickenduy.locationApp.data.database.entity.GPS

class GPSRepository (
    private val gpsDao: GPSDao
) {
    val allGPS: LiveData<List<GPS>> = getAll()

    @WorkerThread
    suspend fun insert(gps: GPS): Long {
        return gpsDao.insert(gps)
    }

    @WorkerThread
    suspend fun insert(gps: List<GPS>) {
        gps.forEach {
            insert(it)
        }
    }

    @WorkerThread
    suspend fun getByID(id: Long): GPS {
        return gpsDao.getById(id)
    }

    @WorkerThread
    fun getByTimestamps(minTimestamp: Long, maxTimestamp: Long): List<GPS> {
        return gpsDao.getByTimestamps(minTimestamp, maxTimestamp)
    }

    @WorkerThread
    fun getByTimestamp(timestamp: Long): GPS {
        return gpsDao.getByTimestamp(timestamp)
    }

    @WorkerThread
    fun get10Recent(): LiveData<List<GPS>> {
        return gpsDao.get10Recent()
    }

    @WorkerThread
    fun getAll(): LiveData<List<GPS>> {
        return gpsDao.getAll()
    }

    @WorkerThread
    suspend fun deleteAll() {
        return gpsDao.deleteAll()
    }


}