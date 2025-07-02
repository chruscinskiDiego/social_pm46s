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
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
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
            onLoginSuccess = {
                navigator.replaceAll(ServicesScreen)
            }
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
    val credentialManager = CredentialManager.create(context)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            WelcomeSection()

            Spacer(modifier = Modifier.height(48.dp))

            LoginButton(
                onClick = {
                    scope.launch {
                        handleGoogleSignIn(
                            context = context,
                            authRepository = authRepository,
                            userRepository = userRepository,
                            credentialManager = credentialManager,
                            onSuccess = onLoginSuccess
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun WelcomeSection(
    modifier: Modifier = Modifier
) {
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
private fun LoginButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                userRepository.saveOrUpdateUserFromGoogle(
                    userId = user.uid,
                    email = user.email ?: "",
                    displayName = user.displayName ?: "",
                    photoUrl = user.photoUrl?.toString() ?: ""
                )

                showToast(context, "Login realizado com sucesso!")
                onSuccess()
            } catch (e: Exception) {
                showToast(context, "Erro ao salvar usuário: ${e.message}")
            }
        } else {
            showToast(context, "Falha na autenticação")
        }
    } catch (e: GetCredentialException) {
        if (e.type != "androidx.credentials.exceptions.NoCredentialException") {
            showToast(context, "Falha no login: ${e.message}")
        }
    } catch (e: Exception) {
        showToast(context, "Erro: ${e.message}")
    }
}

private fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}