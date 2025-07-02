package br.edu.utfpr.social_pm46s.data.repository

import br.edu.utfpr.social_pm46s.data.model.RegistroAtividade
import br.edu.utfpr.social_pm46s.data.model.workout.WorkoutResult
import br.edu.utfpr.social_pm46s.data.model.workout.WorkoutStats
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

class ActivityRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val workoutCollection = firestore.collection("workouts")
    private val userWorkoutsCollection = firestore.collection("user_workouts")
    private val atividadeCollection = firestore.collection("atividades")

    private val _recentWorkouts = MutableStateFlow<List<WorkoutResult>>(emptyList())
    val recentWorkouts: StateFlow<List<WorkoutResult>> = _recentWorkouts

    private val _weeklyStats = MutableStateFlow<WorkoutStats?>(null)
    val weeklyStats: StateFlow<WorkoutStats?> = _weeklyStats

    suspend fun saveWorkout(workout: WorkoutResult): Boolean {
        return try {
            val workoutId =
                if (workout.id.isNotEmpty()) workout.id else UUID.randomUUID().toString()
            val workoutToSave = workout.copy(id = workoutId)

            workoutCollection.document(workoutId).set(workoutToSave).await()

            userWorkoutsCollection
                .document(workout.userId)
                .collection("workouts")
                .document(workoutId)
                .set(
                    mapOf(
                        "workoutId" to workoutId,
                        "startTime" to workout.startTime,
                        "type" to workout.type,
                        "title" to workout.title
                    )
                ).await()

            refreshUserWorkouts(workout.userId)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getUserWorkouts(userId: String, limit: Int = 20): List<WorkoutResult> {
        return try {
            val workoutRefs = userWorkoutsCollection
                .document(userId)
                .collection("workouts")
                .orderBy("startTime", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            val workoutIds = workoutRefs.documents.mapNotNull { it.getString("workoutId") }

            val workouts = mutableListOf<WorkoutResult>()
            for (id in workoutIds) {
                workoutCollection.document(id).get().await()
                    .toObject(WorkoutResult::class.java)?.let { workout ->
                        workouts.add(workout)
                    }
            }
            workouts.sortedByDescending { it.startTime }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getWorkoutById(workoutId: String): WorkoutResult? {
        return try {
            workoutCollection.document(workoutId).get().await().toObject(WorkoutResult::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deleteWorkout(workoutId: String, userId: String): Boolean {
        return try {
            workoutCollection.document(workoutId).delete().await()

            userWorkoutsCollection
                .document(userId)
                .collection("workouts")
                .document(workoutId)
                .delete()
                .await()

            refreshUserWorkouts(userId)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getWeeklyStats(userId: String): WorkoutStats? {
        return try {
            val weekStart = LocalDate.now().minusDays(7)
            val workouts = getWorkoutsByDateRange(userId, weekStart, LocalDate.now())

            if (workouts.isNotEmpty()) {
                val totalDuration = workouts.sumOf { it.duration.toMinutes() }
                val totalSteps = workouts.sumOf { it.steps }
                val totalDistance = workouts.sumOf { it.distance }
                val totalCalories = workouts.sumOf { it.calories }

                WorkoutStats(
                    duration = Duration.ofMinutes(totalDuration),
                    steps = totalSteps,
                    distance = totalDistance,
                    estimatedCalories = totalCalories
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun refreshUserWorkouts(userId: String) {
        val workouts = getUserWorkouts(userId, 10)
        _recentWorkouts.value = workouts

        getWeeklyStats(userId)?.let { stats ->
            _weeklyStats.value = stats
        }
    }

    suspend fun getWorkoutsByDateRange(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<WorkoutResult> {
        return try {
            val startMillis =
                startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endMillis =
                endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val workoutRefs = userWorkoutsCollection
                .document(userId)
                .collection("workouts")
                .whereGreaterThanOrEqualTo("startTime", startMillis)
                .whereLessThan("startTime", endMillis)
                .orderBy("startTime", Query.Direction.DESCENDING)
                .get()
                .await()

            val workoutIds = workoutRefs.documents.mapNotNull { it.getString("workoutId") }

            val workouts = mutableListOf<WorkoutResult>()
            for (id in workoutIds) {
                workoutCollection.document(id).get().await()
                    .toObject(WorkoutResult::class.java)?.let { workout ->
                        workouts.add(workout)
                    }
            }
            workouts.sortedByDescending { it.startTime }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveAtividade(atividade: RegistroAtividade): Boolean {
        return try {
            val documentRef = if (atividade.id.isNotEmpty()) {
                atividadeCollection.document(atividade.id)
            } else {
                atividadeCollection.document()
            }

            documentRef.set(atividade).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getAtividade(atividadeId: String): RegistroAtividade? {
        return try {
            atividadeCollection.document(atividadeId).get().await()
                .toObject(RegistroAtividade::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getAtividadesByUser(userId: String): List<RegistroAtividade> {
        return try {
            atividadeCollection.whereEqualTo("idUsuario", userId)
                .get().await().toObjects(RegistroAtividade::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun deleteAtividade(atividadeId: String): Boolean {
        return try {
            atividadeCollection.document(atividadeId).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }
}