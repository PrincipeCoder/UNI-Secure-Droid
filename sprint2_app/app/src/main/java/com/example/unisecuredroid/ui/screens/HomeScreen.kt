package com.example.unisecuredroid.ui.screens

import android.provider.OpenableColumns
import android.widget.Toast
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
import com.example.unisecuredroid.viewmodels.UploadViewModel.AnalysisState // Importar el estado correcto

@Composable
fun HomeScreen(
    navController: NavController,
    uploadViewModel: UploadViewModel = viewModel()
) {
    val context = LocalContext.current
    var selectedFileName by remember { mutableStateOf<String?>(null) }

    // Usamos el estado correcto del ViewModel
    val analysisState by uploadViewModel.analysisState.collectAsState()
    val themeColors = MaterialTheme.colorScheme

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            // 1. Obtener el nombre del archivo
            val cursor = context.contentResolver.query(it, null, null, null, null)
            cursor?.use { c ->
                val nameIndex = c.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
                if (c.moveToFirst()) {
                    selectedFileName = c.getString(nameIndex)
                }
            }
            // 2. Notificar al ViewModel que el archivo ha sido seleccionado
            uploadViewModel.setApkUri(it)
        }
    }

    // --- LaunchedEffect: Manejo de Navegación y Errores ---
    LaunchedEffect(analysisState) {
        when (analysisState) {
            is AnalysisState.Success -> {
                val jobId = (analysisState as AnalysisState.Success).jobId
                navController.navigate("reporte/$jobId")
                uploadViewModel.resetState() // Limpiar el estado después de la navegación
            }
            is AnalysisState.Error -> {
                val message = (analysisState as AnalysisState.Error).message
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
            else -> {}
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
                text = "Portal de Análisis",
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
                // Permitir solo APKs (aunque GetContent es amplio, el filtro es esencial)
                onClick = { filePicker.launch("application/vnd.android.package-archive") },
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

            // Nombre archivo seleccionado (si el estado es FileSelected o Analyzing)
            if (analysisState is AnalysisState.FileSelected || analysisState is AnalysisState.Analyzing) {
                selectedFileName?.let {
                    Text(
                        "Archivo: $it",
                        color = themeColors.onBackground.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 40.dp)
                    )
                } ?: Spacer(modifier = Modifier.height(60.dp))
            } else {
                Spacer(modifier = Modifier.height(60.dp)) // Reserve space
            }

            // Mensaje de Error
            if (analysisState is AnalysisState.Error) {
                Text(
                    (analysisState as AnalysisState.Error).message,
                    color = Color.Red,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Botón Analizar Archivo / Indicador Carga
            if (analysisState is AnalysisState.Analyzing) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = themeColors.secondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Ejecutando Análisis Estático (IA)...",
                        color = themeColors.onBackground.copy(alpha = 0.8f)
                    )
                }
            } else if (analysisState !is AnalysisState.Success) {
                // Botón Analizar (Azul), habilitado solo si el estado es FileSelected
                Button(
                    onClick = {
                        uploadViewModel.startStaticAnalysis(context)
                    },
                    enabled = analysisState is AnalysisState.FileSelected,
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
                        "Analizar Archivo con IA",
                        color = themeColors.onSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}