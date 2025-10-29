package com.example.unisecuredroid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.unisecuredroid.viewmodels.AuthViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    var user by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val authState by authViewModel.authState.collectAsState()
    val scope = rememberCoroutineScope()
    val themeColors = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(themeColors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "UNI-SecureDroid",
                fontSize = 28.sp,
                color = themeColors.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            OutlinedTextField(
                value = user,
                onValueChange = { user = it },
                label = { Text("Usuario", color = themeColors.onBackground.copy(alpha = 0.7f)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = themeColors.primary,
                    unfocusedIndicatorColor = themeColors.onBackground.copy(alpha = 0.5f),
                    focusedLabelColor = themeColors.primary,
                    unfocusedLabelColor = themeColors.onBackground.copy(alpha = 0.7f),
                    cursorColor = themeColors.primary,
                    focusedTextColor = themeColors.onBackground,
                    unfocusedTextColor = themeColors.onBackground,
                    unfocusedContainerColor = themeColors.background,
                    focusedContainerColor = themeColors.background
                ),
                modifier = Modifier.width(280.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña", color = themeColors.onBackground.copy(alpha = 0.7f)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = themeColors.primary,
                    unfocusedIndicatorColor = themeColors.onBackground.copy(alpha = 0.5f),
                    focusedLabelColor = themeColors.primary,
                    unfocusedLabelColor = themeColors.onBackground.copy(alpha = 0.7f),
                    cursorColor = themeColors.primary,
                    focusedTextColor = themeColors.onBackground,
                    unfocusedTextColor = themeColors.onBackground,
                    unfocusedContainerColor = themeColors.background,
                    focusedContainerColor = themeColors.background
                ),
                modifier = Modifier.width(280.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (authState is AuthViewModel.AuthState.Error) {
                Text(
                    (authState as AuthViewModel.AuthState.Error).message,
                    color = Color.Red,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(24.dp))
            }


            if (authState is AuthViewModel.AuthState.Loading) {
                CircularProgressIndicator(color = themeColors.secondary)
            } else if (authState !is AuthViewModel.AuthState.Success) {

                Button(
                    onClick = {
                        scope.launch { authViewModel.login(user, password) }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = themeColors.secondary
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 2.dp
                    ),
                    modifier = Modifier
                        .width(180.dp)
                        .height(50.dp)
                ) {
                    Text(
                        "Iniciar sesión",
                        color = themeColors.onSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (authState is AuthViewModel.AuthState.Success) {
                LaunchedEffect(Unit) { onLoginSuccess() }
            }
        }
    }
}