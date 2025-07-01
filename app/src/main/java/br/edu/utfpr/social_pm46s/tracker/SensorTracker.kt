package br.edu.utfpr.social_pm46s.tracker

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SensorTracker(private val context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private var initialStepCount = 0
    private var isTracking = false

    private val _currentSteps = MutableStateFlow(0)
    val currentSteps: StateFlow<Int> = _currentSteps

    private val stepListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                if (it.sensor.type == Sensor.TYPE_STEP_COUNTER) {
                    if (initialStepCount == 0) {
                        initialStepCount = it.values[0].toInt()
                    }
                    val currentCount = it.values[0].toInt() - initialStepCount
                    _currentSteps.value = currentCount
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    fun startTracking(): Boolean {
        return if (stepCounterSensor != null && !isTracking) {
            sensorManager.registerListener(stepListener, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL)
            isTracking = true
            initialStepCount = 0
            _currentSteps.value = 0
            true
        } else {
            false
        }
    }

    fun stopTracking(): Int {
        if (isTracking) {
            sensorManager.unregisterListener(stepListener)
            isTracking = false
        }
        return _currentSteps.value
    }

    fun hasStepCounter(): Boolean = stepCounterSensor != null
}