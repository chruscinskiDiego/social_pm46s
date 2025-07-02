package br.edu.utfpr.social_pm46s.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            onNavigateToServices = {
                navigator.push(ServicesScreen)
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
    onNavigateToServices: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cabeçalho
            HeaderSection()

            // Informações do usuário
            UserInfoSection(currentUser = currentUser)

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outline
            )

            // Botões principais
            MainActionsSection(
                onNavigateToServices = onNavigateToServices,
                scope = scope,
                userRepository = userRepository
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outline
            )

            // Botão de logout
            LogoutSection(onSignOutClick = onSignOutClick)
        }
    }
}

@Composable
private fun HeaderSection(
    modifier: Modifier = Modifier
) {
    Text(
        text = "Painel Principal",
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(vertical = 16.dp)
    )
}

@Composable
private fun UserInfoSection(
    currentUser: FirebaseUser?,
    modifier: Modifier = Modifier
) {
    currentUser?.let { user ->
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Bem-vindo!",
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Usuário: ${user.displayName ?: "Sem nome"}",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Email: ${user.email ?: "Sem email"}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MainActionsSection(
    onNavigateToServices: () -> Unit,
    scope: CoroutineScope,
    userRepository: UserRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Botão principal de serviços
        Button(
            onClick = onNavigateToServices,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 4.dp
            )
        ) {
            Text(
                text = "Acessar Serviços",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        // Botões de teste
        OutlinedButton(
            onClick = {
                scope.launch {
                    handleFirestoreTest(
                        context = context,
                        userRepository = userRepository
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = "Testar Firestore",
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        OutlinedButton(
            onClick = {
                scope.launch {
                    handleRealtimeTest(context = context)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = "Testar Realtime DB",
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun LogoutSection(
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onSignOutClick,
        modifier = modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
        )
    ) {
        Text(
            text = "Logout",
            modifier = Modifier.padding(vertical = 4.dp)
        )
    }
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
        showToast(context, "Erro no logout: ${e.message}")
    }
}

private suspend fun handleFirestoreTest(
    context: Context,
    userRepository: UserRepository
) {
    try {
        val testData = mapOf(
            "test" to "Teste realizado em ${System.currentTimeMillis()}",
            "timestamp" to System.currentTimeMillis()
        )

        showToast(context, "Teste do Firestore executado!")
    } catch (e: Exception) {
        showToast(context, "Erro no teste Firestore: ${e.message}")
    }
}

private suspend fun handleRealtimeTest(context: Context) {
    try {
        showToast(context, "Teste do Realtime DB executado!")
    } catch (e: Exception) {
        showToast(context, "Erro no teste Realtime DB: ${e.message}")
    }
}

private fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}