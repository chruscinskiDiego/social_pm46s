package br.edu.utfpr.social_pm46s.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.edu.utfpr.social_pm46s.data.model.social.UserRanking
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

object RankingScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current


        val allRankings = remember {
            listOf(
                UserRanking("1", "Alice", 12_000, 8.4, 320.0),
                UserRanking("2", "Bruno", 18_000, 10.1, 420.5),
                UserRanking("3", "Carla", 9_500, 5.2, 210.3),
                UserRanking("4", "Diego", 25_000, 15.8, 600.0),
                UserRanking("5", "Elaine", 16_000, 9.3, 380.7),
                UserRanking("6", "Felipe", 14_000, 7.0, 350.0),
            )
        }

        val top5 = remember(allRankings) {
            allRankings
                .map { user ->
                    val score = user.totalSteps +
                            user.totalDistance.toInt() +
                            user.totalCalories.toInt()
                    user to score
                }
                .sortedByDescending { it.second }
                .take(5)
                .map { it.first }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF4A148C))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "🏆 RANKING",
                fontSize = 24.sp,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFD1C4E9))
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "TOP 5 GERAL",
                            fontSize = 18.sp,
                            color = Color(0xFF4A148C)
                        )
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(top5) { index, user ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFD1C4E9))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "${index + 1}.  ${user.name}")
                                    val pts = user.totalSteps +
                                            user.totalDistance.toInt() +
                                            user.totalCalories.toInt()
                                    Text(
                                        text = "⭐ $pts pts",
                                        textAlign = TextAlign.End
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { navigator.pop() },
                modifier = Modifier
                    .fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,       // cor de fundo
                    contentColor = Color(0xFF4A148C)    // cor do texto/ícone
                )
            ) {
                Text(
                    "Menu Principal",
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}
