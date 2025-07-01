package br.edu.utfpr.social_pm46s.data.model.workout

import java.time.Instant

data class RealTimeWorkoutData(
    val isActive: Boolean,
    val workoutType: Int,
    val title: String,
    val stats: WorkoutStats,
    val startTime: Instant
)