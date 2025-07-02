package br.edu.utfpr.social_pm46s.data.model.workout


import java.time.Duration
import java.time.Instant


data class WorkoutResult(
    val id: String = "",
    val type: Int = 0,
    val title: String = "",
    val durationMinutes: Long = 0, // Mudança: Duration não é serializável pelo Firebase
    val steps: Int = 0,
    val distance: Double = 0.0,
    val calories: Double = 0.0,
    val startTime: Long = 0, // Timestamp em millis
    val endTime: Long = 0,   // Timestamp em millis
    val notes: String? = null,
    val userId: String = ""
) {
    // Propriedades computadas para manter compatibilidade
    val duration: Duration
        get() = Duration.ofMinutes(durationMinutes)

    val startTimeInstant: Instant
        get() = Instant.ofEpochMilli(startTime)

    val endTimeInstant: Instant
        get() = Instant.ofEpochMilli(endTime)

    // Construtor para converter de Instant/Duration para Firebase
    constructor(
        id: String,
        type: Int,
        title: String,
        duration: Duration,
        steps: Int,
        distance: Double,
        calories: Double,
        startTime: Instant,
        endTime: Instant,
        notes: String?,
        userId: String
    ) : this(
        id = id,
        type = type,
        title = title,
        durationMinutes = duration.toMinutes(),
        steps = steps,
        distance = distance,
        calories = calories,
        startTime = startTime.toEpochMilli(),
        endTime = endTime.toEpochMilli(),
        notes = notes,
        userId = userId
    )
}