package com.chickenduy.locationApp.data.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.ABORT
import androidx.room.Query
import com.chickenduy.locationApp.data.database.entity.GPS

@Dao
interface GPSDao {
    @Insert(onConflict = ABORT)
    fun insert(gps: GPS): Long

    @Insert(onConflict = ABORT)
    fun insert(gps: List<GPS>)

    @Query("SELECT * FROM gps_table WHERE id = :id LIMIT 1")
    fun getById(id: Long): GPS

    @Query("SELECT * FROM gps_table WHERE timestamp > :start AND timestamp < :end ORDER BY timestamp DESC")
    fun getByTimestamps(start: Long, end: Long): List<GPS>

    @Query(" SELECT * FROM gps_table ORDER BY ABS(timestamp - :timestamp) ASC LIMIT 1")
    fun getByTimestamp(timestamp: Long): GPS

    @Query("SELECT * FROM gps_table ORDER BY timestamp DESC Limit 10")
    fun get10Recent(): LiveData<List<GPS>>

    @Query("SELECT * FROM gps_table ORDER BY timestamp DESC")
    fun getAll(): LiveData<List<GPS>>

    @Query("DELETE FROM gps_table")
    fun deleteAll()
}