package com.chickenduy.locationApp.data.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.ABORT
import androidx.room.Query
import com.chickenduy.locationApp.data.database.entity.Activities
import com.google.android.gms.location.ActivityTransition

@Dao
interface ActivitiesDao {
    @Insert(onConflict = ABORT)
    suspend fun insert(activities: Activities): Long

    @Insert(onConflict = ABORT)
    suspend fun insertAll(gps: List<Activities>)

    @Query("SELECT * FROM activities_table WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Activities

    @Query("SELECT * FROM activities_table")
    fun getAll(): LiveData<List<Activities>>

    @Query("SELECT * FROM activities_table WHERE timestamp > :minTimestamp AND timestamp < :maxTimestamp")
    suspend fun getByTimestamp(minTimestamp: Long, maxTimestamp: Long): List<Activities>

    @Query("SELECT * FROM activities_table ORDER BY timestamp DESC LIMIT 10")
    fun get10Recent(): LiveData<List<ActivityTransition>>

    @Query("DELETE FROM activities_table")
    suspend fun deleteAll()
}