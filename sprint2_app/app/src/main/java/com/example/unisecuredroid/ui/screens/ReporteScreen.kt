package com.example.unisecuredroid.ui.screens

import android.content.Context
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
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
import com.example.unisecuredroid.data.models.AnalysisReport
import com.example.unisecuredroid.viewmodels.ReportViewModel
import com.itextpdf.text.Document
import com.itextpdf.text.Paragraph
import com.itextpdf.text.pdf.PdfWriter
import java.io.File
import java.io.FileOutputStream
import java.util.Date
import java.util.Locale

@Composable
fun ReporteScreen(
    navController: NavController,
    jobId: String,
    reportViewModel: ReportViewModel = viewModel()
) {
    val context = LocalContext.current
    val reportState by reportViewModel.reportState.collectAsState()
    val themeColors = MaterialTheme.colorScheme

    LaunchedEffect(jobId) {
        reportViewModel.fetchReport(jobId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(themeColors.background),
        contentAlignment = Alignment.Center
    ) {
        when (val state = reportState) {
            is ReportViewModel.ReportState.Loading -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = themeColors.secondary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Realizando Análisis Completo...",
                        fontSize = 18.sp,
                        color = themeColors.onBackground.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "(Estático + Dinámico)",
                        fontSize = 14.sp,
                        color = themeColors.onBackground.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Job ID: $jobId",
                        fontSize = 14.sp,
                        color = themeColors.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
            is ReportViewModel.ReportState.Success -> {
                ReportView(
                    navController = navController,
                    report = state.report,
                    context = context,
                    themeColors = themeColors
                )
            }
            is ReportViewModel.ReportState.Error -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 32.dp)
                ) {
                    Text(
                        text = "Error en Análisis",
                        fontSize = 22.sp,
                        color = Color.Red,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.message,
                        color = Color.Red,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )
                    Button(
                        onClick = { navController.popBackStack() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.surface),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp, pressedElevation = 2.dp),
                        modifier = Modifier.height(50.dp)
                    ) {
                        Text("Volver", color = themeColors.secondary, fontWeight = FontWeight.Bold)
                    }
                }
            }
            else -> {}
        }
    }
}

