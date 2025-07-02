package br.edu.utfpr.social_pm46s.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.edu.utfpr.social_pm46s.data.model.social.UserRanking
import br.edu.utfpr.social_pm46s.data.repository.UserRepository
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object RankingScreen : Screen {
    private fun readResolve(): Any = RankingScreen

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val userRepository = UserRepository()
        val scope = rememberCoroutineScope()

        RankingScreenContent(
            userRepository = userRepository,
            scope = scope,
            onBackClick = { navigator.pop() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RankingScreenContent(
    userRepository: UserRepository,
    scope: CoroutineScope,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var rankings by remember { mutableStateOf<List<UserRanking>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Carrega dados iniciais (mockados por enquanto)
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                // TODO: Substituir por dados reais do repository
                rankings = getMockRankings()
                isLoading = false
            } catch (e: Exception) {
                isLoading = false
                // TODO: Tratar erro adequadamente
            }
        }
    }

    val top5Rankings = remember(rankings) {
        rankings
            .map { user ->
                val score = calculateUserScore(user)
                user to score
            }
            .sortedByDescending { it.second }
            .take(5)
            .map { it.first }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.primary
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Título da tela
            HeaderSection()

            Spacer(modifier = Modifier.height(16.dp))

            // Card principal com ranking
            RankingCard(
                isLoading = isLoading,
                rankings = top5Rankings,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Botão voltar
            BackButton(onClick = onBackClick)
        }
    }
}

@Composable
private fun HeaderSection(
    modifier: Modifier = Modifier
) {
    Text(
        text = "🏆 RANKING",
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onPrimary,
        modifier = modifier
    )
}

@Composable
private fun RankingCard(
    isLoading: Boolean,
    rankings: List<UserRanking>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        if (isLoading) {
            LoadingContent()
        } else {
            RankingContent(rankings = rankings)
        }
    }
}

@Composable
private fun BackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Text(
            text = "Menu Principal",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun RankingContent(
    rankings: List<UserRanking>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Cabeçalho
        RankingHeader()

        // Lista de rankings
        if (rankings.isEmpty()) {
            EmptyRankingContent()
        } else {
            RankingList(rankings = rankings)
        }
    }
}

@Composable
private fun RankingHeader(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "TOP 5 GERAL",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun RankingList(
    rankings: List<UserRanking>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(rankings) { index, user ->
            RankingItem(
                position = index + 1,
                user = user
            )
        }
    }
}

@Composable
private fun EmptyRankingContent() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Nenhum dado de ranking disponível",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RankingItem(
    position: Int,
    user: UserRanking,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Posição e nome
            UserInfoSection(
                position = position,
                user = user
            )

            // Pontuação
            ScoreSection(user = user)
        }
    }
}

@Composable
private fun UserInfoSection(
    position: Int,
    user: UserRanking,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = getPositionEmoji(position),
            fontSize = 18.sp
        )
        Text(
            text = "$position. ${user.name}",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ScoreSection(
    user: UserRanking,
    modifier: Modifier = Modifier
) {
    Text(
        text = "⭐ ${calculateUserScore(user)} pts",
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.End,
        modifier = modifier
    )
}

// Funções auxiliares
private fun calculateUserScore(user: UserRanking): Int {
    return user.totalSteps +
            user.totalDistance.toInt() +
            user.totalCalories.toInt()
}

private fun getPositionEmoji(position: Int): String {
    return when (position) {
        1 -> "🥇"
        2 -> "🥈"
        3 -> "🥉"
        else -> "🏅"
    }
}

private suspend fun getMockRankings(): List<UserRanking> {
    // Simula delay de rede
    kotlinx.coroutines.delay(1000)

    return listOf(
        UserRanking("1", "Alice", 12_000, 8.4, 320.0),
        UserRanking("2", "Bruno", 18_000, 10.1, 420.5),
        UserRanking("3", "Carla", 9_500, 5.2, 210.3),
        UserRanking("4", "Diego", 25_000, 15.8, 600.0),
        UserRanking("5", "Elaine", 16_000, 9.3, 380.7),
        UserRanking("6", "Felipe", 14_000, 7.0, 350.0),
    )
}