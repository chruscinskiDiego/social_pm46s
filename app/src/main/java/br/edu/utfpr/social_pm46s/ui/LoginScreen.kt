// Em ui/LoginScreen.kt ou ui/AppScreens.kt
package br.edu.utfpr.social_pm46s.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import br.edu.utfpr.social_pm46s.data.repository.AuthRepository
import br.edu.utfpr.social_pm46s.data.repository.UserRepository // 1. IMPORTAMOS O REPOSITÓRIO CORRETO
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch

class LoginScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        // Este callback agora só será chamado após a autenticação E o salvamento no banco
        LoginScreenContent(
            onLoginSuccess = {
                navigator.replaceAll(ServicesScreen)
            }
        )
    }
}

@Composable
private fun LoginScreenContent(
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val authRepository = AuthRepository(context)
    val userRepository = UserRepository() // 2. INSTANCIAMOS O USER REPOSITORY
    val scope = rememberCoroutineScope()
    val credentialManager = CredentialManager.create(context)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = {
                scope.launch {
                    try {
                        val googleIdOption = authRepository.createGetGoogleIdOption()
                        val request = GetCredentialRequest.Builder()
                            .addCredentialOption(googleIdOption)
                            .build()

                        val result = credentialManager.getCredential(context, request)
                        val user = authRepository.signInWithGoogleCredential(result.credential)

                        if (user != null) {
                            try {
                                // Aguarda salvar no Firebase antes de navegar
                                userRepository.saveOrUpdateUserFromGoogle(
                                    userId = user.uid,
                                    email = user.email ?: "",
                                    displayName = user.displayName ?: "",
                                    photoUrl = user.photoUrl?.toString() ?: ""
                                )
                                onLoginSuccess()
                            } catch (e: Exception) {
                                showToast(context, "Erro ao salvar usuário: ${e.message}")
                            }
                        }
                    } catch (e: GetCredentialException) {
                        if (e.type != "androidx.credentials.exceptions.NoCredentialException") {
                            showToast(context, "Sign-in failed: ${e.message}")
                        }
                    } catch (e: Exception) {
                        showToast(context, "Error: ${e.message}")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text(text = "Sign in with Google")
        }
    }
}

private fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}