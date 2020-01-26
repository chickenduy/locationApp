package com.chickenduy.locationApp.backgroundServices.stepsService

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.util.Log
import com.chickenduy.locationApp.MyApp
import com.chickenduy.locationApp.data.database.TrackingDatabase
import com.chickenduy.locationApp.data.database.entity.Steps
import com.chickenduy.locationApp.data.repository.StepsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.abs

/**
 * This class saves the step data to a database
 */
class StepsLogger : SensorEventListener {
    private val TAG = "STEPSLOGGER"
    private var lastTimeStamp = Date().time
    private val stepsRepository: StepsRepository =
        StepsRepository(TrackingDatabase.getDatabase(MyApp.instance).stepsDao())
    private var stepCounter = 0

    /**
     * Triggered each time the sensor reports new data
     */
    override fun onSensorChanged(event: SensorEvent) {
        val now = Date().time
        // We save steps data every minute
        if (abs(now - lastTimeStamp) > 1000 * 60) {
            Thread(Runnable {
                stepsRepository.insert(
                    Steps(
                        0,
                        lastTimeStamp,
                        now,
                        event.values[0].toInt() - stepCounter
                    )
                )
                lastTimeStamp = now
                stepCounter = event.values[0].toInt()
                Log.d(TAG, "Logged steps")
            }).start()
        }
    }

    /**
     * This is required for a SensorListener
     * */
    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {}
}
