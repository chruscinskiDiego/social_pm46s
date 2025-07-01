package br.edu.utfpr.social_pm46s.data.model.social

data class UserRanking(
    val userId: String = "",
    val name: String = "",
    val totalSteps: Int = 0,
    val totalDistance: Double = 0.0,
    val totalCalories: Double = 0.0
)