package br.edu.utfpr.social_pm46s.data.model

import java.time.Duration
import java.time.Instant

data class HealthData(
    val steps: List<StepData>,
    val heartRate: List<HeartRateData>,
    val weight: List<WeightData>,
    val distance: List<DistanceData>,
    val sleep: List<SleepData>,
    val exercises: List<ExerciseData>,
    val aggregated: AggregatedData?
)

data class StepData(
    val count: Long,
    val startTime: Instant,
    val endTime: Instant
)

data class HeartRateData(
    val beatsPerMinute: Long,
    val time: Instant
)

data class WeightData(
    val weightKg: Double,
    val time: Instant
)

data class DistanceData(
    val distanceKm: Double,
    val startTime: Instant,
    val endTime: Instant
)

data class SleepData(
    val startTime: Instant,
    val endTime: Instant,
    val duration: Duration,
    val stages: List<SleepStageData> = emptyList()
)

data class SleepStageData(
    val stage: Int,
    val startTime: Instant,
    val endTime: Instant
)

data class ExerciseData(
    val sessionType: Int,
    val title: String,
    val startTime: Instant,
    val endTime: Instant,
    val duration: Duration,
    val caloriesBurned: Double
)

data class AggregatedData(
    val totalSteps: Long,
    val totalDistanceKm: Double,
    val totalActiveCalories: Double
)

data class ActiveExerciseSession(
    val sessionType: Int,
    val title: String,
    val startTime: Instant
)