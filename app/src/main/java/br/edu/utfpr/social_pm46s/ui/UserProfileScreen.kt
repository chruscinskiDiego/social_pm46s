package br.edu.utfpr.social_pm46s.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.edu.utfpr.social_pm46s.data.UserProfileManager
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import br.edu.utfpr.social_pm46s.data.repository.AuthRepository
import br.edu.utfpr.social_pm46s.data.model.UserData

@OptIn(ExperimentalMaterial3Api::class)
object UserProfileScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val authRepository = remember { AuthRepository(context) }
        val userId = authRepository.getCurrentUser()?.uid
        LaunchedEffect(userId) { UserProfileManager.init(context) }

        val savedData = remember(userId) {
            UserProfileManager.init(context)
            userId?.let { UserProfileManager.getUserData(it) } ?: UserData(0, "", 0f, 0f)
        }
        var age by remember { mutableStateOf(savedData.age.takeIf { it > 0 }?.toString() ?: "") }
        var gender by remember { mutableStateOf(savedData.gender) }
        var weight by remember { mutableStateOf(savedData.weight.takeIf { it > 0f }?.toString() ?: "") }
        var height by remember { mutableStateOf(savedData.height.takeIf { it > 0f }?.toString() ?: "") }
        var showGenderMenu by remember { mutableStateOf(false) }
        var errorMsg by remember { mutableStateOf("") }
        val genderOptions = listOf("Masculino", "Feminino", "Outro")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF4A148C))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "👤 PERFIL DO USUÁRIO",
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
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Seus Dados",
                        fontSize = 18.sp,
                        color = Color(0xFF4A148C)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = age,
                        onValueChange = { if (it.all { c -> c.isDigit() }) age = it },
                        label = { Text("Idade") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ExposedDropdownMenuBox(
                        expanded = showGenderMenu,
                        onExpandedChange = { showGenderMenu = !showGenderMenu }
                    ) {
                        OutlinedTextField(
                            value = gender,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Sexo") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showGenderMenu) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = showGenderMenu,
                            onDismissRequest = { showGenderMenu = false }
                        ) {
                            genderOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        gender = option
                                        showGenderMenu = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { if (it.isEmpty() || it.matches(Regex("[0-9]*\\.?[0-9]*"))) weight = it },
                        label = { Text("Peso (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = height,
                        onValueChange = { if (it.isEmpty() || it.matches(Regex("[0-9]*\\.?[0-9]*"))) height = it },
                        label = { Text("Altura (cm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    if (errorMsg.isNotEmpty()) {
                        Text(errorMsg, color = Color.Red, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Button(
                        onClick = {
                            val ageInt = age.toIntOrNull()
                            val weightFloat = weight.toFloatOrNull()
                            val heightFloat = height.toFloatOrNull()
                            if (ageInt == null || ageInt <= 0) {
                                errorMsg = "Idade inválida"
                            } else if (gender.isBlank()) {
                                errorMsg = "Selecione o sexo"
                            } else if (weightFloat == null || weightFloat <= 0f) {
                                errorMsg = "Peso inválido"
                            } else if (heightFloat == null || heightFloat <= 0f) {
                                errorMsg = "Altura inválida"
                            } else if (userId == null) {
                                errorMsg = "Usuário não identificado. Faça login novamente."
                            } else {
                                UserProfileManager.saveUserData(userId, ageInt, gender, weightFloat, heightFloat)
                                navigator.replace(MonitoringScreen)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A148C), contentColor = Color.White)
                    ) {
                        Text("Salvar")
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { navigator.pop() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF4A148C)
                )
            ) {
                Text("Menu Principal", modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }
}