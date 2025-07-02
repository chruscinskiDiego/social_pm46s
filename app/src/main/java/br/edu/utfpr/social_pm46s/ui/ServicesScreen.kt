package br.edu.utfpr.social_pm46s.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.edu.utfpr.social_pm46s.R
import br.edu.utfpr.social_pm46s.data.repository.AuthRepository
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add

object ServicesScreen : Screen {
    private fun readResolve(): Any = ServicesScreen

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val authRepository = AuthRepository(context)
        val scope = rememberCoroutineScope()

        ServicesScreenContent(
            authRepository = authRepository,
            scope = scope,
            onLogoutClick = {
                scope.launch {
                    handleLogout(
                        context = context,
                        authRepository = authRepository,
                        onSuccess = {
                            navigator.replaceAll(LoginScreen)
                        }
                    )
                }
            },
            onMonitoringClick = { navigator.push(MonitoringScreen) },
            onGroupsClick = { navigator.push(GroupsScreen) },
            onRankingClick = { navigator.push(RankingScreen) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServicesScreenContent(
    authRepository: AuthRepository,
    scope: CoroutineScope,
    onLogoutClick: () -> Unit,
    onMonitoringClick: () -> Unit,
    onGroupsClick: () -> Unit,
    onRankingClick: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Título da tela
            HeaderSection()

            // Cards de serviços
            ServicesSection(
                onMonitoringClick = onMonitoringClick,
                onGroupsClick = onGroupsClick,
                onRankingClick = onRankingClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Botão de logout
            LogoutSection(onLogoutClick = onLogoutClick)
        }
    }
}

@Composable
private fun HeaderSection(
    modifier: Modifier = Modifier
) {
    Text(
        text = "Serviços Disponíveis",
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun ServicesSection(
    onMonitoringClick: () -> Unit,
    onGroupsClick: () -> Unit,
    onRankingClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        ServiceCard(
            title = "Iniciar Monitoramento",
            imageResId = R.drawable.monitoring,
            onClick = onMonitoringClick
        )

        ServiceCard(
            title = "Grupos",
            imageResId = R.drawable.groups,
            onClick = onGroupsClick
        )

        ServiceCard(
            title = "Ranking Geral",
            imageResId = R.drawable.ranking,
            onClick = onRankingClick
        )
    }
}

@Composable
private fun LogoutSection(
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onLogoutClick,
        modifier = modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
        )
    ) {
        Text(
            text = "Logout",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServiceCard(
    title: String,
    imageResId: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        onClick = onClick
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = imageResId),
                contentDescription = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }
    }
}

// Telas de destino (placeholders)
//object MonitoringScreens : Screen {
//    private fun readResolve(): Any = MonitoringScreen
//
//    @OptIn(ExperimentalMaterial3Api::class)
//    @Composable
//    override fun Content() {
//        val navigator = LocalNavigator.currentOrThrow
//
//        Scaffold(
//            modifier = Modifier.fillMaxSize(),
//            containerColor = MaterialTheme.colorScheme.background
//        ) { innerPadding ->
//            Box(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .padding(innerPadding)
//                    .systemBarsPadding(),
//                contentAlignment = Alignment.Center
//            ) {
//                Column(
//                    horizontalAlignment = Alignment.CenterHorizontally,
//                    verticalArrangement = Arrangement.spacedBy(16.dp)
//                ) {
//                    Text(
//                        text = "Tela de Monitoramento",
//                        fontSize = 24.sp,
//                        fontWeight = FontWeight.Bold,
//                        color = MaterialTheme.colorScheme.primary
//                    )
//
//                    Button(
//                        onClick = { navigator.pop() },
//                        colors = ButtonDefaults.buttonColors(
//                            containerColor = MaterialTheme.colorScheme.primary,
//                            contentColor = MaterialTheme.colorScheme.onPrimary
//                        )
//                    ) {
//                        Text("Voltar")
//                    }
//                }
//            }
//        }
//    }
//}

private suspend fun handleLogout(
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

private fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}