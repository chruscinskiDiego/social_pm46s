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
    var isLoading by remember { mutableStateOf(true) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedGroup by remember { mutableStateOf<Group?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Carrega grupos do usuário e todos os grupos
    LaunchedEffect(userId) {
        scope.launch {
            try {
                isLoading = true
                errorMessage = null
                val usuarioGrupos = usuarioGrupoRepository.getUserGroups(userId)
                val userGroupIds = usuarioGrupos.map { it.idGrupo }
                val all = groupRepository.getAllGroups()
                userGroups = all.filter { it.id in userGroupIds }
                allGroups = all
                // Carrega membros de cada grupo
                val membersMap = mutableMapOf<String, List<User>>()
                for (group in all) {
                    val usuariosGrupo = usuarioGrupoRepository.getGroupMembers(group.id)
                    val users = usuariosGrupo.mapNotNull { userRepository.getUser(it.idUsuario) }
                    membersMap[group.id] = users
                }
                groupMembers = membersMap
                isLoading = false
            } catch (e: Exception) {
                isLoading = false
                errorMessage = "Erro ao carregar grupos: ${e.message}"
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.primary,
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(painterResource(id = R.drawable.ic_launcher_foreground), contentDescription = "Criar Grupo")
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
            if (isLoading) {
                CircularProgressIndicator()
            } else if (errorMessage != null) {
                Text(errorMessage!!, color = Color.Red)
            } else {
                // Seção 1: Grupos do usuário
                Text(
                    text = "Seus Grupos",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.fillMaxWidth()
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 180.dp)
                        .background(MaterialTheme.colorScheme.surface),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(userGroups) { group ->
                        GroupListItem(
                            group = group,
                            members = groupMembers[group.id] ?: emptyList(),
                            isUserInGroup = true,
                            onClick = { selectedGroup = group }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                // Seção 2: Todos os grupos
                Text(
                    text = "Todos os Grupos",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.fillMaxWidth()
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surface),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(allGroups) { group ->
                        val isUserInGroup = userGroups.any { it.id == group.id }
                        GroupListItem(
                            group = group,
                            members = groupMembers[group.id] ?: emptyList(),
                            isUserInGroup = isUserInGroup,
                            onClick = { selectedGroup = group }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onBackClick, modifier = Modifier.fillMaxWidth()) {
                Text("Voltar")
            }
        }
        // Dialog de detalhes/criação de grupo
        if (showCreateDialog) {
            CreateGroupDialog(
                onDismiss = { showCreateDialog = false },
                onGroupCreated = { newGroup ->
                    scope.launch {
                        val success = groupRepository.saveGroup(newGroup)
                        if (success) {
                            showCreateDialog = false
                        } else {
                            errorMessage = "Erro ao criar grupo"
                        }
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
                            val usuarioGrupo = UsuarioGrupo(idUsuario = userId, idGrupo = group.id)
                            usuarioGrupoRepository.addUserToGroup(usuarioGrupo)
                            selectedGroup = null
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
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isUserInGroup) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface
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
                Text(group.nomeGrupo, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Integrantes: ${members.size}", fontSize = 12.sp)
            }
            if (isUserInGroup) {
                Text("Você participa", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun CreateGroupDialog(
    onDismiss: () -> Unit,
    onGroupCreated: (Group) -> Unit
) {
    var nomeGrupo by remember { mutableStateOf("") }
    var tipoGrupo by remember { mutableStateOf("publico") }
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Tipo:")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(tipoGrupo)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (nomeGrupo.isNotBlank()) {
                            onGroupCreated(
                                Group(
                                    nomeGrupo = nomeGrupo,
                                    tipoGrupo = tipoGrupo
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Criar")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
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
                    items(members) { user ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Text(user.displayName.ifBlank { user.nomeUsuario }, fontSize = 14.sp)
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