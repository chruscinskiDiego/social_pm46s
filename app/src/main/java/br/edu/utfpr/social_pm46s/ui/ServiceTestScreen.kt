package br.edu.utfpr.social_pm46s.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.edu.utfpr.social_pm46s.data.repository.ActivityRepository
import br.edu.utfpr.social_pm46s.data.repository.AuthRepository
import br.edu.utfpr.social_pm46s.data.repository.UserRepository
import br.edu.utfpr.social_pm46s.service.FitnessTrackingService
import br.edu.utfpr.social_pm46s.service.HealthConnectService
import br.edu.utfpr.social_pm46s.service.LocationTrackingService
import br.edu.utfpr.social_pm46s.service.SocialFitnessService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceTestScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Services
    var healthConnectService by remember { mutableStateOf<HealthConnectService?>(null) }
    var socialFitnessService by remember { mutableStateOf<SocialFitnessService?>(null) }
    var fitnessTrackingService by remember { mutableStateOf<FitnessTrackingService?>(null) }

    // Service connection para FitnessTrackingService
    val serviceConnection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service as FitnessTrackingService.FitnessTrackingBinder
                fitnessTrackingService = binder.getService()
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                fitnessTrackingService = null
            }
        }
    }

    // Estados
    var testResults by remember { mutableStateOf("") }
    var isTracking by remember { mutableStateOf(false) }
    val realTimeData by fitnessTrackingService?.getRealTimeData()?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(null) }

    // Inicializar services
    LaunchedEffect(Unit) {
        healthConnectService = HealthConnectService(context)

        val authRepository = AuthRepository(context)
        val activityRepository = ActivityRepository()
        val userRepository = UserRepository()

        socialFitnessService = SocialFitnessService(activityRepository, userRepository)

        // Bind ao FitnessTrackingService
        val intent = Intent(context, FitnessTrackingService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    DisposableEffect(Unit) {
        onDispose {
            context.unbindService(serviceConnection)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Teste de Services") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Health Connect Service Tests
            ServiceTestSection(
                title = "Health Connect Service",
                tests = listOf(
                    "Verificar Disponibilidade" to {
                        scope.launch {
                            val isAvailable = healthConnectService?.isAvailable() ?: false
                            testResults += "Health Connect disponível: $isAvailable\n"
                        }
                    },
                    "Verificar Permissões" to {
                        scope.launch {
                            val hasPermissions =
                                healthConnectService?.checkAllPermissions() ?: false
                            testResults += "Permissões concedidas: $hasPermissions\n"
                        }
                    },
                    "Obter Dados de Saúde" to {
                        scope.launch {
                            val healthData = healthConnectService?.getAllHealthData()
                            testResults += "Dados obtidos: ${healthData != null}\n"
                            healthData?.let {
                                testResults += "Passos: ${it.aggregated?.totalSteps ?: 0}\n"
                                testResults += "Distância: ${it.aggregated?.totalDistanceKm ?: 0.0}km\n"
                            }
                        }
                    },
                    "Iniciar Exercício de Teste" to {
                        scope.launch {
                            val started =
                                healthConnectService?.startWalking("Teste de Caminhada") ?: false
                            testResults += "Exercício iniciado: $started\n"
                        }
                    }
                )
            )

            // Social Fitness Service Tests
            ServiceTestSection(
                title = "Social Fitness Service",
                tests = listOf(
                    "Buscar Leaderboard" to {
                        scope.launch {
                            val leaderboard = socialFitnessService?.getLeaderboard() ?: emptyList()
                            testResults += "Leaderboard obtido: ${leaderboard.size} usuários\n"
                        }
                    },
                    "Buscar Desafios" to {
                        scope.launch {
                            val authRepository = AuthRepository(context)
                            val userId = authRepository.getCurrentUser()?.uid
                            if (userId != null) {
                                val challenges =
                                    socialFitnessService?.getChallenges(userId) ?: emptyList()
                                testResults += "Desafios encontrados: ${challenges.size}\n"
                            } else {
                                testResults += "Usuário não logado para buscar desafios\n"
                            }
                        }
                    },
                    "Compartilhar Conquista" to {
                        scope.launch {
                            val authRepository = AuthRepository(context)
                            val userId = authRepository.getCurrentUser()?.uid
                            if (userId != null) {
                                socialFitnessService?.shareAchievement(
                                    userId,
                                    "Teste de Service",
                                    "Testando o sistema de conquistas"
                                )
                                testResults += "Conquista compartilhada com sucesso\n"
                            } else {
                                testResults += "Usuário não logado para compartilhar conquista\n"
                            }
                        }
                    }
                )
            )

            // Fitness Tracking Service Tests
            ServiceTestSection(
                title = "Fitness Tracking Service",
                tests = listOf(
                    "Verificar Status" to {
                        scope.launch {
                            isTracking = fitnessTrackingService?.isTracking()?.value ?: false
                            testResults += "Serviço ativo: ${fitnessTrackingService != null}\n"
                            testResults += "Rastreando: $isTracking\n"
                        }
                    },
                    "Iniciar Treino de Teste" to {
                        scope.launch {
                            fitnessTrackingService?.startWorkout(
                                HealthConnectService.EXERCISE_TYPE_WALKING,
                                "Teste de Treino"
                            )
                            testResults += "Treino de teste iniciado\n"
                        }
                    },
                    "Parar Treino" to {
                        scope.launch {
                            fitnessTrackingService?.stopWorkout("Teste finalizado")
                            testResults += "Treino de teste finalizado\n"
                        }
                    }
                )
            )

            // Location Tracking Service Tests
            ServiceTestSection(
                title = "Location Tracking Service",
                tests = listOf(
                    "Iniciar Rastreamento" to {
                        scope.launch {
                            val intent =
                                Intent(context, LocationTrackingService::class.java).apply {
                                    action = "START_LOCATION_TRACKING"
                                }
                            context.startService(intent)
                            testResults += "Rastreamento de localização iniciado\n"
                        }
                    },
                    "Parar Rastreamento" to {
                        scope.launch {
                            val intent =
                                Intent(context, LocationTrackingService::class.java).apply {
                                    action = "STOP_LOCATION_TRACKING"
                                }
                            context.startService(intent)
                            testResults += "Rastreamento de localização parado\n"
                        }
                    }
                )
            )

            // Real-time data display
            if (realTimeData != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "Dados em Tempo Real",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        realTimeData?.let { data ->
                            Text("Título: ${data.title}")
                            Text("Duração: ${data.stats.duration.toMinutes()}min")
                            Text("Calorias: ${data.stats.estimatedCalories.toInt()}kcal")
                            Text("Passos: ${data.stats.steps}")
                            if (data.stats.distance > 0) {
                                Text("Distância: ${"%.2f".format(data.stats.distance)}km")
                            }
                        }
                    }
                }
            }

            // Results display
            if (testResults.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Resultados dos Testes",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(
                                onClick = { testResults = "" }
                            ) {
                                Text("Limpar")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            testResults,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceTestSection(
    title: String,
    tests: List<Pair<String, () -> Unit>>
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            tests.forEach { (testName, testAction) ->
                Button(
                    onClick = testAction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(testName)
                }
            }
        }
    }
}