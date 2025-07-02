// Em MainActivity.kt
package br.edu.utfpr.social_pm46s

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import br.edu.utfpr.social_pm46s.data.repository.AuthRepository
import br.edu.utfpr.social_pm46s.ui.*
import br.edu.utfpr.social_pm46s.ui.theme.Social_pm46sTheme
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val authRepository = AuthRepository(this)

        setContent {
            Social_pm46sTheme {
                var currentUser by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }

                DisposableEffect(Unit) {
                    val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                        currentUser = firebaseAuth.currentUser
                    }
                    FirebaseAuth.getInstance().addAuthStateListener(authStateListener)
                    onDispose {
                        FirebaseAuth.getInstance().removeAuthStateListener(authStateListener)
                    }
                }

                if (currentUser != null) {
                    // MUDANÇA: Adicionado parênteses ()
                    AppNavigator(authRepository = authRepository, startScreen = ServicesScreen)
                } else {
                    // MUDANÇA: Adicionado parênteses ()
                    Navigator(screen = LoginScreen()) { navigator ->
                        CurrentScreen()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigator(authRepository: AuthRepository, startScreen: Screen) {
    Navigator(screen = startScreen) { navigator ->
        val scope = rememberCoroutineScope()

        SlideTransition(navigator) { screen ->
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            val title = when (screen) {
                                is ServicesScreen -> "Serviços"
                                is MonitoringScreen -> "Monitoramento"
                                is GroupsScreen -> "Grupos"
                                is RankingScreen -> "Ranking Geral"
                                else -> ""
                            }
                            Text(title)
                        },
                        navigationIcon = {
                            if (navigator.canPop) {
                                IconButton(onClick = { navigator.pop() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                                }
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                scope.launch {
                                    authRepository.signOut()
                                    // A lógica reativa na MainActivity vai cuidar da troca de tela
                                }
                            }) {
                                Icon(Icons.Default.ExitToApp, "Logout")
                            }
                        }
                    )
                }
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    CurrentScreen()
                }
            }
        }
    }
}