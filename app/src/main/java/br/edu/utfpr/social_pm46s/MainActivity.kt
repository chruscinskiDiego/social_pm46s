package br.edu.utfpr.social_pm46s

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import br.edu.utfpr.social_pm46s.data.repository.AuthRepository
import br.edu.utfpr.social_pm46s.ui.LoginScreen
import br.edu.utfpr.social_pm46s.ui.ServiceTestScreen
import br.edu.utfpr.social_pm46s.ui.theme.Social_pm46sTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        authRepository = AuthRepository(this)

        setContent {
            Social_pm46sTheme {
                var isUserLoggedIn by remember { mutableStateOf(authRepository.isUserLoggedIn()) }
                var currentUser by remember { mutableStateOf(authRepository.getCurrentUser()) }
                var showServiceTests by remember { mutableStateOf(false) }

                // Atualiza o estado quando há mudança na autenticação
                LaunchedEffect(Unit) {
                    isUserLoggedIn = authRepository.isUserLoggedIn()
                    currentUser = authRepository.getCurrentUser()
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when {
                        !isUserLoggedIn -> {
                            LoginScreen(
                                onLoginSuccess = {
                                    isUserLoggedIn = true
                                    currentUser = authRepository.getCurrentUser()
                                    Log.d("Login", "Login realizado: ${currentUser?.email}")
                                }
                            )
                        }

                        showServiceTests -> {
                            ServiceTestScreen(
                                modifier = Modifier.padding(innerPadding),
                                onBackClick = { showServiceTests = false }
                            )
                        }

                        else -> {
                            MainScreen(
                                modifier = Modifier.padding(innerPadding),
                                onSignOutClick = {
                                    handleSignOut()
                                    isUserLoggedIn = false
                                    currentUser = null
                                },
                                onServiceTestClick = { showServiceTests = true },
                                currentUser = currentUser
                            )
                        }
                    }
                }
            }
        }
    }

    private fun handleSignOut() {
        lifecycleScope.launch {
            authRepository.signOut()
            Log.d("Auth", "Logout realizado")
        }
    }
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    onSignOutClick: () -> Unit,
    onServiceTestClick: () -> Unit,
    currentUser: com.google.firebase.auth.FirebaseUser?
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Bem-vindo!", fontSize = 22.sp)

        currentUser?.let { user ->
            Text("Usuário: ${user.displayName ?: "Sem nome"}")
            Text("Email: ${user.email ?: "Sem email"}")
        }

        Button(
            onClick = onSignOutClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Logout")
        }

        HorizontalDivider()

        Button(
            onClick = onServiceTestClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Testar Services")
        }
    }
}