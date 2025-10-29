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
import com.example.unisecuredroid.data.models.StaticReport
import com.example.unisecuredroid.viewmodels.ReportViewModel
import com.itextpdf.text.Document
import com.itextpdf.text.Paragraph
import com.itextpdf.text.pdf.PdfWriter
import java.io.File
import java.io.FileOutputStream
import java.util.Date

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
        reportViewModel.fetchReport(context, jobId)
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
                        text = "Procesando análisis...",
                        fontSize = 18.sp,
                        color = themeColors.onBackground.copy(alpha = 0.8f)
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
    report: StaticReport,
    context: Context,
    themeColors: ColorScheme
) {
    var showScoreDetails by remember { mutableStateOf(false) }

    // 1. Lógica para determinar el color basado en el veredicto
    val verdictColor = when (report.verdict) {
        "MALICIOSO" -> Color.Red
        "Sospechoso" -> Color(0xFFFF9800)
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
        Text(text = "Job ID: ${report.jobId}", fontSize = 14.sp, color = themeColors.onBackground.copy(alpha = 0.6f))
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
                    text = report.verdict,
                    color = verdictColor,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // VENTANA MODAL (Alert Dialog)
        if (showScoreDetails) {
            AlertDialog(
                onDismissRequest = { showScoreDetails = false },
                title = { Text("Detalle Técnico del Veredicto", color = themeColors.onBackground) },
                text = {

                    LazyColumn {
                        item {
                            Text(
                                report.verdictDetails,
                                color = themeColors.onBackground,
                                fontSize = 14.sp
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

        // Detalle de Permisos (Card)
        LazyColumn(modifier = Modifier.weight(1f)) {
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
                        Text(text = "Permisos Detectados:", color = themeColors.primary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        report.permissions.forEach { permission ->
                            Text(
                                " • ${permission.substringAfterLast('.')}",
                                color = themeColors.onSurface,
                                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                            )
                        }
                    }
                }
            }

            // Sección de URLs/IPs Clasificadas
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("URLs/IPs Encontradas:", color = themeColors.primary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(report.urls) { item ->
                when {

                    item.startsWith("CAT_START:") -> {
                        val subtitle = item.substringAfter("CAT_START:")
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            subtitle,
                            color = themeColors.secondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        Divider(color = themeColors.secondary.copy(alpha = 0.5f), thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))
                    }

                    else -> {
                        Text(
                            " • $item",
                            color = themeColors.onSurface,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
                        )
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
                    val pdfFile = generarPDF(context, report)
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

// Función generar PDF
private fun generarPDF(context: Context, report: StaticReport): File? {
    return try {
        val pdfText = """
            UNIVERSIDAD NACIONAL DE INGENIERÍA
            Proyecto: UNI-SecureDroid
            Job ID: ${report.jobId}
            Fecha: ${Date()}

            REPORTE DE ANÁLISIS ESTÁTICO
            ------------------------------------

            VEREDICTO PRELIMINAR: ${report.verdict}
            
            DETALLES TÉCNICOS:
            ${report.verdictDetails}

            --- PERMISOS ---
            ${report.permissions.joinToString("\n") { " - ${it.substringAfterLast('.')}" }}

            --- URLs/IPs CLASIFICADAS ---
            ${report.urls.joinToString("\n") {
            if (it.startsWith("CAT_START:")) "\n${it.substringAfter(':', "CLASE DESCONOCIDA").uppercase()}\n" else " - $it"
        }} 

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