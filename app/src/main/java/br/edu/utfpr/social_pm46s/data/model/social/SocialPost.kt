package br.edu.utfpr.social_pm46s.data.model.social

import br.edu.utfpr.social_pm46s.data.model.workout.WorkoutResult

data class SocialPost(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val message: String = "",
    val type: String = "",
    val timestamp: Long = 0,
    val workoutData: WorkoutResult? = null,
    val likes: List<String> = emptyList(),
    val comments: List<Map<String, Any>> = emptyList()
)
