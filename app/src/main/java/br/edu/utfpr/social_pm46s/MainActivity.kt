package br.edu.utfpr.social_pm46s

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import br.edu.utfpr.social_pm46s.firebase.FirebaseManager
import br.edu.utfpr.social_pm46s.ui.theme.Social_pm46sTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val firebaseManager = FirebaseManager()
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            handleGoogleSignInResult(result.data)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setupGoogleSignIn()
        setContent {
            Social_pm46sTheme {
                var isUserLoggedIn by remember { mutableStateOf(firebaseManager.isUserLoggedIn()) }
                var currentUserInfo by remember { mutableStateOf(firebaseManager.getCurrentUserInfo()?.mapValues { it.value ?: "" }) }

                // Atualiza o estado quando a Activity volta para o topo (ex: após login Google)
                LaunchedEffect(Unit) {
                    isUserLoggedIn = firebaseManager.isUserLoggedIn()
                    currentUserInfo = firebaseManager.getCurrentUserInfo()?.mapValues { it.value ?: "" }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SimpleFirebaseScreen(
                        modifier = Modifier.padding(innerPadding),
                        onGoogleSignInClick = {
                            handleGoogleSignIn()
                        },
                        onSignOutClick = {
                            handleSignOut()
                            isUserLoggedIn = false
                            currentUserInfo = null
                        },
                        onFirestoreTestClick = { handleFirestoreTest() },
                        onRealtimeTestClick = { handleRealtimeTest() },
                        isUserLoggedIn = isUserLoggedIn,
                        currentUserInfo = currentUserInfo
                    )
                }

                // Atualiza o estado após login Google
                fun updateLoginState() {
                    isUserLoggedIn = firebaseManager.isUserLoggedIn()
                    currentUserInfo = firebaseManager.getCurrentUserInfo()?.mapValues { it.value ?: "" }
                }

                // Chama updateLoginState após login Google
                DisposableEffect(Unit) {
                    val callback = {
                        updateLoginState()
                    }
                    onDispose { }
                }

                // Exemplo: atualizar estado após login Google
                LaunchedEffect(isUserLoggedIn) {
                    if (isUserLoggedIn) {
                        currentUserInfo = firebaseManager.getCurrentUserInfo()?.mapValues { it.value ?: "" }
                    }
                }

                // Função para ser chamada após login Google
                fun onLoginSuccess() {
                    isUserLoggedIn = true
                    currentUserInfo = firebaseManager.getCurrentUserInfo()?.mapValues { it.value ?: "" }
                }

                // Passa a função para ser chamada após login Google
                this@MainActivity.onLoginSuccess = ::onLoginSuccess
            }
        }
    }

    private fun setupGoogleSignIn() {
        val webClientId = "1084557816281-8r3odquj2meclhrnpott6vj5mseuk9qc.apps.googleusercontent.com"
        firebaseManager.setupGoogleSignIn(this, webClientId)
    }

    private var onLoginSuccess: (() -> Unit)? = null

    private fun handleGoogleSignIn() {
        firebaseManager.getGoogleSignInIntent()?.let { googleSignInLauncher.launch(it) }
    }

    private fun handleGoogleSignInResult(data: Intent?) {
        lifecycleScope.launch {
            val result = firebaseManager.handleGoogleSignInResult(data)
            result.fold(
                onSuccess = { user ->
                    Log.d("GoogleSignIn", "Login realizado: ${user.email}")
                    onLoginSuccess?.invoke()
                },
                onFailure = { exception -> Log.e("GoogleSignIn", "Erro: ${exception.message}") }
            )
        }
    }

    private fun handleSignOut() {
        firebaseManager.signOut()
        Log.d("GoogleSignIn", "Logout realizado")
    }

    private fun handleFirestoreTest() {
        lifecycleScope.launch {
            val userData = mapOf(
                "name" to "João Silva",
                "email" to "joao@example.com",
                "age" to 25,
                "createdAt" to System.currentTimeMillis()
            )
            val saveResult = firebaseManager.saveDocumentToFirestore("users", data = userData)
            saveResult.fold(
                onSuccess = { documentId ->
                    Log.d("Firestore", "Salvo: $documentId")
                    firebaseManager.getDocumentFromFirestore("users", documentId).fold(
                        onSuccess = { data -> Log.d("Firestore", "Lido: $data") },
                        onFailure = { e -> Log.e("Firestore", "Erro leitura: ${e.message}") }
                    )
                },
                onFailure = { e -> Log.e("Firestore", "Erro salvar: ${e.message}") }
            )
        }
    }

    private fun handleRealtimeTest() {
        lifecycleScope.launch {
            val messageData = mapOf(
                "text" to "Olá, Firebase!",
                "timestamp" to System.currentTimeMillis(),
                "userId" to "user123"
            )
            val saveResult = firebaseManager.saveDataToRealtimeDatabase("messages/message1", messageData)
            saveResult.fold(
                onSuccess = {
                    firebaseManager.getDataFromRealtimeDatabase("messages/message1").fold(
                        onSuccess = { data -> Log.d("RealtimeDB", "Lido: $data") },
                        onFailure = { e -> Log.e("RealtimeDB", "Erro leitura: ${e.message}") }
                    )
                },
                onFailure = { e -> Log.e("RealtimeDB", "Erro salvar: ${e.message}") }
            )
        }
    }
}

@Composable
fun SimpleFirebaseScreen(
    modifier: Modifier = Modifier,
    onGoogleSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onFirestoreTestClick: () -> Unit,
    onRealtimeTestClick: () -> Unit,
    isUserLoggedIn: Boolean,
    currentUserInfo: Map<String, Any?>?
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Firebase Google Sign-In", fontSize = 22.sp)
        if (!isUserLoggedIn) {
            Button(onClick = onGoogleSignInClick, modifier = Modifier.fillMaxWidth()) { Text("Entrar com Google") }
        } else if (currentUserInfo != null) {
            Text("Usuário: ${currentUserInfo["displayName"] ?: ""}")
            Text("Email: ${currentUserInfo["email"] ?: ""}")
            Button(onClick = onSignOutClick, modifier = Modifier.fillMaxWidth()) { Text("Logout") }
            Divider()
            Button(onClick = onFirestoreTestClick, modifier = Modifier.fillMaxWidth()) { Text("Testar Firestore") }
            Button(onClick = onRealtimeTestClick, modifier = Modifier.fillMaxWidth()) { Text("Testar Realtime DB") }
        }
    }
}