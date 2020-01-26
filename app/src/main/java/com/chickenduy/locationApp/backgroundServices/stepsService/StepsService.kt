package com.chickenduy.locationApp.backgroundServices.stepsService

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.util.Log
import com.chickenduy.locationApp.MyApp
import com.chickenduy.locationApp.data.database.TrackingDatabase
import com.chickenduy.locationApp.data.database.entity.Steps
import com.chickenduy.locationApp.data.repository.StepsRepository
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * This class activates the step sensor
 */
class StepsService(context: Context) {
    private val TAG = "STEPSSERVICE"
    private var sensorManager: SensorManager
    private val stepsLogger = StepsLogger()

    /**
     * Registering the step sensor listener if the
     * sensor is available
     */
    init {
        Log.d(TAG, "Starting StepsService")
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val stepsSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        GlobalScope.launch {
            val stepsRepository = StepsRepository(TrackingDatabase.getDatabase(MyApp.instance).stepsDao())
            stepsRepository.deleteAll()
            val stepsList: List<Steps> = listOf(
                Steps(0,1577836800000,1577836860000, Random.nextInt(0,300)),
                Steps(0,1577836860000,1577836920000, Random.nextInt(0,300)),
                Steps(0,1577836920000,1577836980000, Random.nextInt(0,300)),
                Steps(0,1577836980000,1577837040000, Random.nextInt(0,300)),
                Steps(0,1577837040000,1577837100000, Random.nextInt(0,300)),
                Steps(0,1577837100000,1577837160000, Random.nextInt(0,300)),
                Steps(0,1577837160000,1577837220000, Random.nextInt(0,300)),
                Steps(0,1577837220000,1577837280000, Random.nextInt(0,300)),
                Steps(0,1577837280000,1577837340000, Random.nextInt(0,300)),
                Steps(0,1577837340000,1577837400000, Random.nextInt(0,300))
            )
            stepsRepository.insert(stepsList)
        }

        if (stepsSensor != null) {
            sensorManager.registerListener(
                stepsLogger,
                stepsSensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        } else {
            Log.e(TAG, "Missing StepSensor")
        }
    }
}