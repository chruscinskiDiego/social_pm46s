package br.edu.utfpr.social_pm46s.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.edu.utfpr.social_pm46s.data.repository.AuthRepository
import br.edu.utfpr.social_pm46s.data.repository.UserRepository
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object MainScreen : Screen {
    private fun readResolve(): Any = MainScreen

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val authRepository = AuthRepository(context)
        val userRepository = UserRepository()
        val scope = rememberCoroutineScope()

        MainScreenContent(
            currentUser = authRepository.getCurrentUser(),
            authRepository = authRepository,
            userRepository = userRepository,
            scope = scope,
            onSignOutClick = {
                scope.launch {
                    handleSignOut(
                        context = context,
                        authRepository = authRepository,
                        onSuccess = {
                            navigator.replaceAll(LoginScreen)
                        }
                    )
                }
            },
            onServiceClick = { serviceType ->
                when (serviceType) {
                    ServiceType.MONITORING -> navigator.push(MonitoringScreen)
                    ServiceType.SOCIAL -> {
                        // Tela não implementada ainda
                        showToast(context, "Funcionalidade em desenvolvimento")
                    }
                    ServiceType.PROFILE -> {
                        // Tela não implementada ainda
                        showToast(context, "Funcionalidade em desenvolvimento")
                    }
                    ServiceType.HISTORY -> {
                        // Tela não implementada ainda
                        showToast(context, "Funcionalidade em desenvolvimento")
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreenContent(
    currentUser: FirebaseUser?,
    authRepository: AuthRepository,
    userRepository: UserRepository,
    scope: CoroutineScope,
    onSignOutClick: () -> Unit,
    onServiceClick: (ServiceType) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Dashboard",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onSignOutClick) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Sair"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                // Seção de boas-vindas
                WelcomeSection(currentUser = currentUser)
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outline
                )
            }

            item {
                // Título dos serviços
                Text(
                    text = "Serviços Disponíveis",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Cards dos serviços
            items(getAvailableServices()) { service ->
                ServiceCard(
                    service = service,
                    onClick = { onServiceClick(service.type) }
                )
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.outline
                )
            }

            item {
                // Botões de teste (removíveis em produção)
                TestButtonsSection(
                    scope = scope,
                    userRepository = userRepository
                )
            }
        }
    }
}

@Composable
private fun WelcomeSection(
    currentUser: FirebaseUser?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Bem-vindo!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            currentUser?.let { user ->
                Text(
                    text = user.displayName ?: "Usuário",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Text(
                    text = user.email ?: "",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun ServiceCard(
    service: ServiceInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp,
            pressedElevation = 8.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Ícone do serviço
            Surface(
                modifier = Modifier.size(48.dp),
                shape = MaterialTheme.shapes.medium,
                color = service.color
            ) {
                Icon(
                    imageVector = service.icon,
                    contentDescription = service.title,
                    modifier = Modifier.padding(12.dp),
                    tint = androidx.compose.ui.graphics.Color.White
                )
            }

            // Informações do serviço
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = service.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = service.description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Ícone de navegação
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Acessar",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TestButtonsSection(
    scope: CoroutineScope,
    userRepository: UserRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Testes (Desenvolvimento)",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    scope.launch {
                        handleFirestoreTest(context, userRepository)
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Firestore")
            }

            OutlinedButton(
                onClick = {
                    scope.launch {
                        handleRealtimeTest(context)
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Realtime")
            }
        }
    }
}

// Data classes para os serviços
enum class ServiceType {
    MONITORING, SOCIAL, PROFILE, HISTORY
}

data class ServiceInfo(
    val type: ServiceType,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: androidx.compose.ui.graphics.Color
)

private fun getAvailableServices(): List<ServiceInfo> {
    return listOf(
        ServiceInfo(
            type = ServiceType.MONITORING,
            title = "Monitoramento",
            description = "Inicie e monitore seus exercícios em tempo real",
            icon = Icons.Default.DirectionsRun,
            color = androidx.compose.ui.graphics.Color(0xFF4CAF50)
        ),
        ServiceInfo(
            type = ServiceType.SOCIAL,
            title = "Social",
            description = "Conecte-se com amigos e compartilhe suas atividades",
            icon = Icons.Default.Group,
            color = androidx.compose.ui.graphics.Color(0xFF2196F3)
        ),
        ServiceInfo(
            type = ServiceType.PROFILE,
            title = "Perfil",
            description = "Gerencie suas informações pessoais e configurações",
            icon = Icons.Default.Person,
            color = androidx.compose.ui.graphics.Color(0xFF9C27B0)
        ),
        ServiceInfo(
            type = ServiceType.HISTORY,
            title = "Histórico",
            description = "Visualize o histórico de seus exercícios e progresso",
            icon = Icons.Default.Assessment,
            color = androidx.compose.ui.graphics.Color(0xFFFF9800)
        )
    )
}

private suspend fun handleSignOut(
    context: Context,
    authRepository: AuthRepository,
    onSuccess: () -> Unit
) {
    try {
        authRepository.signOut()
        showToast(context, "Logout realizado com sucesso!")
        onSuccess()
    } catch (e: Exception) {
        showToast(context, "Erro ao fazer logout: ${e.message}")
    }
}

private suspend fun handleFirestoreTest(
    context: Context,
    userRepository: UserRepository
) {
    try {
        val users = userRepository.getAllUsers()
        showToast(context, "Firestore: ${users.size} usuários encontrados")
    } catch (e: Exception) {
        showToast(context, "Erro no Firestore: ${e.message}")
    }
}

private suspend fun handleRealtimeTest(context: Context) {
    try {
        showToast(context, "Teste do Realtime DB executado!")
    } catch (e: Exception) {
        showToast(context, "Erro no Realtime: ${e.message}")
    }
}

private fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}