@Composable
private fun ReportView(
    navController: NavController,
    report: AnalysisReport,
    context: Context,
    themeColors: ColorScheme
) {
    var showScoreDetails by remember { mutableStateOf(false) }

    val verdictColor = when {
        report.verdict.contains("MALICIOSO", ignoreCase = true) -> Color.Red
        report.verdict.contains("Sospechoso", ignoreCase = true) -> Color(0xFFFF9800)
        else -> Color(0xFF4CAF50)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(text = "Reporte de Análisis", fontSize = 26.sp, color = themeColors.onBackground, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        // El Job ID se mostrará ahora en la Card de detalles
        // Text(text = "Job ID: ${report.jobId}", fontSize = 14.sp, color = themeColors.onBackground.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Veredicto Preliminar",
                        fontSize = 16.sp,
                        color = themeColors.onSurface.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))

                    // BOTÓN DE JUSTIFICACIÓN
                    IconButton(
                        onClick = { showScoreDetails = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = "Mostrar detalles del score",
                            tint = themeColors.secondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = report.verdict, // Veredicto de IA (ej: MALICIOSO (IA: 98%))
                    color = verdictColor,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                val score = String.format(Locale.US, "%.2f", report.aiProbability * 100)
                Text(
                    text = "Riesgo: $score% | ${report.risk}",
                    color = themeColors.onSurface.copy(alpha = 0.9f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Familia: ${report.family}",
                    color = themeColors.onSurface.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }
        }

        // VENTANA MODAL (Alert Dialog) para Justificación
        if (showScoreDetails) {
            AlertDialog(
                onDismissRequest = { showScoreDetails = false },
                title = { Text("Detalle Técnico del Veredicto", color = themeColors.onBackground) },
                text = {
                    LazyColumn {
                        item {
                            Text(
                                "Análisis completado en ${report.analysisTimeSeconds}s",
                                color = themeColors.onBackground,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Timestamp: ${report.timestamp}",
                                color = themeColors.onBackground.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { showScoreDetails = false }) {
                        Text("Cerrar")
                    }
                },
                containerColor = themeColors.surface
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // LazyColumn principal para Permisos y URLs
        LazyColumn(modifier = Modifier.weight(1f)) {

            // --- NUEVO BLOQUE: DETALLES DEL ARCHIVO (HASH RF-3) ---
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Detalles del Archivo", color = themeColors.primary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Muestra el Job ID y el Hash
                        DetailRow(label = "Job ID:", value = report.jobId, themeColors = themeColors)
                        DetailRow(label = "SHA-256:", value = report.sha256, themeColors = themeColors)
                    }
                }
            }
            // ----------------------------------------------------

            // --- PERMISOS DETECTADOS (Card) ---
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "APIs/Intents Sospechosos:",
                            color = themeColors.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (report.staticAnalysis.apisDetected.isEmpty()) {
                            Text("No se detectaron APIs/Intents de alto riesgo.", color = themeColors.onSurface.copy(alpha = 0.7f))
                        } else {
                            report.staticAnalysis.apisDetected.forEach { api ->
                                Text(
                                    " • $api",
                                    color = themeColors.onSurface,
                                    modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
            // ------------------------------------

            // Sección de URLs/IPs Clasificadas
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("URLs/IPs Encontradas:", color = themeColors.primary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(report.staticAnalysis.topSignals) { signal ->
                Text(
                    " • $signal",
                    color = themeColors.onSurface,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Análisis Dinámico (Sandbox)", color = themeColors.primary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text("Conexiones de Red:", fontWeight = FontWeight.SemiBold, color = themeColors.onSurface)
                        report.dynamicFeatures.network.connections.forEach {
                            Text(" • $it", fontSize = 14.sp, color = themeColors.onSurface)
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Operaciones de Archivos:", fontWeight = FontWeight.SemiBold, color = themeColors.onSurface)
                        report.dynamicFeatures.fileOperations.forEach {
                            Text(" • ${it.action}: ${it.path}", fontSize = 14.sp, color = themeColors.onSurface)
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Datos enviados: ${report.dynamicFeatures.network.dataSentKb} KB", fontSize = 14.sp, color = themeColors.onSurface)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    val pdfFile = generarPDF(report)
                    val mensaje = if (pdfFile != null) {
                        "PDF guardado en Descargas"
                    } else { "Error al generar PDF." }
                    Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.secondary),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp, pressedElevation = 2.dp),
                modifier = Modifier.height(50.dp)
            ) {
                Text("Descargar PDF", color = themeColors.onSecondary, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { navController.popBackStack("home", inclusive = false) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.surface),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp, pressedElevation = 2.dp),
                modifier = Modifier.height(50.dp)
            ) {
                Text("Analizar Otro", color = themeColors.secondary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// --- FUNCIÓN HELPER: Muestra detalles en dos columnas ---
@Composable
fun DetailRow(label: String, value: String, themeColors: ColorScheme) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontWeight = FontWeight.SemiBold, color = themeColors.onSurface.copy(alpha = 0.8f))

        // Truncar el SHA-256 para una mejor visualización en pantalla
        val displayValue = if (value.length > 20 && label.contains("SHA-256")) {
            "${value.substring(0, 16)}..."
        } else {
            value
        }
        Text(text = displayValue, color = themeColors.onSurface, fontSize = 14.sp)
    }
}

// Función generar PDF
private fun generarPDF(report: AnalysisReport): File? {
    return try {
        // Formatear la probabilidad para el PDF
        val aiScore = String.format(Locale.US, "%.2f%%", report.aiProbability * 100)
        
        val signalsText = report.staticAnalysis.topSignals.joinToString("\n") { " - $it" }
        val apisText = if (report.staticAnalysis.apisDetected.isEmpty()) " - No se detectaron APIs/Intents de riesgo." else report.staticAnalysis.apisDetected.joinToString("\n") { " - $it" }
        val connectionsText = report.dynamicFeatures.network.connections.joinToString("\n") { " - $it" }
        val filesText = report.dynamicFeatures.fileOperations.joinToString("\n") { " - ${it.action}: ${it.path}" }

        val pdfText = """
            UNIVERSIDAD NACIONAL DE INGENIERÍA
            Proyecto: UNI-SecureDroid
            Job ID: ${report.jobId}
            Hash SHA-256: ${report.sha256}
            Fecha: ${Date()}

            REPORTE DE ANÁLISIS ESTÁTICO
            ------------------------------------

            VEREDICTO: ${report.verdict}
            RIESGO: ${report.risk}
            FAMILIA: ${report.family}
            PROBABILIDAD IA: $aiScore
            TIEMPO DE ANÁLISIS: ${report.analysisTimeSeconds}s

            --- SEÑALES ESTÁTICAS ---
            $signalsText

            --- APIs / INTENTS SOSPECHOSOS ---
            $apisText

            --- ANÁLISIS DINÁMICO (SANDBOX) ---
            Conexiones de Red:
            $connectionsText
            
            Operaciones de Archivos:
            $filesText
            
            Datos Enviados: ${report.dynamicFeatures.network.dataSentKb} KB

        """.trimIndent()

        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloads.exists()) {
            downloads.mkdirs()
        }
        val file = File(downloads, "reporte_unisecuredroid_${report.jobId}.pdf")

        val pdfDoc = Document()
        val outputStream = FileOutputStream(file)
        PdfWriter.getInstance(pdfDoc, outputStream)

        pdfDoc.open()
        pdfDoc.add(Paragraph(pdfText))
        pdfDoc.close()

        outputStream.close()

        file
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
