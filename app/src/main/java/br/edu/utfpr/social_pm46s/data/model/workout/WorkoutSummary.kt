package br.edu.utfpr.social_pm46s.data.model.workout

import java.time.Duration
import java.time.Instant

data class WorkoutSummary(
    val exerciseType: Int,
    val title: String,
    val startTime: Instant,
    val endTime: Instant,
    val duration: Duration,
    val caloriesBurned: Double?,
    val distanceKm: Double?,
    val steps: Long?,
    val averageHeartRate: Long?,
    val maxHeartRate: Long?,
    val averageSpeed: Double?
)