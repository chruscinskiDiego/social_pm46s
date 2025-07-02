package br.edu.utfpr.social_pm46s.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import br.edu.utfpr.social_pm46s.R
import br.edu.utfpr.social_pm46s.data.model.workout.RealTimeWorkoutData
import br.edu.utfpr.social_pm46s.data.model.workout.WorkoutResult
import br.edu.utfpr.social_pm46s.tracker.FitnessTracker
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Duration
import java.util.Locale

class FitnessTrackingService : Service() {

    companion object {
        private const val TAG = "FitnessTrackingService"
        private const val CHANNEL_ID = "FITNESS_TRACKING_CHANNEL"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START_WORKOUT = "ACTION_START_WORKOUT"
        const val ACTION_PAUSE_WORKOUT = "ACTION_PAUSE_WORKOUT"
        const val ACTION_RESUME_WORKOUT = "ACTION_RESUME_WORKOUT"
        const val ACTION_STOP_WORKOUT = "ACTION_STOP_WORKOUT"

        const val EXTRA_EXERCISE_TYPE = "EXTRA_EXERCISE_TYPE"
        const val EXTRA_EXERCISE_TITLE = "EXTRA_EXERCISE_TITLE"
    }

    private val binder = FitnessTrackingBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Propriedades nullable para evitar UninitializedPropertyAccessException
    private var fitnessTracker: FitnessTracker? = null
    private var healthConnectService: HealthConnectService? = null
    private var notificationManager: NotificationManager? = null

    private var notificationUpdateJob: Job? = null

    // StateFlows próprios para controlar o estado
    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _realTimeData = MutableStateFlow<RealTimeWorkoutData?>(null)
    val realTimeData: StateFlow<RealTimeWorkoutData?> = _realTimeData.asStateFlow()

