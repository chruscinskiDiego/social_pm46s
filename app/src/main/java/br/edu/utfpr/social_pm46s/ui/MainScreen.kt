package br.edu.utfpr.social_pm46s.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.edu.utfpr.social_pm46s.data.repository.AuthRepository
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

object MainScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val authRepository = AuthRepository(context)
        val scope = rememberCoroutineScope()

        // Passamos a lógica de logout para o próprio Composable
        MainScreenContent(
            currentUser = authRepository.getCurrentUser(),
            onSignOutClick = {
                scope.launch {
                    authRepository.signOut()
                    // Substitui a tela atual pela de Login
                    navigator.replaceAll(LoginScreen)
                }
            },
            onFirestoreTestClick = { /* Sua lógica de teste aqui */ },
            onRealtimeTestClick = { /* Sua lógica de teste aqui */ }
        )
    }
}

// O seu Composable de UI, agora privado e sem lógica de navegação direta
@Composable
private fun MainScreenContent(
    modifier: Modifier = Modifier,
    onSignOutClick: () -> Unit,
    onFirestoreTestClick: () -> Unit,
    onRealtimeTestClick: () -> Unit,
    currentUser: FirebaseUser?
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

        Button(onClick = onSignOutClick, modifier = Modifier.fillMaxWidth()) {
            Text("Logout")
        }

        HorizontalDivider()

        Button(onClick = onFirestoreTestClick, modifier = Modifier.fillMaxWidth()) {
            Text("Testar Firestore")
        }

        Button(onClick = onRealtimeTestClick, modifier = Modifier.fillMaxWidth()) {
            Text("Testar Realtime DB")
        }
    }
}