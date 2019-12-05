package com.chickenduy.locationApp.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "steps_table")
data class Steps (
    @PrimaryKey(autoGenerate = true)
    val id: Long,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "steps")
    val steps: Int
    )