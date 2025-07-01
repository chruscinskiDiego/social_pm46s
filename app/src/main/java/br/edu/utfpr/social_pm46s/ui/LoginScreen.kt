package br.edu.utfpr.social_pm46s.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import br.edu.utfpr.social_pm46s.data.repository.AuthRepository
import kotlinx.coroutines.launch
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val authRepository = AuthRepository(context)
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
                            onLoginSuccess()
                        } else {
                            showToast(context, "Login failed")
                        }
                    } catch (e: GetCredentialException) {
                        showToast(context, "Sign-in failed: ${e.message}")
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