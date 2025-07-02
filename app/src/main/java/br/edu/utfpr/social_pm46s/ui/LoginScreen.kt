package br.edu.utfpr.social_pm46s.ui

import android.Manifest
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.health.connect.client.PermissionController
import br.edu.utfpr.social_pm46s.data.repository.AuthRepository
import br.edu.utfpr.social_pm46s.data.repository.UserRepository
import br.edu.utfpr.social_pm46s.manager.HealthConnectPermissionManager
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object LoginScreen : Screen {
    private fun readResolve(): Any = LoginScreen

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val authRepository = AuthRepository(context)
        val userRepository = UserRepository()
        val scope = rememberCoroutineScope()

        LoginScreenContent(
            authRepository = authRepository,
            userRepository = userRepository,
            scope = scope,
            onLoginSuccess = {
                navigator.replaceAll(MainScreen)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
private fun LoginScreenContent(
    authRepository: AuthRepository,
    userRepository: UserRepository,
    scope: CoroutineScope,
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val credentialManager = CredentialManager.create(context)

    // Gerenciador de permissões Health Connect
    val healthConnectManager = remember { HealthConnectPermissionManager.getInstance(context) }
    var showHealthConnectDialog by remember { mutableStateOf(false) }
    var showLocationPermissionDialog by remember { mutableStateOf(false) }
    var isLoggedIn by remember { mutableStateOf(false) }

    // Estados das permissões
    val isHealthConnectAvailable by healthConnectManager.isHealthConnectAvailable.collectAsState()
    val healthConnectPermissionsGranted by healthConnectManager.permissionsGranted.collectAsState()

    // Permissões de localização (necessárias para fitness tracking)
    val locationPermissions = remember {
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

    val locationPermissionState =
        rememberMultiplePermissionsState(permissions = locationPermissions)

    // Launcher para permissões Health Connect
    val healthConnectPermissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { grantedPermissions ->
        val requiredPermissions = healthConnectManager.getRequiredPermissions()
        val allGranted = grantedPermissions.containsAll(requiredPermissions)

        healthConnectManager.updatePermissionStatus(allGranted)

        if (allGranted) {
            Toast.makeText(context, "Permissões Health Connect concedidas!", Toast.LENGTH_SHORT)
                .show()
        } else {
            Toast.makeText(
                context,
                "Algumas permissões não foram concedidas. O app funcionará com funcionalidades limitadas.",
                Toast.LENGTH_LONG
            ).show()
        }

        // Verificar permissões de localização após Health Connect
        if (isLoggedIn) {
            if (!locationPermissionState.allPermissionsGranted) {
                showLocationPermissionDialog = true
            } else {
                onLoginSuccess()
            }
        }
    }

    // Verificar permissões ao inicializar (se Health Connect disponível)
    LaunchedEffect(isHealthConnectAvailable) {
        if (isHealthConnectAvailable) {
            healthConnectManager.checkPermissions()
        }
    }

    // Dialog para Health Connect
    if (showHealthConnectDialog) {
        HealthConnectPermissionDialog(
            onConfirm = {
                showHealthConnectDialog = false
                val requiredPermissions = healthConnectManager.getRequiredPermissions()
                healthConnectPermissionLauncher.launch(requiredPermissions)
            },
            onSkip = {
                showHealthConnectDialog = false
                if (isLoggedIn) {
                    if (!locationPermissionState.allPermissionsGranted) {
                        showLocationPermissionDialog = true
                    } else {
                        onLoginSuccess()
                    }
                }
            },
            onDismiss = { showHealthConnectDialog = false }
        )
    }

    // Dialog para permissões de localização
    if (showLocationPermissionDialog) {
        LocationPermissionDialog(
            onConfirm = {
                showLocationPermissionDialog = false
                locationPermissionState.launchMultiplePermissionRequest()
            },
            onSkip = {
                showLocationPermissionDialog = false
                if (isLoggedIn) {
                    onLoginSuccess()
                }
            },
            onDismiss = { showLocationPermissionDialog = false }
        )
    }

    // Observar mudanças nas permissões de localização
    LaunchedEffect(locationPermissionState.allPermissionsGranted) {
        if (isLoggedIn && showLocationPermissionDialog == false && showHealthConnectDialog == false) {
            if (locationPermissionState.allPermissionsGranted) {
                onLoginSuccess()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Seção de boas-vindas
            WelcomeSection(
                modifier = Modifier.padding(bottom = 48.dp)
            )

            // Status das permissões (se logado)
            if (isLoggedIn) {
                PermissionsStatusSection(
                    isHealthConnectAvailable = isHealthConnectAvailable,
                    healthConnectPermissionsGranted = healthConnectPermissionsGranted,
                    locationPermissionsGranted = locationPermissionState.allPermissionsGranted,
                    onRequestHealthConnectPermissions = {
                        val requiredPermissions = healthConnectManager.getRequiredPermissions()
                        healthConnectPermissionLauncher.launch(requiredPermissions)
                    },
                    onRequestLocationPermissions = {
                        locationPermissionState.launchMultiplePermissionRequest()
                    },
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }

            // Botão de login
            LoginButton(
                onClick = {
                    scope.launch {
                        handleGoogleSignIn(
                            context = context,
                            authRepository = authRepository,
                            userRepository = userRepository,
                            credentialManager = credentialManager,
                            onSuccess = {
                                isLoggedIn = true

                                // Verificar permissões em sequência
                                when {
                                    isHealthConnectAvailable && !healthConnectPermissionsGranted -> {
                                        showHealthConnectDialog = true
                                    }

                                    !locationPermissionState.allPermissionsGranted -> {
                                        showLocationPermissionDialog = true
                                    }

                                    else -> {
                                        onLoginSuccess()
                                    }
                                }
                            }
                        )
                    }
                },
                modifier = Modifier.padding(top = 24.dp)
            )
        }
    }
}

@Composable
private fun PermissionsStatusSection(
    isHealthConnectAvailable: Boolean,
    healthConnectPermissionsGranted: Boolean,
    locationPermissionsGranted: Boolean,
    onRequestHealthConnectPermissions: () -> Unit,
    onRequestLocationPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Health Connect Status
        if (isHealthConnectAvailable) {
            PermissionStatusCard(
                title = if (healthConnectPermissionsGranted) "✓ Health Connect" else "⚠ Health Connect",
                description = if (healthConnectPermissionsGranted)
                    "Configurado"
                else
                    "Permissões para dados de saúde",
                isGranted = healthConnectPermissionsGranted,
                onRequestPermissions = onRequestHealthConnectPermissions
            )
        }

        // Location Permissions Status
        PermissionStatusCard(
            title = if (locationPermissionsGranted) "✓ Localização" else "⚠ Localização",
            description = if (locationPermissionsGranted)
                "Configurado"
            else
                "Necessário para rastreamento de exercícios",
            isGranted = locationPermissionsGranted,
            onRequestPermissions = onRequestLocationPermissions
        )
    }
}

@Composable
private fun PermissionStatusCard(
    title: String,
    description: String,
    isGranted: Boolean,
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Medium,
                color = if (isGranted)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onErrorContainer
            )

            if (!isGranted) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onRequestPermissions) {
                    Text("Configurar")
                }
            }
        }
    }
}

