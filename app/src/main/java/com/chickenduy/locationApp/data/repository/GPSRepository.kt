package com.chickenduy.locationApp.data.repository

import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import com.chickenduy.locationApp.data.database.dao.GPSDao
import com.chickenduy.locationApp.data.database.entity.GPS

class GPSRepository (
    private val gpsDao: GPSDao
) {
    val allGPS: LiveData<List<GPS>> = gpsDao.getAll()

    @WorkerThread
    suspend fun insert(gps: GPS): Long {
        return gpsDao.insert(gps)
    }

    @WorkerThread
    suspend fun insertAll(gps: List<GPS>) {
        return gpsDao.insertAll(gps)
    }

    @WorkerThread
    suspend fun getByID(timestamp: Long): GPS {
        return gpsDao.getById(timestamp)
    }

    @WorkerThread
    suspend fun getByTimestamp(minTimestamp: Long, maxTimestamp: Long): List<GPS> {
        return gpsDao.getByTimestamp(minTimestamp, maxTimestamp)
    }

    @WorkerThread
    suspend fun deleteAll() {
        return gpsDao.deleteAll()
    }
}