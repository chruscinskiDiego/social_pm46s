package br.edu.utfpr.social_pm46s.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.edu.utfpr.social_pm46s.data.model.Group
import br.edu.utfpr.social_pm46s.data.model.User
import br.edu.utfpr.social_pm46s.data.model.UsuarioGrupo
import br.edu.utfpr.social_pm46s.data.repository.GroupRepository
import br.edu.utfpr.social_pm46s.data.repository.UsuarioGrupoRepository
import br.edu.utfpr.social_pm46s.data.repository.UserRepository
import br.edu.utfpr.social_pm46s.data.repository.AuthRepository
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import br.edu.utfpr.social_pm46s.R
import kotlinx.coroutines.CoroutineScope
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.rememberAsyncImagePainter
import br.edu.utfpr.social_pm46s.firebase.FirebaseManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star

object GroupsScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val groupRepository = remember { GroupRepository() }
        val usuarioGrupoRepository = remember { UsuarioGrupoRepository() }
        val userRepository = remember { UserRepository() }
        val authRepository = remember { AuthRepository(context) }
        val scope = rememberCoroutineScope()
        val currentUser = authRepository.getCurrentUser()
        val userId = currentUser?.uid ?: ""

        GroupsScreenContent(
            userId = userId,
            groupRepository = groupRepository,
            usuarioGrupoRepository = usuarioGrupoRepository,
            userRepository = userRepository,
            onBackClick = { navigator.pop() },
            scope = scope
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupsScreenContent(
    userId: String,
    groupRepository: GroupRepository,
    usuarioGrupoRepository: UsuarioGrupoRepository,
    userRepository: UserRepository,
    onBackClick: () -> Unit,
    scope: CoroutineScope,
    modifier: Modifier = Modifier
) {
    var userGroups by remember { mutableStateOf<List<Group>>(emptyList()) }
    var allGroups by remember { mutableStateOf<List<Group>>(emptyList()) }
    var groupMembers by remember { mutableStateOf<Map<String, List<User>>>(emptyMap()) }
    var groupScores by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedGroup by remember { mutableStateOf<Group?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Meus Grupos", "Todos os Grupos")
    val activityRepository = remember { br.edu.utfpr.social_pm46s.data.repository.ActivityRepository() }

    suspend fun calculateUserScore(userId: String): Int {
        val stats = activityRepository.getWeeklyStats(userId)
        return (stats?.steps ?: 0) + (stats?.distance?.toInt() ?: 0) + (stats?.estimatedCalories?.toInt() ?: 0)
    }

    fun loadGroupsData() {
        scope.launch {
            try {
                isLoading = true
                errorMessage = null
                val usuarioGrupos = usuarioGrupoRepository.getUserGroups(userId)
                val userGroupIds = usuarioGrupos.map { it.idGrupo }
                val all = groupRepository.getAllGroups()
                val membersMap = mutableMapOf<String, List<User>>()
                val scoresMap = mutableMapOf<String, Int>()
                for (group in all) {
                    val usuariosGrupo = usuarioGrupoRepository.getGroupMembers(group.id)
                    val users = usuariosGrupo.mapNotNull { userRepository.getUser(it.idUsuario) }
                    membersMap[group.id] = users
                    // Score do grupo: soma dos scores dos membros
                    var groupScore = 0
                    for (user in users) {
                        groupScore += calculateUserScore(user.id)
                    }
                    scoresMap[group.id] = groupScore
                }
                groupMembers = membersMap
                groupScores = scoresMap
                // Ordenar grupos pelo score
                userGroups = all.filter { it.id in userGroupIds }
                    .sortedByDescending { scoresMap[it.id] ?: 0 }
                allGroups = all.sortedByDescending { scoresMap[it.id] ?: 0 }
                isLoading = false
            } catch (e: Exception) {
                isLoading = false
                errorMessage = "Erro ao carregar grupos: ${e.message}"
            }
        }
    }

    LaunchedEffect(userId) {
        loadGroupsData()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.primary,
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Criar Grupo")
            }
        }
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
            Text(
                text = "👥 GRUPOS",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, maxLines = 1) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    errorMessage != null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(errorMessage!!, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                        }
                    }
                    else -> {
                        if (selectedTab == 0) {
                            // Meus Grupos
                            if (userGroups.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Você ainda não participa de nenhum grupo.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("Entre ou crie um grupo!", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    contentPadding = PaddingValues(vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(userGroups) { group ->
                                        GroupListItem(
                                            group = group,
                                            members = groupMembers[group.id] ?: emptyList(),
                                            isUserInGroup = true,
                                            groupScore = groupScores[group.id] ?: 0,
                                            onClick = { selectedGroup = group }
                                        )
                                    }
                                }
                            }
                        } else {
                            // Todos os Grupos
                            if (allGroups.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Nenhum grupo encontrado.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    contentPadding = PaddingValues(vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(allGroups) { group ->
                                        val isUserInGroup = userGroups.any { it.id == group.id }
                                        GroupListItem(
                                            group = group,
                                            members = groupMembers[group.id] ?: emptyList(),
                                            isUserInGroup = isUserInGroup,
                                            groupScore = groupScores[group.id] ?: 0,
                                            onClick = { selectedGroup = group }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onBackClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Voltar")
            }
        }
        // Dialog de detalhes/criação de grupo
        if (showCreateDialog) {
            CreateGroupDialog(
                onDismiss = { showCreateDialog = false },
                onGroupCreated = { newGroup ->
                    scope.launch {
                        isLoading = true
                        val success = groupRepository.saveGroup(newGroup)
                        if (success) {
                            val usuarioGrupo = UsuarioGrupo(idUsuario = userId, idGrupo = newGroup.id)
                            usuarioGrupoRepository.addUserToGroup(usuarioGrupo)
                            showCreateDialog = false
                            loadGroupsData()
                        } else {
                            errorMessage = "Erro ao criar grupo"
                        }
                        isLoading = false
                    }
                }
            )
        }
        // Dialog de detalhes do grupo
        selectedGroup?.let { group ->
            GroupDetailDialog(
                group = group,
                members = groupMembers[group.id] ?: emptyList(),
                isUserInGroup = userGroups.any { it.id == group.id },
                onJoinGroup = {
                    scope.launch {
                        val alreadyInGroup = userGroups.any { it.id == group.id }
                        if (!alreadyInGroup) {
                            isLoading = true
                            val usuarioGrupo = UsuarioGrupo(idUsuario = userId, idGrupo = group.id)
                            usuarioGrupoRepository.addUserToGroup(usuarioGrupo)
                            selectedGroup = null
                            loadGroupsData()
                            isLoading = false
                        }
                    }
                },
                onDismiss = { selectedGroup = null }
            )
        }
    }
}

@Composable
private fun GroupListItem(
    group: Group,
    members: List<User>,
    isUserInGroup: Boolean,
    groupScore: Int = 0,
    onClick: () -> Unit
) {
    val backgroundColor = if (isUserInGroup) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface
    val textColor = if (isUserInGroup) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface
    val scoreColor = if (isUserInGroup) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.groups),
                contentDescription = "Ícone do grupo",
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(group.nomeGrupo, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textColor)
                Text("Integrantes: ${members.size}", fontSize = 12.sp, color = textColor)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Pontuação",
                        tint = scoreColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${groupScore} pts",
                        fontSize = 14.sp,
                        color = scoreColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (isUserInGroup) {
                Text("Você participa", color = scoreColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun CreateGroupDialog(
    onDismiss: () -> Unit,
    onGroupCreated: (Group) -> Unit
) {
    val context = LocalContext.current
    var nomeGrupo by remember { mutableStateOf("") }
    var tipoGrupo by remember { mutableStateOf("publico") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        imageUri = uri
    }
    val scope = rememberCoroutineScope()
    val firebaseManager = remember { FirebaseManager() }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Criar Novo Grupo", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = nomeGrupo,
                    onValueChange = { nomeGrupo = it },
                    label = { Text("Nome do Grupo") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Tipo de grupo (público/privado)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Tipo:")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(tipoGrupo)
                }
                Spacer(modifier = Modifier.height(8.dp))
                // Seleção de imagem
                Button(onClick = { launcher.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                    Text("Selecionar Imagem")
                }
                imageUri?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Image(
                        painter = rememberAsyncImagePainter(it),
                        contentDescription = null,
                        modifier = Modifier.size(80.dp)
                    )
                }
                errorMessage?.let {
                    Text(it, color = Color.Red, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (nomeGrupo.isNotBlank()) {
                            isUploading = true
                            errorMessage = null
                            scope.launch {
                                var imageUrl: String? = null
                                val groupId = System.currentTimeMillis().toString()
                                if (imageUri != null) {
                                    imageUrl = firebaseManager.uploadGroupImage(imageUri!!, groupId)
                                    if (imageUrl == null) {
                                        errorMessage = "Erro ao fazer upload da imagem."
                                        isUploading = false
                                        return@launch
                                    }
                                }
                                onGroupCreated(
                                    Group(
                                        id = groupId,
                                        nomeGrupo = nomeGrupo,
                                        tipoGrupo = tipoGrupo,
                                        imagemUrl = imageUrl
                                    )
                                )
                                isUploading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isUploading
                ) {
                    if (isUploading) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    else Text("Criar")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), enabled = !isUploading) {
                    Text("Cancelar")
                }
            }
        }
    }
}

@Composable
private fun GroupDetailDialog(
    group: Group,
    members: List<User>,
    isUserInGroup: Boolean,
    onJoinGroup: () -> Unit,
    onDismiss: () -> Unit
) {
    val activityRepository = remember { br.edu.utfpr.social_pm46s.data.repository.ActivityRepository() }
    var memberScores by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    val scope = rememberCoroutineScope()

    // Calcula os scores dos membros ao abrir o dialog
    LaunchedEffect(members) {
        scope.launch {
            val scores = mutableMapOf<String, Int>()
            for (user in members) {
                val stats = activityRepository.getWeeklyStats(user.id)
                val score = (stats?.steps ?: 0) + (stats?.distance?.toInt() ?: 0) + (stats?.estimatedCalories?.toInt() ?: 0)
                scores[user.id] = score
            }
            memberScores = scores
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(group.nomeGrupo, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Image(
                    painter = painterResource(id = R.drawable.groups),
                    contentDescription = "Ícone do grupo",
                    modifier = Modifier.size(60.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Tipo: ${group.tipoGrupo}")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Integrantes:", fontWeight = FontWeight.SemiBold)
                LazyColumn(
                    modifier = Modifier.heightIn(max = 120.dp)
                ) {
                    val sortedMembers = members.sortedByDescending { memberScores[it.id] ?: 0 }
                    items(sortedMembers) { user ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Text(user.displayName.ifBlank { user.nomeUsuario }, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Pontuação",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${memberScores[user.id] ?: 0} pts",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (!isUserInGroup) {
                    Button(onClick = onJoinGroup, modifier = Modifier.fillMaxWidth()) {
                        Text("Entrar no Grupo")
                    }
                } else {
                    Text("Você já está neste grupo", color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Fechar")
                }
            }
        }
    }
}