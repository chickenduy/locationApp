package com.chickenduy.locationApp.backgroundServices.stepsService

import android.content.Context
import android.content.Context.SENSOR_SERVICE
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.chickenduy.locationApp.MyApp
import com.chickenduy.locationApp.data.database.TrackingDatabase
import com.chickenduy.locationApp.data.database.entity.Steps
import com.chickenduy.locationApp.data.repository.StepsRepository
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

class StepsLogger(private val context: Context): Runnable, SensorEventListener {
    private val TAG = "STEPSLOGGER"
    private lateinit var sensorManager: SensorManager
    private var lastTimeStamp = Date().time
    private lateinit var stepsRepository: StepsRepository

    /**
     * Registering the step sensor listener if the
     * sensor is available
     */
    override fun run() {
        Log.d(TAG,"Starting StepsLogger")
        sensorManager = context.getSystemService(SENSOR_SERVICE) as SensorManager
        val stepsSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        stepsRepository = StepsRepository(TrackingDatabase.getDatabase(MyApp.instance).stepsDao())
//        GlobalScope.launch {
//            stepsRepository.deleteAll()
//        }

        if (stepsSensor != null) {
            TrackingDatabase.hasStepCounter = true
            sensorManager.registerListener(
                this,
                stepsSensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
        else
        {
            Log.e(TAG, "Missing StepSensor")
        }
    }

    /**
     * Triggered each time the sensor reports new data
     */
    override fun onSensorChanged(event: SensorEvent) {
        val now = Date().time
        // We only save steps data at a 60 seconds interval
            if (now - lastTimeStamp > 1000 * 10) {
            lastTimeStamp = now
            GlobalScope.launch {
                stepsRepository.insert(
                    Steps(
                        0,
                        Date().time,
                        event.values[0].toInt()
                    )
                )
            }
            Log.d(TAG, "Logged steps")
        }
    }

    /**
     * This is required for a SensorListener
     * */
    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {}
}
