package com.chickenduy.locationApp.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.chickenduy.locationApp.data.database.dao.ActivitiesDao
import com.chickenduy.locationApp.data.database.dao.GPSDao
import com.chickenduy.locationApp.data.database.dao.StepsDao
import com.chickenduy.locationApp.data.database.entity.Activities
import com.chickenduy.locationApp.data.database.entity.GPS
import com.chickenduy.locationApp.data.database.entity.Steps
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Database(entities = [
    GPS::class,
    Activities::class,
    Steps::class
], version = 1)
abstract class TrackingDatabase : RoomDatabase() {

    abstract fun gPSDao(): GPSDao
    abstract fun activitiesDao(): ActivitiesDao
    abstract fun stepsDao(): StepsDao

    companion object {

        var hasStepCounter: Boolean = false

        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: TrackingDatabase? = null

        fun getDatabase(context: Context): TrackingDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TrackingDatabase::class.java,
                    "tracking.db"
                ).build()
                INSTANCE = instance
                return instance
            }
        }
    }
}