    inner class FitnessTrackingBinder : Binder() {
        fun getService(): FitnessTrackingService = this@FitnessTrackingService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")

        try {
            // Inicializar serviços com tratamento de erro
            healthConnectService = HealthConnectService(this)
            notificationManager = getSystemService(NotificationManager::class.java)

            createNotificationChannel()

            // Inicializar FitnessTracker depois que tudo estiver pronto
            fitnessTracker = FitnessTracker(this, healthConnectService!!)

            // Conectar StateFlows se o FitnessTracker os fornece
            serviceScope.launch {
                fitnessTracker?.let { tracker ->
                    try {
                        tracker.isTracking.collect { _isTracking.value = it }
                    } catch (e: Exception) {
                        Log.e(TAG, "Erro ao coletar isTracking: ${e.message}")
                    }
                }
            }

            serviceScope.launch {
                fitnessTracker?.let { tracker ->
                    try {
                        tracker.isPaused.collect { _isPaused.value = it }
                    } catch (e: Exception) {
                        Log.e(TAG, "Erro ao coletar isPaused: ${e.message}")
                    }
                }
            }

            serviceScope.launch {
                fitnessTracker?.let { tracker ->
                    try {
                        tracker.realTimeData.collect { _realTimeData.value = it }
                    } catch (e: Exception) {
                        Log.e(TAG, "Erro ao coletar realTimeData: ${e.message}")
                    }
                }
            }

            Log.d(TAG, "Service initialized successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Erro ao inicializar service: ${e.message}", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            when (intent?.action) {
                ACTION_START_WORKOUT -> {
                    val exerciseType = intent.getIntExtra(EXTRA_EXERCISE_TYPE, -1)
                    val exerciseTitle = intent.getStringExtra(EXTRA_EXERCISE_TITLE) ?: "Exercício"

                    if (exerciseType != -1) {
                        serviceScope.launch {
                            startWorkout(exerciseType, exerciseTitle)
                        }
                    }
                }

                ACTION_PAUSE_WORKOUT -> {
                    serviceScope.launch { pauseWorkout() }
                }

                ACTION_RESUME_WORKOUT -> {
                    serviceScope.launch { resumeWorkout() }
                }

                ACTION_STOP_WORKOUT -> {
                    serviceScope.launch { finishWorkout() }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao processar comando: ${e.message}", e)
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")

        try {
            notificationUpdateJob?.cancel()
            serviceScope.cancel()

            // Verificar se fitnessTracker foi inicializado antes de usar
            fitnessTracker?.let { tracker ->
                if (tracker.isCurrentlyTracking()) {
                    serviceScope.launch {
                        tracker.cancelWorkout()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao destruir service: ${e.message}", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager?.let { manager ->
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Rastreamento de Exercícios",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Notificações para rastreamento de exercícios em tempo real"
                    setSound(null, null)
                    enableVibration(false)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    suspend fun startWorkout(exerciseType: Int, title: String): Boolean {
        Log.d(TAG, "Starting workout: $title (type: $exerciseType)")

        return try {
            val tracker = fitnessTracker ?: return false
            val success = tracker.startWorkout(exerciseType, title)

            if (success) {
                startForeground(NOTIFICATION_ID, createWorkoutNotification())
                startNotificationUpdates()
                _isTracking.value = true
            }

            success
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao iniciar workout: ${e.message}", e)
            false
        }
    }

    suspend fun pauseWorkout(): Boolean {
        Log.d(TAG, "Pausing workout")

        return try {
            val tracker = fitnessTracker ?: return false
            val success = tracker.pauseWorkout()

            if (success) {
                updateNotification()
                _isPaused.value = true
            }

            success
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao pausar workout: ${e.message}", e)
            false
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    suspend fun resumeWorkout(): Boolean {
        Log.d(TAG, "Resuming workout")

        return try {
            val tracker = fitnessTracker ?: return false
            val success = tracker.resumeWorkout()

            if (success) {
                updateNotification()
                _isPaused.value = false
            }

            success
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao retomar workout: ${e.message}", e)
            false
        }
    }

    suspend fun finishWorkout(userNotes: String? = null): WorkoutResult? {
        Log.d(TAG, "Finishing workout")

        return try {
            val tracker = fitnessTracker ?: return null
            val result = tracker.finishWorkout(userNotes)

            stopNotificationUpdates()
            stopForeground(STOP_FOREGROUND_REMOVE)

            _isTracking.value = false
            _isPaused.value = false
            _realTimeData.value = null

            result
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao finalizar workout: ${e.message}", e)
            null
        }
    }

    suspend fun cancelWorkout(): Boolean {
        Log.d(TAG, "Cancelling workout")

        return try {
            val tracker = fitnessTracker ?: return false
            val success = tracker.cancelWorkout()

            if (success) {
                stopNotificationUpdates()
                stopForeground(STOP_FOREGROUND_REMOVE)

                _isTracking.value = false
                _isPaused.value = false
                _realTimeData.value = null
            }

            success
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao cancelar workout: ${e.message}", e)
            false
        }
    }

    // Getters para informações do workout
    fun getCurrentExerciseType(): Int? = fitnessTracker?.getCurrentExerciseType()
    fun getCurrentExerciseTitle(): String? = fitnessTracker?.getCurrentExerciseTitle()
    fun isCurrentlyTracking(): Boolean = fitnessTracker?.isCurrentlyTracking() ?: false
    fun isCurrentlyPaused(): Boolean = fitnessTracker?.isCurrentlyPaused() ?: false

    private fun startNotificationUpdates() {
        notificationUpdateJob?.cancel()
        notificationUpdateJob = serviceScope.launch {
            _realTimeData.collect { workoutData ->
                if (workoutData != null) {
                    updateNotification()
                }
            }
        }
    }

    private fun stopNotificationUpdates() {
        notificationUpdateJob?.cancel()
        notificationUpdateJob = null
    }

    private fun createWorkoutNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Exercício em Andamento")
        .setContentText("Preparando...")
        .setSmallIcon(android.R.drawable.ic_media_play)
        .setOngoing(true)
        .setSilent(true)
        .setContentIntent(createContentIntent())
        .addAction(createPauseResumeAction())
        .addAction(createStopAction())
        .build()

    private fun updateNotification() {
        val workoutData = _realTimeData.value ?: return
        val manager = notificationManager ?: return

        val title = if (_isPaused.value) {
            "${workoutData.title} - PAUSADO"
        } else {
            workoutData.title
        }

        val content = buildString {
            append("⏱️ ${formatDuration(workoutData.stats.duration)}")
            if (workoutData.stats.distance > 0) {
                append(" • 📍 ${"%.2f".format(workoutData.stats.distance)}km")
            }
            if (workoutData.stats.steps > 0) {
                append(" • 👟 ${workoutData.stats.steps}")
            }
            append(" • 🔥 ${workoutData.stats.estimatedCalories.toInt()}kcal")
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(createContentIntent())
            .addAction(createPauseResumeAction())
            .addAction(createStopAction())
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun createContentIntent(): PendingIntent {
        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: Intent()

        return PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createPauseResumeAction(): NotificationCompat.Action {
        val isPaused = _isPaused.value
        val actionText = if (isPaused) "Retomar" else "Pausar"
        val actionIntent = if (isPaused) ACTION_RESUME_WORKOUT else ACTION_PAUSE_WORKOUT

        val intent = Intent(this, FitnessTrackingService::class.java).apply {
            action = actionIntent
        }
        val pendingIntent = PendingIntent.getService(
            this, 1, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Action.Builder(
            android.R.drawable.ic_media_pause,
            actionText,
            pendingIntent
        ).build()
    }

    private fun createStopAction(): NotificationCompat.Action {
        val intent = Intent(this, FitnessTrackingService::class.java).apply {
            action = ACTION_STOP_WORKOUT
        }
        val pendingIntent = PendingIntent.getService(
            this, 2, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_close_clear_cancel,
            "Parar",
            pendingIntent
        ).build()
    }

    private fun formatDuration(duration: Duration): String {
        val hours = duration.toHours()
        val minutes = duration.toMinutes() % 60
        val seconds = duration.seconds % 60

        return if (hours > 0) {
            String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }
}