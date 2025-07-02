package br.edu.utfpr.social_pm46s.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.edu.utfpr.social_pm46s.R
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import br.edu.utfpr.social_pm46s.data.repository.AuthRepository
import kotlinx.coroutines.launch

object ServicesScreen : Screen {

    @Composable
    override fun Content() {
        // Pega a instância do navegador atual
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val authRepository = AuthRepository(context)
        val scope = rememberCoroutineScope()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    scope.launch {
                        authRepository.signOut()
                        navigator.replaceAll(LoginScreen())
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Logout")
            }


            ServiceCard(
                title = "Iniciar Monitoramento",
                imageResId = R.drawable.monitoring,
                // Ao clicar, "empurra" a nova tela para a pilha de navegação
                onClick = { navigator.push(MonitoringScreen) }
            )

            ServiceCard(
                title = "Grupos",
                imageResId = R.drawable.groups,
                onClick = { navigator.push(GroupsScreen) }
            )

            ServiceCard(
                title = "Ranking Geral",
                imageResId = R.drawable.ranking,
                onClick = { navigator.push(RankingScreen)}
            )
        }
    }
}

// Telas de destino (placeholders)
object MonitoringScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Tela de Monitoramento", fontSize = 24.sp)
                Button(onClick = { navigator.pop() }) { // 'pop' volta para a tela anterior
                    Text("Voltar")
                }
            }
        }
    }
}

object GroupsScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Tela de Grupos", fontSize = 24.sp)
                Button(onClick = { navigator.pop() }) {
                    Text("Voltar")
                }
            }
        }
    }
}

/*object RankingScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Tela de Ranking Geral", fontSize = 24.sp)
                Button(onClick = { navigator.pop() }) {
                    Text("Voltar")
                }
            }
        }
    }
}*/


// O Composable do Card não precisa ser uma Screen, ele é apenas um componente de UI.
// Ele continua o mesmo de antes.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceCard(
    title: String,
    imageResId: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
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
                    .background(Color(0xFF4A148C))
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }
    }
}