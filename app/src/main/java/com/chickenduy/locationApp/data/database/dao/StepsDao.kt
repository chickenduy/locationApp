package com.chickenduy.locationApp.data.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.ABORT
import androidx.room.Query
import com.chickenduy.locationApp.data.database.entity.Steps

@Dao
interface StepsDao {
    @Insert(onConflict = ABORT)
    fun insert(steps: Steps): Long

    @Insert(onConflict = ABORT)
    fun insert(steps: List<Steps>)

    @Query("SELECT * FROM steps_table WHERE id = :id LIMIT 1")
    fun getById(id: Long): Steps

    @Query("SELECT * FROM steps_table WHERE start > :minTimestamp AND `end` < :maxTimestamp ORDER BY start")
    fun getByTimestamps(minTimestamp: Long, maxTimestamp: Long): List<Steps>

    @Query("SELECT * FROM steps_table  ORDER BY start DESC Limit 10")
    fun get10Recent(): LiveData<List<Steps>>

    @Query("SELECT * FROM steps_table  ORDER BY start DESC")
    fun getAll(): LiveData<List<Steps>>

    @Query("DELETE FROM steps_table")
    fun deleteAll()
}