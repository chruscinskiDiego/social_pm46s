package br.edu.utfpr.social_pm46s.ui

// Usando SmallTopAppBar do material3, sem definição customizada
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
//import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import br.edu.utfpr.social_pm46s.R
import br.edu.utfpr.social_pm46s.data.model.Group

// Data class for group members
private data class GroupMember(
    val id: String,
    val name: String
)

object GroupsScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val allGroups = remember {
            listOf(
                Group("1", "Trilha Aventura", "publico", "ABC123", 0.0),
                Group("2", "Yoga Lovers", "privado", null, 0.0),
                Group("3", "Gamers BR", "publico", "GAMER321", 0.0),
                Group("4", "Leitura coletiva", "publico", null, 0.0)
            )
        }
        var userGroups by remember { mutableStateOf(listOf(allGroups[0], allGroups[2])) }

        val groupMembers = remember {
            mapOf(
                "1" to listOf(
                    GroupMember("u1", "Alice"),
                    GroupMember("u2", "Bruno"),
                    GroupMember("u3", "Carla")
                ),
                "2" to listOf(
                    GroupMember("u4", "Daniela"),
                    GroupMember("u5", "Eduardo")
                ),
                "3" to listOf(
                    GroupMember("u6", "Fernanda"),
                    GroupMember("u7", "Gustavo")
                ),
                "4" to listOf(
                    GroupMember("u8", "Helena")
                )
            )
        }

        val expandedIds = remember { mutableStateListOf<String>() }

        Scaffold(
            topBar = {
                SmallTopAppBar(
                    title = { Text("Grupos", fontSize = 20.sp) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { /*TODO*/ }) {
                    Icon(Icons.Default.Add, contentDescription = "Criar Grupo")
                }
            },
            content = { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(Color(0xFFF5F5F5))
                        .padding(16.dp)
                ) {
                    Text("Meus Grupos", fontSize = 18.sp, color = Color.DarkGray)
                    Spacer(Modifier.height(8.dp))
                    Card(
                        Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        LazyRow(
                            contentPadding = PaddingValues(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(userGroups) { group ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(80.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_launcher_background),
                                        contentDescription = group.nomeGrupo,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(group.nomeGrupo, fontSize = 14.sp, maxLines = 1)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Text("Todos os Grupos", fontSize = 18.sp, color = Color.DarkGray)
                    Spacer(Modifier.height(8.dp))
                    Card(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        LazyColumn(
                            Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(allGroups) { _, group ->
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (expandedIds.contains(group.id)) expandedIds.remove(group.id)
                                                else expandedIds.add(group.id)
                                            }
                                            .padding(8.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_launcher_background),
                                                contentDescription = null,
                                                modifier = Modifier.size(32.dp)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Column {
                                                Text(group.nomeGrupo, fontSize = 16.sp)
                                                Text(
                                                    "${groupMembers[group.id]?.size ?: 0} integrantes",
                                                    fontSize = 12.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                        }
                                        val isMember = userGroups.any { it.id == group.id }
                                        Button(
                                            onClick = { if (!isMember) userGroups = userGroups + group },
                                            enabled = !isMember,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isMember) Color.LightGray else Color(0xFF4A148C),
                                                contentColor = Color.White
                                            )
                                        ) { Text(if (isMember) "OK" else "Entrar") }
                                    }
                                    if (expandedIds.contains(group.id)) {
                                        Column(
                                            Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xFFF0F0F0))
                                                .padding(8.dp)
                                        ) {
                                            Text("Membros:", fontSize = 14.sp, color = Color.DarkGray)
                                            groupMembers[group.id]?.forEach { member ->
                                                Text(
                                                    member.name,
                                                    Modifier.padding(start = 8.dp, top = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        )
    }

    @Composable
    fun SmallTopAppBar(title: @Composable () -> Unit, navigationIcon: @Composable () -> Unit) {
        TODO("Not yet implemented")
    }
}
