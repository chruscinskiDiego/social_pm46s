package br.edu.utfpr.social_pm46s.tracker

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import br.edu.utfpr.social_pm46s.data.model.workout.RealTimeWorkoutData
import br.edu.utfpr.social_pm46s.data.model.workout.WorkoutResult
import br.edu.utfpr.social_pm46s.data.model.workout.WorkoutStats
import br.edu.utfpr.social_pm46s.data.model.workout.WorkoutSummary
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
    private var pauseStartTime: Instant? = null
    private var totalPausedDuration: Duration = Duration.ZERO
    private var exerciseType: Int? = null
    private var exerciseTitle: String? = null

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused

    val realTimeData: StateFlow<RealTimeWorkoutData?> = combine(
        _isTracking,
        _isPaused,
        sensorTracker.currentSteps,
        locationTracker.totalDistance.map { it },
        locationTracker.currentPace.map { it ?: 0.0 }
    ) { tracking: Boolean, paused: Boolean, steps: Int, distance: Double, pace: Double ->
        if (tracking && startTime != null && exerciseType != null && exerciseTitle != null) {
            val effectiveDuration = getEffectiveDuration()
            val calories = calorieCalculator.calculate(
                exerciseType!!,
                effectiveDuration.toMinutes().toInt(),
                getUserWeight()
            )

            val stats = WorkoutStats(
                duration = effectiveDuration,
                steps = steps,
                distance = distance,
                estimatedCalories = calories,
                currentPace = if (paused) 0.0 else pace
            )

            RealTimeWorkoutData(
                isActive = tracking && !paused,
                workoutType = exerciseType!!,
                title = exerciseTitle!!,
                stats = stats,
                startTime = startTime!!
            )
        } else null
    }.stateIn(scope, kotlinx.coroutines.flow.SharingStarted.Lazily, null)

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    suspend fun startWorkout(type: Int, title: String): Boolean {
        if (_isTracking.value) {
            return false
        }

        return try {
            startTime = Instant.now()
            exerciseType = type
            exerciseTitle = title
            totalPausedDuration = Duration.ZERO
            pauseStartTime = null

            // Iniciar rastreamento de sensores
            sensorTracker.startTracking()

            // Iniciar GPS se necessário
            if (needsLocationTracking(type)) {
                locationTracker.startTracking()
            }

            // Iniciar sessão no Health Connect
            healthConnectService.startExercise(type, title)

            _isTracking.value = true
            _isPaused.value = false

            true
        } catch (e: Exception) {
            resetState()
            false
        }
    }

    suspend fun pauseWorkout(): Boolean {
        if (!_isTracking.value || _isPaused.value) {
            return false
        }

        return try {
            pauseStartTime = Instant.now()
            _isPaused.value = true

            // Pausar trackers - remover métodos que não existem
            sensorTracker.stopTracking()
            locationTracker.stopTracking()

            true
        } catch (e: Exception) {
            false
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    suspend fun resumeWorkout(): Boolean {
        if (!_isTracking.value || !_isPaused.value) {
            return false
        }

        return try {
            // Calcular tempo pausado
            pauseStartTime?.let { pauseStart ->
                val pauseDuration = Duration.between(pauseStart, Instant.now())
                totalPausedDuration = totalPausedDuration.plus(pauseDuration)
            }

            pauseStartTime = null
            _isPaused.value = false

            // Retomar trackers - usar startTracking novamente
            sensorTracker.startTracking()
            if (exerciseType?.let { needsLocationTracking(it) } == true) {
                locationTracker.startTracking()
            }

            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun finishWorkout(userNotes: String? = null): WorkoutResult? {
        if (!_isTracking.value) return null

        return try {
            val endTime = Instant.now()
            val effectiveDuration = getEffectiveDuration()

            // Se estiver pausado, calcular duração pausada final
            if (_isPaused.value && pauseStartTime != null) {
                val finalPauseDuration = Duration.between(pauseStartTime!!, endTime)
                totalPausedDuration = totalPausedDuration.plus(finalPauseDuration)
            }

            val finalStats = getCurrentWorkoutStats()

            if (finalStats != null) {
                val workoutSummary = WorkoutSummary(
                    exerciseType = exerciseType!!,
                    title = exerciseTitle!!,
                    startTime = startTime!!,
                    endTime = endTime,
                    duration = effectiveDuration,
                    caloriesBurned = finalStats.estimatedCalories,
                    distanceKm = finalStats.distance,
                    steps = finalStats.steps.toLong(),
                    averageHeartRate = null, // Remover propriedades que não existem
                    maxHeartRate = null, // Remover propriedades que não existem
                    averageSpeed = null // Remover propriedades que não existem
                )

                // Salvar no Health Connect
                healthConnectService.finishExerciseComplete(workoutSummary)

                val workoutResult = WorkoutResult(
                    id = generateWorkoutId(),
                    type = exerciseType!!,
                    title = exerciseTitle!!,
                    duration = effectiveDuration,
                    steps = finalStats.steps,
                    distance = finalStats.distance,
                    calories = finalStats.estimatedCalories,
                    startTime = startTime!!,
                    endTime = endTime,
                    notes = userNotes,
                    userId = getCurrentUserId()
                )

                resetState()
                workoutResult
            } else {
                cancelWorkout()
                null
            }
        } catch (e: Exception) {
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

    fun getCurrentWorkoutStats(): WorkoutStats? {
        if (!_isTracking.value || startTime == null) return null

        val effectiveDuration = getEffectiveDuration()
        val calories = calorieCalculator.calculate(
            exerciseType!!,
            effectiveDuration.toMinutes().toInt(),
            getUserWeight()
        )

        return WorkoutStats(
            duration = effectiveDuration,
            steps = sensorTracker.currentSteps.value,
            distance = locationTracker.totalDistance.value,
            estimatedCalories = calories,
            currentPace = if (_isPaused.value) 0.0 else locationTracker.currentPace.value
        )
    }

    private fun getEffectiveDuration(): Duration {
        val currentTime = Instant.now()
        val totalDuration = startTime?.let { Duration.between(it, currentTime) } ?: Duration.ZERO

        var pausedTime = totalPausedDuration

        // Se estiver pausado agora, adicionar o tempo da pausa atual
        if (_isPaused.value && pauseStartTime != null) {
            val currentPauseDuration = Duration.between(pauseStartTime!!, currentTime)
            pausedTime = pausedTime.plus(currentPauseDuration)
        }

        return totalDuration.minus(pausedTime)
    }

    private fun resetState() {
        startTime = null
        exerciseType = null
        exerciseTitle = null
        pauseStartTime = null
        totalPausedDuration = Duration.ZERO
        _isTracking.value = false
        _isPaused.value = false
    }

    private fun needsLocationTracking(exerciseType: Int): Boolean {
        return when (exerciseType) {
            HealthConnectService.EXERCISE_TYPE_WALKING,
            HealthConnectService.EXERCISE_TYPE_RUNNING,
            HealthConnectService.EXERCISE_TYPE_CYCLING -> true

            else -> false
        }
    }

    private fun getUserWeight(): Double {
        // TODO: Implementar busca do peso do usuário do perfil
        return 70.0
    }

    private fun getCurrentUserId(): String {
        // TODO: Implementar busca do ID do usuário atual
        return "user_123"
    }

    private fun generateWorkoutId(): String {
        return "workout_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }

    // Métodos de conveniência para tipos específicos de exercício
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    suspend fun startWalking(title: String = "Caminhada") =
        startWorkout(HealthConnectService.EXERCISE_TYPE_WALKING, title)

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    suspend fun startRunning(title: String = "Corrida") =
        startWorkout(HealthConnectService.EXERCISE_TYPE_RUNNING, title)

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    suspend fun startCycling(title: String = "Ciclismo") =
        startWorkout(HealthConnectService.EXERCISE_TYPE_CYCLING, title)

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    suspend fun startWeightlifting(title: String = "Musculação") =
        startWorkout(HealthConnectService.EXERCISE_TYPE_WEIGHTLIFTING, title)

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    suspend fun startYoga(title: String = "Yoga") =
        startWorkout(HealthConnectService.EXERCISE_TYPE_YOGA, title)

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    suspend fun startSwimming(title: String = "Natação") =
        startWorkout(HealthConnectService.EXERCISE_TYPE_SWIMMING, title)

    // Getters para estados
    fun isCurrentlyTracking(): Boolean = _isTracking.value
    fun isCurrentlyPaused(): Boolean = _isPaused.value
    fun getCurrentExerciseType(): Int? = exerciseType
    fun getCurrentExerciseTitle(): String? = exerciseTitle
    fun getWorkoutStartTime(): Instant? = startTime
    fun getTotalPausedDuration(): Duration = totalPausedDuration
}