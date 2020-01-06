package com.chickenduy.locationApp.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activitiesDetailed_table")
data class ActivitiesDetailed (
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long,

    @ColumnInfo(name = "start")
    val start: Long,

    @ColumnInfo(name = "end")
    val end: Long,

    /**
     * 0 - in vehicle
     * 1 - on bicycle
     * 2 - on foot
     * 3 - still
     * 4 - unknown
     * 5 - tilting
     * 6 - N/A
     * 7 - on foot -> walking
     * 8 - on foot -> running
     */
    @ColumnInfo(name = "type")
    val type: Int
)