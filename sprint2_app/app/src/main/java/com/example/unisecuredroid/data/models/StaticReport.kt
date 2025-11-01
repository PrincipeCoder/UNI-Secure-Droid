package com.example.unisecuredroid.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Representa el informe completo del análisis estático de un archivo APK.
 *
 * @property jobId Identificador único de la tarea de análisis.
 * @property sha256 Hash SHA-256 del archivo APK (Identificación).
 * @property verdict Veredicto final del análisis (e.g., "MALICIOSO (IA: 98%)", "Benigno").
 * @property aiProbability Probabilidad bruta de ser malicioso devuelta por el modelo TFLite (0.0 a 1.0).
 * @property permissions Lista de permisos de Android solicitados.
 * @property urls Lista de URLs e IPs encontradas en el código DEX.
 * @property apisDetected (RF-7 al RF-11) Lista de APIs sospechosas o relevantes detectadas (e.g., Reflection, Crypto).
 * @property verdictDetails Justificación y detalles del veredicto (incluyendo debug del vector IA).
 */
@Parcelize
data class StaticReport(
    val jobId: String,
    val sha256: String,
    val verdict: String,
    val aiProbability: Float?, // Probabilidad de la IA (null si falló)
    val permissions: List<String>,
    val urls: List<String>,
    val apisDetected: List<String> = emptyList(), // Cumplimiento de RF-7 a RF-11
    val verdictDetails: String
) : Parcelable
