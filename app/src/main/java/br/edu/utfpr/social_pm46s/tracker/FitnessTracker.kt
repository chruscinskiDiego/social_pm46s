package br.edu.utfpr.social_pm46s.tracker

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import br.edu.utfpr.social_pm46s.data.model.workout.RealTimeWorkoutData
import br.edu.utfpr.social_pm46s.data.model.workout.WorkoutResult
import br.edu.utfpr.social_pm46s.data.model.workout.WorkoutStats
import br.edu.utfpr.social_pm46s.service.HealthConnectService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.map
import java.time.Duration
import java.time.Instant

class FitnessTracker(
    private val context: Context,
    private val healthConnectService: HealthConnectService
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val sensorTracker = SensorTracker(context)
    private val locationTracker = LocationTracker(context)
    private val calorieCalculator = CalorieCalculator()

    private var startTime: Instant? = null
    private var exerciseType: Int? = null
    private var exerciseTitle: String? = null

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking

    val realTimeData: StateFlow<RealTimeWorkoutData?> = combine(
        _isTracking,
        sensorTracker.currentSteps,
        locationTracker.totalDistance.map { it },
        locationTracker.currentPace.map { it ?: 0.0 }
    ) { tracking: Boolean, steps: Int, distance: Double, pace: Double ->
        if (tracking && startTime != null && exerciseType != null && exerciseTitle != null) {
            val currentDuration = Duration.between(startTime, Instant.now())
            val calories = calorieCalculator.calculate(
                exerciseType!!,
                currentDuration.toMinutes().toInt(),
                getUserWeight()
            )

            RealTimeWorkoutData(
                isActive = true,
                workoutType = exerciseType!!,
                title = exerciseTitle!!,
                stats = WorkoutStats(
                    duration = currentDuration,
                    steps = steps,
                    distance = distance,
                    estimatedCalories = calories,
                    currentPace = pace
                ),
                startTime = startTime!!
            )
        } else null
    }.stateIn(scope, kotlinx.coroutines.flow.SharingStarted.Lazily, null)

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    suspend fun startWorkout(type: Int, title: String): Boolean {
        if (_isTracking.value) return false

        return try {
            if (!healthConnectService.startExercise(type, title)) return false

            startTime = Instant.now()
            exerciseType = type
            exerciseTitle = title

            sensorTracker.startTracking()

            if (needsLocationTracking(type)) {
                locationTracker.startTracking()
            }

            _isTracking.value = true
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun finishWorkout(userNotes: String? = null): WorkoutResult? {
        if (!_isTracking.value) return null

        val endTime = Instant.now()
        val duration = startTime?.let { Duration.between(it, endTime) } ?: return null

        val steps = sensorTracker.stopTracking()
        val distance = locationTracker.stopTracking()
        val calories = calorieCalculator.calculate(
            exerciseType ?: 0,
            duration.toMinutes().toInt(),
            getUserWeight()
        )

        // Salva no Health Connect
        val saved = healthConnectService.finishExercise(calories, userNotes)

        return if (saved) {
            val result = WorkoutResult(
                id = "",
                type = exerciseType ?: 0,
                title = exerciseTitle ?: "",
                durationMinutes = duration.toMinutes(),
                steps = steps,
                distance = distance,
                calories = calories,
                startTime = startTime!!.toEpochMilli(),
                endTime = endTime.toEpochMilli(),
                notes = userNotes,
                userId = getCurrentUserId()
            )

            resetState()
            result
        } else {
            resetState()
            null
        }
    }

    suspend fun cancelWorkout(): Boolean {
        if (!_isTracking.value) return false

        sensorTracker.stopTracking()
        locationTracker.stopTracking()
        healthConnectService.cancelExercise()
        resetState()
        return true
    }

    private fun resetState() {
        startTime = null
        exerciseType = null
        exerciseTitle = null
        _isTracking.value = false
    }

    private fun needsLocationTracking(exerciseType: Int): Boolean {
        return when (exerciseType) {
            HealthConnectService.EXERCISE_TYPE_RUNNING,
            HealthConnectService.EXERCISE_TYPE_WALKING,
            HealthConnectService.EXERCISE_TYPE_CYCLING -> true

            else -> false
        }
    }

    private fun getUserWeight(): Double {
        // Implementar busca do peso do usuário
        return 70.0
    }

    private fun getCurrentUserId(): String {
        // Implementar busca do ID do usuário atual
        return "user_123"
    }
}