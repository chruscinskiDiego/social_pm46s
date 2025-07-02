package br.edu.utfpr.social_pm46s.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.edu.utfpr.social_pm46s.data.model.social.UserRanking
import br.edu.utfpr.social_pm46s.data.repository.ActivityRepository
import br.edu.utfpr.social_pm46s.data.repository.UserRepository
import br.edu.utfpr.social_pm46s.service.SocialFitnessService
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
        val userRepository = UserRepository()
        val activityRepository = ActivityRepository()
        val socialFitnessService = SocialFitnessService(activityRepository, userRepository)
        val scope = rememberCoroutineScope()

        RankingScreenContent(
            socialFitnessService = socialFitnessService,
            scope = scope,
            onBackClick = { navigator.pop() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RankingScreenContent(
    socialFitnessService: SocialFitnessService,
    scope: CoroutineScope,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var rankings by remember { mutableStateOf<List<UserRanking>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedPeriod by remember { mutableStateOf("weekly") }

    LaunchedEffect(selectedPeriod) {
        scope.launch {
            try {
                isLoading = true
                errorMessage = null
                rankings = socialFitnessService.getLeaderboard(selectedPeriod)
                isLoading = false
            } catch (e: Exception) {
                isLoading = false
                errorMessage = "Erro ao carregar ranking: ${e.message}"
            }
        }
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

            HeaderSection()

            Spacer(modifier = Modifier.height(16.dp))

            PeriodFilterSection(
                selectedPeriod = selectedPeriod,
                onPeriodChanged = { selectedPeriod = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            RankingCard(
                isLoading = isLoading,
                rankings = rankings,
                errorMessage = errorMessage,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.height(16.dp))

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
private fun PeriodFilterSection(
    selectedPeriod: String,
    onPeriodChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedPeriod == "weekly",
            onClick = { onPeriodChanged("weekly") },
            label = { Text("Semanal") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.surface,
                selectedLabelColor = MaterialTheme.colorScheme.primary
            )
        )

        FilterChip(
            selected = selectedPeriod == "monthly",
            onClick = { onPeriodChanged("monthly") },
            label = { Text("Mensal") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.surface,
                selectedLabelColor = MaterialTheme.colorScheme.primary
            )
        )

        FilterChip(
            selected = selectedPeriod == "all_time",
            onClick = { onPeriodChanged("all_time") },
            label = { Text("Todos") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.surface,
                selectedLabelColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
private fun RankingCard(
    isLoading: Boolean,
    rankings: List<UserRanking>,
    errorMessage: String?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        when {
            isLoading -> LoadingContent()
            errorMessage != null -> ErrorContent(errorMessage)
            else -> RankingContent(rankings = rankings)
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
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Carregando ranking...",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorContent(
    errorMessage: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "❌",
                fontSize = 32.sp
            )
            Text(
                text = errorMessage,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun RankingContent(
    rankings: List<UserRanking>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        RankingHeader(totalUsers = rankings.size)

        if (rankings.isEmpty()) {
            EmptyRankingContent()
        } else {
            RankingList(rankings = rankings)
        }
    }
}

@Composable
private fun RankingHeader(
    totalUsers: Int,
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
            text = "TOP $totalUsers USUÁRIOS",
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
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "📊",
                fontSize = 32.sp
            )
            Text(
                text = "Nenhum dado de ranking disponível",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Complete algumas atividades para aparecer no ranking!",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
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
            .height(70.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (position) {
                1 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                2 -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                3 -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            UserInfoSection(
                position = position,
                user = user
            )

            StatsSection(user = user)
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
            fontSize = 20.sp
        )
        Column {
            Text(
                text = "$position. ${user.name}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "⭐ ${calculateUserScore(user)} pts",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun StatsSection(
    user: UserRanking,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End
    ) {
        if (user.totalSteps > 0) {
            Text(
                text = "👣 ${formatNumber(user.totalSteps)}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (user.totalDistance > 0) {
            Text(
                text = "📍 ${String.format("%.1f", user.totalDistance)}km",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (user.totalCalories > 0) {
            Text(
                text = "🔥 ${String.format("%.0f", user.totalCalories)}kcal",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

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

private fun formatNumber(number: Int): String {
    return when {
        number >= 1000000 -> "${number / 1000000}M"
        number >= 1000 -> "${number / 1000}K"
        else -> number.toString()
    }
}