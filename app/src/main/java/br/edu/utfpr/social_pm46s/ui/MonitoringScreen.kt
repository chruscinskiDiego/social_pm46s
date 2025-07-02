package br.edu.utfpr.social_pm46s.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.edu.utfpr.social_pm46s.data.model.workout.RealTimeWorkoutData
import br.edu.utfpr.social_pm46s.service.FitnessTrackingService
import br.edu.utfpr.social_pm46s.service.HealthConnectService
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Duration

object MonitoringScreen : Screen {
    private fun readResolve(): Any = MonitoringScreen

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()

        // Obter usuário atual diretamente do Firebase Auth
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser == null) {
            LaunchedEffect(Unit) {
                navigator.replace(LoginScreen)
            }
            return
        }

        MonitoringScreenContent(
            scope = scope,
            currentUser = currentUser,
            onBackClick = { navigator.pop() }
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun MonitoringScreenContent(
    scope: CoroutineScope,
    currentUser: FirebaseUser,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isTracking by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var selectedWorkoutType by remember { mutableStateOf(HealthConnectService.EXERCISE_TYPE_WALKING) }
    var fitnessService by remember { mutableStateOf<FitnessTrackingService?>(null) }
    var realTimeData by remember { mutableStateOf<RealTimeWorkoutData?>(null) }

    val context = LocalContext.current

    // Service Connection
    val serviceConnection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service as FitnessTrackingService.FitnessTrackingBinder
                fitnessService = binder.getService()

                // Coletar estados usando StateFlow
                scope.launch {
                    fitnessService?.isTracking?.collect { tracking ->
                        isTracking = tracking
                    }
                }

                scope.launch {
                    fitnessService?.isPaused?.collect { paused ->
                        isPaused = paused
                    }
                }

                scope.launch {
                    fitnessService?.realTimeData?.collect { data ->
                        realTimeData = data
                    }
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                fitnessService = null
            }
        }
    }

    // Conectar ao service quando a tela é criada
    LaunchedEffect(Unit) {
        val intent = Intent(context, FitnessTrackingService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    // Desconectar quando a tela é destruída
    DisposableEffect(Unit) {
        onDispose {
            try {
                context.unbindService(serviceConnection)
            } catch (e: Exception) {
                // Service já pode ter sido desconectado
            }
        }
    }

    val permissionsToRequest = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACTIVITY_RECOGNITION
            )
        } else {
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
    }
    val permissionState = rememberMultiplePermissionsState(permissions = permissionsToRequest)

    val onStartClick: () -> Unit = {
        if (permissionState.allPermissionsGranted) {
            scope.launch {
                val workoutTitle = when (selectedWorkoutType) {
                    HealthConnectService.EXERCISE_TYPE_RUNNING -> "Corrida"
                    HealthConnectService.EXERCISE_TYPE_WALKING -> "Caminhada"
                    HealthConnectService.EXERCISE_TYPE_CYCLING -> "Ciclismo"
                    else -> "Exercício"
                }

                // Usar as constantes corretas do FitnessTrackingService
                val startIntent = Intent(context, FitnessTrackingService::class.java).apply {
                    action = FitnessTrackingService.ACTION_START_WORKOUT
                    putExtra(FitnessTrackingService.EXTRA_EXERCISE_TYPE, selectedWorkoutType)
                    putExtra(FitnessTrackingService.EXTRA_EXERCISE_TITLE, workoutTitle)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(startIntent)
                } else {
                    context.startService(startIntent)
                }

                Toast.makeText(
                    context,
                    "Exercício iniciado! Você pode sair do app que continuará monitorando.",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            permissionState.launchMultiplePermissionRequest()
        }
    }

    val onPauseResumeClick: () -> Unit = {
        scope.launch {
            val action = if (isPaused) {
                FitnessTrackingService.ACTION_RESUME_WORKOUT
            } else {
                FitnessTrackingService.ACTION_PAUSE_WORKOUT
            }

            val intent = Intent(context, FitnessTrackingService::class.java).apply {
                this.action = action
            }
            context.startService(intent)

            val message = if (isPaused) "Exercício retomado!" else "Exercício pausado!"
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    val onStopClick: () -> Unit = {
        scope.launch {
            isSaving = true

            // Parar via Intent
            val stopIntent = Intent(context, FitnessTrackingService::class.java).apply {
                action = FitnessTrackingService.ACTION_STOP_WORKOUT
            }
            context.startService(stopIntent)

            Toast.makeText(
                context,
                "Exercício finalizado e salvo com sucesso!",
                Toast.LENGTH_LONG
            ).show()

            // Delay para permitir salvamento
            kotlinx.coroutines.delay(2000)
            isSaving = false
            isTracking = false
            isPaused = false
            realTimeData = null
        }
    }

    Scaffold(
        topBar = {
            HeaderSection(
                isTracking = isTracking,
                userName = currentUser.displayName ?: "Usuário",
                modifier = Modifier.fillMaxWidth()
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MonitoringCard(
                isTracking = isTracking,
                isSaving = isSaving,
                isPaused = isPaused,
                selectedWorkoutType = selectedWorkoutType,
                onWorkoutTypeChange = { selectedWorkoutType = it },
                realTimeData = realTimeData,
                onStartClick = onStartClick,
                onPauseResumeClick = onPauseResumeClick,
                onStopClick = onStopClick
            )

            BackButton(
                onClick = onBackClick,
                isTracking = isTracking
            )
        }
    }
}

@Composable
private fun HeaderSection(
    isTracking: Boolean,
    userName: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isTracking) "Monitorando Exercício" else "Iniciar Exercício",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Olá, $userName!",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MonitoringCard(
    isTracking: Boolean,
    isSaving: Boolean,
    isPaused: Boolean,
    selectedWorkoutType: Int,
    onWorkoutTypeChange: (Int) -> Unit,
    realTimeData: RealTimeWorkoutData?,
    onStartClick: () -> Unit,
    onPauseResumeClick: () -> Unit,
    onStopClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        when {
            isSaving -> SavingContent()
            isTracking -> TrackingContent(
                data = realTimeData,
                isPaused = isPaused,
                onPauseResumeClick = onPauseResumeClick,
                onStopClick = onStopClick
            )

            else -> PreStartContent(
                selectedWorkoutType = selectedWorkoutType,
                onWorkoutTypeChange = onWorkoutTypeChange,
                onStartClick = onStartClick
            )
        }
    }
}

@Composable
private fun PreStartContent(
    selectedWorkoutType: Int,
    onWorkoutTypeChange: (Int) -> Unit,
    onStartClick: () -> Unit
) {
    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Escolha o tipo de exercício",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedWorkoutType == HealthConnectService.EXERCISE_TYPE_WALKING,
                    onClick = { onWorkoutTypeChange(HealthConnectService.EXERCISE_TYPE_WALKING) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Caminhada", style = MaterialTheme.typography.bodyLarge)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedWorkoutType == HealthConnectService.EXERCISE_TYPE_RUNNING,
                    onClick = { onWorkoutTypeChange(HealthConnectService.EXERCISE_TYPE_RUNNING) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Corrida", style = MaterialTheme.typography.bodyLarge)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedWorkoutType == HealthConnectService.EXERCISE_TYPE_CYCLING,
                    onClick = { onWorkoutTypeChange(HealthConnectService.EXERCISE_TYPE_CYCLING) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ciclismo", style = MaterialTheme.typography.bodyLarge)
            }
        }

        Button(
            onClick = onStartClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Iniciar Exercício", fontSize = 16.sp)
        }
    }
}

@Composable
private fun TrackingContent(
    data: RealTimeWorkoutData?,
    isPaused: Boolean,
    onPauseResumeClick: () -> Unit,
    onStopClick: () -> Unit
) {
    val duration = data?.stats?.duration ?: Duration.ZERO
    val distance = data?.stats?.distance ?: 0.0
    val calories = data?.stats?.estimatedCalories ?: 0.0
    val steps = data?.stats?.steps ?: 0

    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = if (isPaused) "Exercício Pausado" else "Exercício em Andamento",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = if (isPaused) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.primary
        )

        if (isPaused) {
            Text(
                text = "Toque em 'Continuar' para retomar ou use a notificação.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem("Tempo", formatDuration(duration))
            StatItem("Distância", "${String.format("%.2f", distance)} km")
            StatItem("Calorias", "${calories.toInt()} kcal")
            StatItem("Passos", steps.toString())
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onPauseResumeClick,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPaused) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.secondary
                )
            ) {
                Text(if (isPaused) "Continuar" else "Pausar")
            }

            Button(
                onClick = onStopClick,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Parar")
            }
        }

        if (!isPaused) {
            Text(
                text = "💡 Você pode sair do app que o monitoramento continuará em segundo plano",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SavingContent() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = "Salvando exercício...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BackButton(onClick: () -> Unit, isTracking: Boolean) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = !isTracking
    ) {
        Text(
            text = if (isTracking) "Finalize o exercício para voltar" else "Voltar ao Dashboard",
            color = if (isTracking) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.primary
        )
    }
}

private fun formatDuration(duration: Duration): String {
    val seconds = duration.seconds
    val minutes = seconds / 60
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    val remainingSeconds = seconds % 60

    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, remainingMinutes, remainingSeconds)
    } else {
        String.format("%02d:%02d", remainingMinutes, remainingSeconds)
    }
}