@Composable
private fun LocationPermissionDialog(
    onConfirm: () -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Permissões de Localização") },
        text = {
            Column {
                Text("Para o rastreamento de exercícios funcionar corretamente, precisamos de:")
                Spacer(modifier = Modifier.height(8.dp))
                Text("• Localização precisa (GPS)")
                Text("• Reconhecimento de atividade física")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Isso permite calcular distância, velocidade e tipo de exercício automaticamente.",
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Você pode continuar sem essas permissões, mas com funcionalidades limitadas.",
                    fontSize = 12.sp
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Permitir")
            }
        },
        dismissButton = {
            TextButton(onClick = onSkip) {
                Text("Pular")
            }
        }
    )
}

@Composable
private fun HealthConnectPermissionDialog(
    onConfirm: () -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sincronização Health Connect") },
        text = {
            Column {
                Text("O Health Connect permite sincronizar seus dados de atividade física e saúde:")
                Spacer(modifier = Modifier.height(8.dp))
                Text("• Passos e distância percorrida")
                Text("• Calorias queimadas")
                Text("• Frequência cardíaca")
                Text("• Dados de sono")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Isso é opcional - você pode continuar sem essas permissões.",
                    fontWeight = FontWeight.Medium
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Configurar")
            }
        },
        dismissButton = {
            TextButton(onClick = onSkip) {
                Text("Pular")
            }
        }
    )
}

@Composable
private fun WelcomeSection(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Bem-vindo!",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "Faça login para continuar",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun LoginButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth(0.8f)
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 8.dp,
            pressedElevation = 12.dp
        )
    ) {
        Text(
            text = "Entrar com Google",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private suspend fun handleGoogleSignIn(
    context: Context,
    authRepository: AuthRepository,
    userRepository: UserRepository,
    credentialManager: CredentialManager,
    onSuccess: () -> Unit
) {
    try {
        val googleIdOption = authRepository.createGetGoogleIdOption()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result = credentialManager.getCredential(context, request)
        val user = authRepository.signInWithGoogleCredential(result.credential)

        if (user != null) {
            try {
                userRepository.createUserIfNotExists(user)
                Toast.makeText(context, "Login realizado com sucesso!", Toast.LENGTH_SHORT).show()
                onSuccess()
            } catch (e: Exception) {
                Toast.makeText(context, "Erro ao salvar dados do usuário", Toast.LENGTH_SHORT)
                    .show()
            }
        } else {
            Toast.makeText(context, "Falha na autenticação", Toast.LENGTH_SHORT).show()
        }
    } catch (e: GetCredentialException) {
        if (e.type != "androidx.credentials.exceptions.NoCredentialException") {
            Toast.makeText(context, "Falha no login: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}