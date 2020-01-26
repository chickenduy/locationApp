package com.chickenduy.locationApp.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "steps_table")
data class Steps(
    @PrimaryKey(autoGenerate = true)
    val id: Long,

    @ColumnInfo(name = "start")
    val start: Long,

    @ColumnInfo(name = "end")
    val end: Long,

    @ColumnInfo(name = "steps")
    val steps: Int
)