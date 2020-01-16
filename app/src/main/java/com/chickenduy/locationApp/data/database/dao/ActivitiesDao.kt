package com.chickenduy.locationApp.data.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.ABORT
import androidx.room.Query
import com.chickenduy.locationApp.data.database.entity.Activities

@Dao
interface ActivitiesDao {
    @Insert(onConflict = ABORT)
    fun insert(activities: Activities): Long

    @Insert(onConflict = ABORT)
    fun insert(activities: List<Activities>)

    @Query("SELECT * FROM activities_table WHERE id = :id LIMIT 1")
    fun getById(id: Long): Activities

    @Query("SELECT * FROM activities_table WHERE timestamp > :minTimestamp AND timestamp < :maxTimestamp")
    fun getByTimestamp(minTimestamp: Long, maxTimestamp: Long): List<Activities>

    @Query("SELECT * FROM activities_table")
    fun getAll(): LiveData<List<Activities>>

    @Query("SELECT * FROM activities_table ORDER BY timestamp ASC LIMIT 10")
    fun get10Recent(): LiveData<List<Activities>>

    @Query("SELECT * FROM activities_table WHERE enter = 0 ORDER BY timestamp DESC LIMIT 1")
    fun getLatestEntered(): Activities

    @Query("DELETE FROM activities_table")
    fun deleteAll()
}