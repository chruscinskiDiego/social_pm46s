package br.edu.utfpr.social_pm46s.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import br.edu.utfpr.social_pm46s.data.repository.AuthRepository
import br.edu.utfpr.social_pm46s.data.repository.UserRepository
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
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
            onLoginSuccess = { navigator.replace(MainScreen) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoginScreenContent(
    authRepository: AuthRepository,
    userRepository: UserRepository,
    scope: CoroutineScope,
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.primary
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .systemBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Seção de boas-vindas
                    WelcomeSection()

                    // Botão de login
                    LoginButton(
                        onClick = {
                            scope.launch {
                                handleGoogleSignIn(
                                    context = context,
                                    authRepository = authRepository,
                                    userRepository = userRepository,
                                    onSuccess = onLoginSuccess
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeSection(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Bem-vindo!",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Faça login para acessar sua conta",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LoginButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Text(
            text = "Entrar com Google",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}

private suspend fun handleGoogleSignIn(
    context: Context,
    authRepository: AuthRepository,
    userRepository: UserRepository,
    onSuccess: () -> Unit
) {
    try {
        // Primeira tentativa: contas autorizadas
        val user = authRepository.signInWithGoogle()

        if (user != null) {
            try {
                // Usar o método específico do UserRepository para dados do Google
                userRepository.saveOrUpdateUserFromGoogle(
                    userId = user.uid,
                    email = user.email ?: "",
                    displayName = user.displayName ?: "",
                    photoUrl = user.photoUrl?.toString() ?: ""
                )

                showToast(context, "Login realizado com sucesso!")
                onSuccess()
            } catch (e: Exception) {
                showToast(context, "Erro ao salvar dados: ${e.message}")
            }
        } else {
            showToast(context, "Falha no login")
        }

    } catch (e: NoCredentialException) {
        // Se não há credenciais, tenta com todas as contas disponíveis
        handleFallbackGoogleSignIn(
            context = context,
            authRepository = authRepository,
            userRepository = userRepository,
            onSuccess = onSuccess
        )
    } catch (e: GetCredentialException) {
        when (e.type) {
            "android.credentials.GetCredentialException.TYPE_USER_CANCELED" -> {
                showToast(context, "Login cancelado pelo usuário")
            }

            "android.credentials.GetCredentialException.TYPE_NO_CREDENTIAL" -> {
                // Fallback para forçar o prompt
                handleFallbackGoogleSignIn(
                    context = context,
                    authRepository = authRepository,
                    userRepository = userRepository,
                    onSuccess = onSuccess
                )
            }

            else -> {
                showToast(context, "Erro de credencial: ${e.message}")
            }
        }
    } catch (e: Exception) {
        showToast(context, "Erro inesperado: ${e.message}")
    }
}

private suspend fun handleFallbackGoogleSignIn(
    context: Context,
    authRepository: AuthRepository,
    userRepository: UserRepository,
    onSuccess: () -> Unit
) {
    try {
        // Força o prompt de login mesmo sem contas
        val user = authRepository.signInWithGoogleFallback()

        if (user != null) {
            try {
                // Usar o método específico do UserRepository para dados do Google
                userRepository.saveOrUpdateUserFromGoogle(
                    userId = user.uid,
                    email = user.email ?: "",
                    displayName = user.displayName ?: "",
                    photoUrl = user.photoUrl?.toString() ?: ""
                )

                showToast(context, "Login realizado com sucesso!")
                onSuccess()
            } catch (e: Exception) {
                showToast(context, "Erro ao salvar dados: ${e.message}")
            }
        } else {
            showToast(
                context,
                "Nenhuma conta Google disponível. Adicione uma conta nas configurações do dispositivo."
            )
        }

    } catch (e: Exception) {
        showToast(context, "Erro no login alternativo: ${e.message}")
    }
}

private fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}