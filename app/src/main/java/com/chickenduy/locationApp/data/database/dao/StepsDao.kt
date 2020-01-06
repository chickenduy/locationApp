package com.chickenduy.locationApp.data.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.ABORT
import androidx.room.Query
import com.chickenduy.locationApp.data.database.entity.GPS
import com.chickenduy.locationApp.data.database.entity.Steps
import java.util.*

@Dao
interface StepsDao {
    @Insert(onConflict = ABORT)
    suspend fun insert(steps: Steps): Long

    @Insert(onConflict = ABORT)
    suspend fun insert(steps: List<Steps>)

    @Query("SELECT * FROM steps_table WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Steps

    @Query("SELECT * FROM steps_table WHERE timestamp > :minTimestamp AND timestamp < :maxTimestamp ORDER BY timestamp LIMIT 1")
    fun getByTimestamp(minTimestamp: Long, maxTimestamp: Long): List<Steps>

    @Query("SELECT * FROM steps_table  ORDER BY timestamp DESC Limit 10")
    fun get10Recent(): LiveData<List<Steps>>

    @Query("SELECT * FROM steps_table  ORDER BY timestamp DESC")
    fun getAll(): LiveData<List<Steps>>

    @Query("DELETE FROM steps_table")
    suspend fun deleteAll()
}