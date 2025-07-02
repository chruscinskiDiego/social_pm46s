package br.edu.utfpr.social_pm46s.service

import android.util.Log
import br.edu.utfpr.social_pm46s.data.model.User
import br.edu.utfpr.social_pm46s.data.model.social.Achievement
import br.edu.utfpr.social_pm46s.data.model.social.Challenge
import br.edu.utfpr.social_pm46s.data.model.social.SocialPost
import br.edu.utfpr.social_pm46s.data.model.social.UserRanking
import br.edu.utfpr.social_pm46s.data.model.workout.WorkoutResult
import br.edu.utfpr.social_pm46s.data.model.workout.WorkoutStats
import br.edu.utfpr.social_pm46s.data.repository.ActivityRepository
import br.edu.utfpr.social_pm46s.data.repository.UserRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import java.time.Duration

class SocialFitnessService(
    private val activityRepository: ActivityRepository,
    private val userRepository: UserRepository
) {

    companion object {
        private const val TAG = "SocialFitnessService"
    }

    private val firestore = FirebaseFirestore.getInstance()
    private val socialPostsCollection = firestore.collection("social_posts")
    private val challengesCollection = firestore.collection("challenges")
    private val achievementsCollection = firestore.collection("achievements")

    val recentWorkouts: StateFlow<List<WorkoutResult>> = activityRepository.recentWorkouts
    val weeklyStats: StateFlow<WorkoutStats?> = activityRepository.weeklyStats

    suspend fun shareWorkoutStart(type: Int, title: String, userId: String) {
        val message = when (type) {
            HealthConnectService.EXERCISE_TYPE_RUNNING -> "🏃‍♂️ Começando uma corrida: $title"
            HealthConnectService.EXERCISE_TYPE_WALKING -> "🚶‍♂️ Saindo para caminhar: $title"
            HealthConnectService.EXERCISE_TYPE_CYCLING -> "🚴‍♂️ Pedalando: $title"
            HealthConnectService.EXERCISE_TYPE_WEIGHTLIFTING -> "💪 Treino de força: $title"
            HealthConnectService.EXERCISE_TYPE_YOGA -> "🧘‍♀️ Yoga: $title"
            HealthConnectService.EXERCISE_TYPE_SWIMMING -> "🏊‍♂️ Natação: $title"
            else -> "💪 Iniciando exercício: $title"
        }

        postToSocialFeed(message, "workout_start", userId, null)
    }

    suspend fun shareWorkoutResult(result: WorkoutResult) {
        val message = buildWorkoutResultMessage(result)
        postToSocialFeed(message, "workout_complete", result.userId, result)

        checkAndAwardAchievements(result)
    }

    suspend fun shareAchievement(userId: String, achievement: String, description: String) {
        val message = "🏆 Nova conquista desbloqueada!\n$achievement\n$description"
        postToSocialFeed(message, "achievement", userId, null)

        saveAchievement(userId, achievement, description)
    }

    suspend fun shareWeeklyGoal(userId: String, goalReached: Boolean, description: String) {
        val emoji = if (goalReached) "✅" else "💪"
        val message = "$emoji Meta semanal: $description"
        postToSocialFeed(message, "weekly_goal", userId, null)
    }

    suspend fun getFriendsActivity(userId: String): List<SocialPost> {
        return try {
            val user = userRepository.getUser(userId)
            val friendIds = emptyList<String>() // Implementar busca de amigos quando necessário

            if (friendIds.isEmpty()) return emptyList()

            val posts = mutableListOf<SocialPost>()

            friendIds.chunked(10).forEach { chunk ->
                val friendPosts = socialPostsCollection
                    .whereIn("userId", chunk)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(20)
                    .get()
                    .await()

                friendPosts.documents.forEach { doc ->
                    doc.toObject(SocialPost::class.java)?.let { post ->
                        posts.add(post)
                    }
                }
            }

            val postsWithUserNames = posts.map { post ->
                val userName = userRepository.getUser(post.userId)?.nomeUsuario ?: "Usuário"
                post.copy(userName = userName)
            }

            postsWithUserNames.sortedByDescending { it.timestamp }.take(50)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao buscar atividades dos amigos", e)
            emptyList()
        }
    }

    suspend fun getLeaderboard(period: String = "weekly"): List<UserRanking> {
        return try {
            val users = userRepository.getAllUsers()

            val rankings = users.mapIndexed { index, user ->
                val userStats = activityRepository.getWeeklyStats(user.id)

                UserRanking(
                    userId = user.id,
                    name = user.nomeUsuario,
                    totalSteps = userStats?.steps ?: 0,
                    totalDistance = userStats?.distance ?: 0.0,
                    totalCalories = userStats?.estimatedCalories ?: 0.0
                )
            }

            rankings.sortedByDescending { it.totalSteps }.take(10)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao buscar leaderboard", e)
            emptyList()
        }
    }

    suspend fun getChallenges(userId: String): List<Challenge> {
        return try {
            val challenges = challengesCollection
                .whereEqualTo("active", true)
                .get()
                .await()

            val challengeList = mutableListOf<Challenge>()

            challenges.documents.forEach { doc ->
                doc.toObject(Challenge::class.java)?.let { challenge ->
                    val progress = calculateChallengeProgress(userId, challenge)
                    challengeList.add(challenge.copy(userProgress = progress))
                }
            }

            challengeList
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao buscar desafios", e)
            emptyList()
        }
    }

    suspend fun getUserAchievements(userId: String): List<Achievement> {
        return try {
            val achievements = achievementsCollection
                .whereEqualTo("userId", userId)
                .orderBy("unlockedAt", Query.Direction.DESCENDING)
                .get()
                .await()

            achievements.documents.mapNotNull { doc ->
                doc.toObject(Achievement::class.java)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao buscar conquistas do usuário", e)
            emptyList()
        }
    }

    suspend fun likePost(postId: String, userId: String): Boolean {
        return try {
            val postRef = socialPostsCollection.document(postId)

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(postRef)
                val post = snapshot.toObject(SocialPost::class.java)

                if (post != null) {
                    val currentLikes = post.likes.toMutableList()
                    if (currentLikes.contains(userId)) {
                        currentLikes.remove(userId)
                    } else {
                        currentLikes.add(userId)
                    }

                    transaction.update(postRef, "likes", currentLikes)
                }
            }.await()

            true
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao curtir post", e)
            false
        }
    }

    suspend fun commentOnPost(postId: String, userId: String, comment: String): Boolean {
        return try {
            val userName = userRepository.getUser(userId)?.nomeUsuario ?: "Usuário"

            val commentData = mapOf(
                "userId" to userId,
                "userName" to userName,
                "comment" to comment,
                "timestamp" to System.currentTimeMillis()
            )

            val postRef = socialPostsCollection.document(postId)

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(postRef)
                val post = snapshot.toObject(SocialPost::class.java)

                if (post != null) {
                    val currentComments = post.comments.toMutableList()
                    currentComments.add(commentData)

                    transaction.update(postRef, "comments", currentComments)
                }
            }.await()

            true
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao comentar post", e)
            false
        }
    }

    private suspend fun postToSocialFeed(
        message: String,
        type: String,
        userId: String,
        workoutData: WorkoutResult?
    ) {
        try {
            val userName = userRepository.getUser(userId)?.nomeUsuario ?: "Usuário"

            val socialPost = SocialPost(
                id = firestore.collection("temp").document().id,
                userId = userId,
                userName = userName,
                message = message,
                type = type,
                timestamp = System.currentTimeMillis(),
                workoutData = workoutData,
                likes = emptyList(),
                comments = emptyList()
            )

            socialPostsCollection.document(socialPost.id).set(socialPost).await()
            Log.i(TAG, "Post social criado: $type - $message")

        } catch (e: Exception) {
            Log.e(TAG, "Erro ao postar no feed social", e)
        }
    }

    private fun buildWorkoutResultMessage(result: WorkoutResult): String {
        return buildString {
            append("✅ Finalizei: ${result.title}\n")
            append("⏱️ ${formatDuration(result.duration)}\n")

            if (result.distance > 0) {
                append("📍 ${String.format("%.2f", result.distance)} km\n")
            }

            if (result.steps > 0) {
                append("👣 ${result.steps} passos\n")
            }

            append("🔥 ${String.format("%.0f", result.calories)} kcal")

            result.notes?.let {
                if (it.isNotBlank()) {
                    append("\n📝 $it")
                }
            }
        }
    }

    private suspend fun checkAndAwardAchievements(workout: WorkoutResult) {
        try {
            checkFirstWorkoutAchievement(workout.userId)
            checkDistanceAchievements(workout)
            checkStreakAchievements(workout.userId)
            checkCalorieAchievements(workout)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao verificar conquistas", e)
        }
    }

    private suspend fun checkFirstWorkoutAchievement(userId: String) {
        val userWorkouts = activityRepository.getUserWorkouts(userId, 1)
        if (userWorkouts.size == 1) {
            shareAchievement(userId, "Primeira Atividade", "Parabéns pelo seu primeiro exercício!")
        }
    }

    private suspend fun checkDistanceAchievements(workout: WorkoutResult) {
        when {
            workout.distance >= 10.0 -> {
                shareAchievement(
                    workout.userId,
                    "10km Conquistados",
                    "Incrível! Você completou 10km!"
                )
            }

            workout.distance >= 5.0 -> {
                shareAchievement(workout.userId, "5km Conquistados", "Você correu/caminhou 5km!")
            }
        }
    }

    private suspend fun checkStreakAchievements(userId: String) {
        val weeklyStats = activityRepository.getWeeklyStats(userId)
        // Implementar lógica de sequência quando necessário
    }

    private suspend fun checkCalorieAchievements(workout: WorkoutResult) {
        when {
            workout.calories >= 1000 -> {
                shareAchievement(
                    workout.userId,
                    "Máquina de Queimar",
                    "1000 calorias em um treino!"
                )
            }

            workout.calories >= 500 -> {
                shareAchievement(workout.userId, "Queimador de Calorias", "500 calorias queimadas!")
            }
        }
    }

    private suspend fun saveAchievement(userId: String, title: String, description: String) {
        try {
            val achievement = Achievement(
                id = achievementsCollection.document().id,
                userId = userId,
                title = title,
                description = description,
                unlockedAt = System.currentTimeMillis()
            )

            achievementsCollection.document(achievement.id).set(achievement).await()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao salvar conquista", e)
        }
    }

    private suspend fun calculateChallengeProgress(userId: String, challenge: Challenge): Int {
        return try {
            when (challenge.type) {
                "steps" -> {
                    val weeklyStats = activityRepository.getWeeklyStats(userId)
                    (weeklyStats?.steps?.div(challenge.target.toDouble()) ?: 0.0).times(100).toInt()
                }

                "distance" -> {
                    val weeklyStats = activityRepository.getWeeklyStats(userId)
                    (weeklyStats?.distance?.div(challenge.target.toDouble()) ?: 0.0).times(100)
                        .toInt()
                }

                else -> 0
            }
        } catch (e: Exception) {
            0
        }
    }

    private fun formatDuration(duration: Duration): String {
        val hours = duration.toHours()
        val minutes = duration.toMinutes() % 60
        val seconds = duration.seconds % 60

        return when {
            hours > 0 -> "${hours}h ${minutes}min"
            minutes > 0 -> "${minutes}min ${seconds}s"
            else -> "${seconds}s"
        }
    }
}