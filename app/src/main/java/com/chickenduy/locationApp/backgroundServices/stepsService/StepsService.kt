package com.chickenduy.locationApp.backgroundServices.stepsService

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.util.Log
import com.chickenduy.locationApp.MyApp
import com.chickenduy.locationApp.data.database.TrackingDatabase
import com.chickenduy.locationApp.data.repository.StepsRepository

/**
 * This class activates the step sensor
 */
class StepsService(context: Context) {
    private val logTAG = "STEPSSERVICE"
    private var sensorManager: SensorManager
    private var stepsRepository: StepsRepository
    private val stepsLogger = StepsLogger()

    /**
     * Registering the step sensor listener if the
     * sensor is available
     */
    init {
        Log.d(logTAG, "Starting StepsService")
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val stepsSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        stepsRepository = StepsRepository(TrackingDatabase.getDatabase(MyApp.instance).stepsDao())
//        GlobalScope.launch {
//            stepsRepository.deleteAll()
//        }

        if (stepsSensor != null) {
            TrackingDatabase.hasStepCounter = true
            sensorManager.registerListener(
                stepsLogger,
                stepsSensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        } else {
            Log.e(logTAG, "Missing StepSensor")
        }
    }
}