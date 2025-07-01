package br.edu.utfpr.social_pm46s.tracker

import br.edu.utfpr.social_pm46s.service.HealthConnectService

class CalorieCalculator {

    private val metValues = mapOf(
        HealthConnectService.EXERCISE_TYPE_WALKING to 3.8,
        HealthConnectService.EXERCISE_TYPE_RUNNING to 9.8,
        HealthConnectService.EXERCISE_TYPE_CYCLING to 7.5,
        HealthConnectService.EXERCISE_TYPE_WEIGHTLIFTING to 6.0,
        HealthConnectService.EXERCISE_TYPE_YOGA to 2.5,
        HealthConnectService.EXERCISE_TYPE_SWIMMING to 8.0
    )

    fun calculate(exerciseType: Int, durationMinutes: Int, weightKg: Double): Double {
        val met = metValues[exerciseType] ?: 4.0
        return met * weightKg * (durationMinutes / 60.0)
    }
}