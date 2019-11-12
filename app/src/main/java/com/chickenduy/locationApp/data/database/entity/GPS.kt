package com.chickenduy.locationApp.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gps_table")
data class GPS (
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "longitude")
    val longitude: Float,

    @ColumnInfo(name = "latitude")
    val latitude: Float
    )