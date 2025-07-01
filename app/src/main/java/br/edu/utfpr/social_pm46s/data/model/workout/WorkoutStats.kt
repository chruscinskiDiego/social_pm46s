package br.edu.utfpr.social_pm46s.data.model.workout

import java.time.Duration

data class WorkoutStats(
    val duration: Duration,
    val steps: Int,
    val distance: Double,
    val estimatedCalories: Double,
    val currentPace: Double? = null
)
