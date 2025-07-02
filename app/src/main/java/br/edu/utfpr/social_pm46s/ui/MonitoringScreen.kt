package br.edu.utfpr.social_pm46s.ui

import android.Manifest
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
import br.edu.utfpr.social_pm46s.data.repository.ActivityRepository
import br.edu.utfpr.social_pm46s.data.repository.AuthRepository
import br.edu.utfpr.social_pm46s.service.HealthConnectService
import br.edu.utfpr.social_pm46s.tracker.FitnessTracker
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import android.os.Build
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

object MonitoringScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current.applicationContext
        val scope = rememberCoroutineScope()

        val healthConnectService = HealthConnectService(context)
        val fitnessTracker = FitnessTracker(context, healthConnectService)
        val activityRepository = ActivityRepository()
        val authRepository = AuthRepository(context)

        MonitoringScreenContent(
            scope = scope,
            fitnessTracker = fitnessTracker,
            activityRepository = activityRepository,
            authRepository = authRepository,
            onBackClick = { navigator.pop() }
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun MonitoringScreenContent(
    scope: CoroutineScope,
    fitnessTracker: FitnessTracker,
    activityRepository: ActivityRepository,
    authRepository: AuthRepository,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isTracking by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var selectedWorkoutType by remember { mutableStateOf(HealthConnectService.EXERCISE_TYPE_WALKING) }

    val realTimeData by fitnessTracker.realTimeData.collectAsState()
    val context = LocalContext.current

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
                val title = when (selectedWorkoutType) {
                    HealthConnectService.EXERCISE_TYPE_WALKING -> "Caminhada"
                    HealthConnectService.EXERCISE_TYPE_RUNNING -> "Corrida"
                    HealthConnectService.EXERCISE_TYPE_CYCLING -> "Ciclismo"
                    else -> "Exercício"
                }

                try {
                    isTracking = fitnessTracker.startWorkout(selectedWorkoutType, title)
                    if (!isTracking) {
                        Toast.makeText(context, "Não foi possível iniciar o treino.", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: SecurityException) {
                    Toast.makeText(context, "Permissão negada: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            permissionState.launchMultiplePermissionRequest()
        }
    }

    val onStopClick: () -> Unit = {
        scope.launch {
            isSaving = true
            val workoutResult = fitnessTracker.finishWorkout(userNotes = null)

            if (workoutResult != null) {
                val userId = authRepository.getCurrentUser()?.uid
                if (userId != null) {
                    val finalResult = workoutResult.copy(userId = userId)
                    val saved = activityRepository.saveWorkout(finalResult)
                    if (saved) {
                        Toast.makeText(context, "Treino salvo com sucesso!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Erro ao salvar o treino.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Erro: Usuário não encontrado.", Toast.LENGTH_SHORT).show()
                }
            }
            isSaving = false
            isTracking = false
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.primary
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HeaderSection(isTracking = isTracking)
            Spacer(modifier = Modifier.height(16.dp))

            MonitoringCard(
                isTracking = isTracking,
                isSaving = isSaving,
                realTimeData = realTimeData,
                selectedWorkoutType = selectedWorkoutType,
                hasPermissions = permissionState.allPermissionsGranted,
                onWorkoutTypeSelected = { selectedWorkoutType = it },
                onStartClick = onStartClick,
                onStopClick = onStopClick,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.height(16.dp))
            BackButton(onClick = onBackClick, isTracking = isTracking)
        }
    }
}

@Composable
private fun HeaderSection(isTracking: Boolean) {
    Text(
        text = if (isTracking) "ATIVIDADE EM CURSO" else "INICIAR ATIVIDADE",
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onPrimary
    )
}

@Composable
private fun MonitoringCard(
    isTracking: Boolean,
    isSaving: Boolean,
    realTimeData: RealTimeWorkoutData?,
    selectedWorkoutType: Int,
    hasPermissions: Boolean,
    onWorkoutTypeSelected: (Int) -> Unit,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        when {
            isSaving -> SavingContent()
            isTracking -> TrackingContent(realTimeData, onStopClick)
            else -> PreStartContent(selectedWorkoutType, hasPermissions, onWorkoutTypeSelected, onStartClick)
        }
    }
}

@Composable
private fun PreStartContent(
    selectedWorkoutType: Int,
    hasPermissions: Boolean,
    onWorkoutTypeSelected: (Int) -> Unit,
    onStartClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Selecione o tipo de exercício:", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selectedWorkoutType == HealthConnectService.EXERCISE_TYPE_WALKING,
                onClick = { onWorkoutTypeSelected(HealthConnectService.EXERCISE_TYPE_WALKING) },
                label = { Text("Caminhada") }
            )
            FilterChip(
                selected = selectedWorkoutType == HealthConnectService.EXERCISE_TYPE_RUNNING,
                onClick = { onWorkoutTypeSelected(HealthConnectService.EXERCISE_TYPE_RUNNING) },
                label = { Text("Corrida") }
            )
            FilterChip(
                selected = selectedWorkoutType == HealthConnectService.EXERCISE_TYPE_CYCLING,
                onClick = { onWorkoutTypeSelected(HealthConnectService.EXERCISE_TYPE_CYCLING) },
                label = { Text("Ciclismo") }
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onStartClick,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text(
                text = if (hasPermissions) "INICIAR" else "CONCEDER PERMISSÃO",
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun TrackingContent(data: RealTimeWorkoutData?, onStopClick: () -> Unit) {
    val duration = data?.stats?.duration ?: Duration.ZERO
    val distance = data?.stats?.distance ?: 0.0
    val calories = data?.stats?.estimatedCalories ?: 0.0
    val steps = data?.stats?.steps ?: 0

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Text(formatDuration(duration), fontSize = 64.sp, fontWeight = FontWeight.Light)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            StatItem(label = "Passos", value = steps.toString())
            StatItem(label = "Distância", value = String.format("%.2f km", distance))
            StatItem(label = "Calorias", value = String.format("%.0f kcal", calories))
        }

        Button(
            onClick = onStopClick,
            modifier = Modifier.fillMaxWidth(0.8f).height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("PARAR E SALVAR", fontSize = 16.sp)
        }
    }
}

@Composable
private fun SavingContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator()
            Text("Salvando seu treino...", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun BackButton(onClick: () -> Unit, isTracking: Boolean) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = !isTracking,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Text("Voltar ao Menu", modifier = Modifier.padding(vertical = 8.dp))
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