package br.edu.utfpr.social_pm46s.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import br.edu.utfpr.social_pm46s.R
import br.edu.utfpr.social_pm46s.data.model.workout.RealTimeWorkoutData
import br.edu.utfpr.social_pm46s.data.model.workout.WorkoutStats
import br.edu.utfpr.social_pm46s.data.repository.UserRepository
import br.edu.utfpr.social_pm46s.data.repository.ActivityRepository
import br.edu.utfpr.social_pm46s.data.repository.AuthRepository
import br.edu.utfpr.social_pm46s.tracker.FitnessTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant

class FitnessTrackingService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "fitness_tracking_channel"

        const val ACTION_START_WORKOUT = "start_workout"
        const val ACTION_STOP_WORKOUT = "stop_workout"
        const val ACTION_PAUSE_WORKOUT = "pause_workout"

        const val EXTRA_WORKOUT_TYPE = "workout_type"
        const val EXTRA_WORKOUT_TITLE = "workout_title"
    }

    private val binder = FitnessTrackingBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private lateinit var fitnessTracker: FitnessTracker
    private lateinit var workoutRepository: ActivityRepository
    private var socialFitnessService: SocialFitnessService? = null

    private lateinit var userRepo: UserRepository
    private lateinit var authRepository: AuthRepository
    private var currentUserId: String? = null

    inner class FitnessTrackingBinder : Binder() {
        fun getService(): FitnessTrackingService = this@FitnessTrackingService
    }

    override fun onCreate() {
        super.onCreate()

        val healthConnectService = HealthConnectService(this)
        fitnessTracker = FitnessTracker(this, healthConnectService)
        workoutRepository = ActivityRepository()
        userRepo = UserRepository()
        authRepository = AuthRepository(this)

        initializeSocialFitnessService()

        createNotificationChannel()

        serviceScope.launch {
            fitnessTracker.realTimeData.collect { workoutData ->
                workoutData?.let { updateNotification(it) }
            }
        }
    }

    private fun initializeSocialFitnessService() {
        val currentUser = authRepository.getCurrentUser()
        currentUser?.uid?.let { userId ->
            currentUserId = userId
            socialFitnessService = SocialFitnessService(
                workoutRepository,
                userRepo
            )
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_WORKOUT -> {
                val type = intent.getIntExtra(EXTRA_WORKOUT_TYPE, 0)
                val title = intent.getStringExtra(EXTRA_WORKOUT_TITLE) ?: "Exercício"
                startWorkout(type, title)
            }

            ACTION_STOP_WORKOUT -> stopWorkout()
            ACTION_PAUSE_WORKOUT -> pauseWorkout()
        }

        return START_STICKY
    }

    fun startWorkout(type: Int, title: String) {
        serviceScope.launch {
            val started = fitnessTracker.startWorkout(type, title)
            if (started) {
                val notification = createWorkoutNotification(title)
                startForeground(NOTIFICATION_ID, notification)

                currentUserId?.let { userId ->
                    socialFitnessService?.shareWorkoutStart(type, title, userId)
                }
            }
        }
    }

    fun stopWorkout(notes: String? = null) {
        serviceScope.launch {
            val result = fitnessTracker.finishWorkout(notes)
            result?.let { workout ->
                workoutRepository.saveWorkout(workout)
                socialFitnessService?.shareWorkoutResult(workout)
            }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    fun pauseWorkout() {
        serviceScope.launch {
            fitnessTracker.cancelWorkout()
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
    }

    fun getRealTimeData(): StateFlow<RealTimeWorkoutData?> = fitnessTracker.realTimeData
    fun isTracking(): StateFlow<Boolean> = fitnessTracker.isTracking

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Rastreamento de Exercícios",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notificações para exercícios em andamento"
            setShowBadge(false)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createWorkoutNotification(title: String): Notification {
        val stopIntent = Intent(this, FitnessTrackingService::class.java).apply {
            action = ACTION_STOP_WORKOUT
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Exercício em andamento")
            .setContentText(title)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_pause, "Parar", stopPendingIntent)
            .build()
    }

    private fun updateNotification(workoutData: RealTimeWorkoutData) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Exercício em andamento")
            .setContentText(formatWorkoutData(workoutData))
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    @SuppressLint("DefaultLocale")
    private fun formatWorkoutData(data: RealTimeWorkoutData): String {
        val stats = data.stats
        val startTime = data.startTime
        val currentTime = Instant.now()

        val durationMillis = java.time.Duration.between(startTime, currentTime).toMillis()
        val duration = "${(durationMillis / 60000)}min"

        val calories = "${stats.estimatedCalories.toInt()}kcal"
        val distance = stats.distance
        val steps = stats.steps

        return if (distance > 0) {
            "$duration • ${String.format("%.2f", distance)}km • $calories"
        } else {
            "$duration • $steps passos • $calories"
        }
    }

    @SuppressLint("DefaultLocale")
    private fun formatWorkoutStats(stats: WorkoutStats): String {
        val duration = "${stats.duration.toMinutes()}min"
        val calories = "${stats.estimatedCalories.toInt()}kcal"
        val distance = stats.distance
        val steps = stats.steps

        return if (distance > 0) {
            "$duration • ${String.format("%.2f", distance)}km • $calories"
        } else {
            "$duration • $steps passos • $calories"
        }
    }
}