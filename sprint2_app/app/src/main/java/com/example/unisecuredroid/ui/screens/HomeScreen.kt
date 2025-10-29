package com.example.unisecuredroid.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.unisecuredroid.viewmodels.UploadViewModel
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    navController: NavController,
    uploadViewModel: UploadViewModel = viewModel()
) {
    val context = LocalContext.current
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }

    val uploadState by uploadViewModel.uploadState.collectAsState()
    val scope = rememberCoroutineScope()
    val themeColors = MaterialTheme.colorScheme
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedFileUri = it
            val cursor = context.contentResolver.query(it, null, null, null, null)
            cursor?.use { c ->
                val nameIndex = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (c.moveToFirst()) {
                    selectedFileName = c.getString(nameIndex)
                }
            }
            uploadViewModel.resetState()
        }
    }

    LaunchedEffect(uploadState) {
        if (uploadState is UploadViewModel.UploadState.Success) {
            val jobId = (uploadState as UploadViewModel.UploadState.Success).jobId
            navController.navigate("reporte/$jobId")
            uploadViewModel.resetState()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(themeColors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Text(
                text = "Portal de Carga",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = themeColors.onBackground,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "UNI-SecureDroid",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = themeColors.primary,
                modifier = Modifier.padding(bottom = 40.dp)
            )

            // Botón Cargar Archivo
            Button(
                onClick = { filePicker.launch("*/*") }, // Permitimos cualquier archivo
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.surface),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp, pressedElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    "Cargar archivo (.apk)",
                    color = themeColors.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Nombre archivo seleccionado
            selectedFileName?.let {
                Text(
                    "Archivo: $it",
                    color = themeColors.onBackground.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 40.dp)
                )
            }
            if(selectedFileName == null) {
                Spacer(modifier = Modifier.height(60.dp)) // Reserve space
            }

            // Mensaje de Error
            if (uploadState is UploadViewModel.UploadState.Error) {
                Text(
                    (uploadState as UploadViewModel.UploadState.Error).message,
                    color = Color.Red,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(24.dp)) // Keep space consistent
            }

            // Botón Analizar Archivo / Indicador Carga
            if (uploadState is UploadViewModel.UploadState.Loading) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = themeColors.secondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Subiendo y procesando...", color = themeColors.onBackground.copy(alpha = 0.8f))
                }
            } else if (uploadState !is UploadViewModel.UploadState.Success) {
                // Botón Analizar (Azul)
                Button(
                    onClick = {
                        if (selectedFileUri != null && selectedFileName != null) {
                            scope.launch {
                                uploadViewModel.uploadFile(context, selectedFileName!!, selectedFileUri!!)
                            }
                        }
                    },
                    enabled = selectedFileUri != null,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = themeColors.secondary,
                        disabledContainerColor = themeColors.surface.copy(alpha = 0.5f)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp, pressedElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        "Analizar Archivo",
                        color = themeColors.onSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}