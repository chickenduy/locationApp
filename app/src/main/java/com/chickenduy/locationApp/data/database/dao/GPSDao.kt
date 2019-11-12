package com.chickenduy.locationApp.data.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.ABORT
import androidx.room.Query
import com.chickenduy.locationApp.data.database.entity.GPS
import java.util.Date

@Dao
interface GPSDao {
    @Insert(onConflict = ABORT)
    suspend fun insert(gps: GPS): Long

    @Insert(onConflict = ABORT)
    suspend fun insertAll(gps: List<GPS>)

    @Query("SELECT * FROM gps_table WHERE timestamp = :timestamp LIMIT 1")
    suspend fun getById(timestamp: Long): GPS

    @Query("SELECT * FROM gps_table WHERE timestamp > :minTimestamp AND timestamp < :maxTimestamp ORDER BY timestamp DESC")
    suspend fun getByTimestamp(minTimestamp: Long, maxTimestamp: Long): List<GPS>

    @Query("SELECT * FROM gps_table  ORDER BY timestamp DESC")
    fun getAll(): LiveData<List<GPS>>

    @Query("DELETE FROM gps_table")
    suspend fun deleteAll()
}