package br.edu.utfpr.social_pm46s.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import br.edu.utfpr.social_pm46s.R
import br.edu.utfpr.social_pm46s.data.model.workout.RealTimeWorkoutData
import br.edu.utfpr.social_pm46s.data.model.workout.WorkoutStats
import br.edu.utfpr.social_pm46s.data.repository.UserRepository
import br.edu.utfpr.social_pm46s.data.repository.ActivityRepository
import br.edu.utfpr.social_pm46s.tracker.FitnessTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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
    private lateinit var socialFitnessService: SocialFitnessService

    private lateinit var userRepo: UserRepository

    inner class FitnessTrackingBinder : Binder() {
        fun getService(): FitnessTrackingService = this@FitnessTrackingService
    }

    override fun onCreate() {
        super.onCreate()

        // Inicializar dependências
        val healthConnectService = HealthConnectService(this)
        fitnessTracker = FitnessTracker(this, healthConnectService)
        workoutRepository = ActivityRepository()
        socialFitnessService = SocialFitnessService(
            workoutRepository,
            userRepo
        )

        createNotificationChannel()

        // Observar dados em tempo real para atualizar notificação
//        serviceScope.launch {
//            fitnessTracker.realTimeData.collect { workoutData ->
//                workoutData?.let { updateNotification(it) }
//            }
//        }
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
//                startForeground(NOTIFICATION_ID, createWorkoutNotification(title))
                socialFitnessService.shareWorkoutStart(type, title)
            }
        }
    }

    fun stopWorkout(notes: String? = null) {
        serviceScope.launch {
            val result = fitnessTracker.finishWorkout(notes)
            result?.let { workout ->
                workoutRepository.saveWorkout(workout)
                socialFitnessService.shareWorkoutResult(workout)
            }
//            stopForeground(true)
            stopSelf()
        }
    }

    fun pauseWorkout() {
        serviceScope.launch {
            // Implementar lógica de pausa se necessário
            fitnessTracker.cancelWorkout()
            stopForeground(true)
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


    @SuppressLint("DefaultLocale")
    private fun formatWorkoutStats(stats: WorkoutStats): String {
        val duration = "${stats.duration.toMinutes()}min"
        val calories = "${stats.estimatedCalories.toInt()}kcal"

        return if (stats.distance > 0) {
            "$duration • ${String.format("%.2f", stats.distance)}km • $calories"
        } else {
            "$duration • ${stats.steps} passos • $calories"
        }
    }
}