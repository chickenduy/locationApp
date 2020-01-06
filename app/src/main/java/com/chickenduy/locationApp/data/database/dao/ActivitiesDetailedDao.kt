package com.chickenduy.locationApp.data.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.ABORT
import androidx.room.Query
import com.chickenduy.locationApp.data.database.entity.ActivitiesDetailed

@Dao
interface ActivitiesDetailedDao {
    @Insert(onConflict = ABORT)
    suspend fun insert(activitiesDetailed: ActivitiesDetailed): Long

    @Insert(onConflict = ABORT)
    suspend fun insert(activitiesDetailed: List<ActivitiesDetailed>)

    @Query("SELECT * FROM activitiesDetailed_table WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ActivitiesDetailed

    @Query("SELECT * FROM activitiesDetailed_table WHERE start < :timestamp AND `end` > :timestamp")
    suspend fun getByTimestamp(timestamp: Long): List<ActivitiesDetailed>

    @Query("SELECT * FROM activitiesDetailed_table")
    fun getAll(): LiveData<List<ActivitiesDetailed>>

    @Query("SELECT * FROM activitiesDetailed_table ORDER BY start ASC LIMIT 10")
    fun get10Recent(): LiveData<List<ActivitiesDetailed>>

    @Query("DELETE FROM activitiesDetailed_table")
    suspend fun deleteAll()
}