package com.example.unisecuredroid.data

import android.content.Context
import android.net.Uri
import com.example.unisecuredroid.data.models.StaticReport
import net.dongliu.apk.parser.ApkFile
import net.dongliu.apk.parser.bean.ApkMeta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.regex.Pattern
import java.util.zip.ZipFile
import kotlinx.coroutines.delay
import kotlin.random.Random

object StaticAnalyzer {

    // Función de utilidad: Copiar Uri a File
    private fun copyUriToTempFile(context: Context, apkUri: Uri, tag: String): File? {
        val tempFile = File.createTempFile("temp_apk_$tag", ".apk", context.cacheDir)
        return try {
            context.contentResolver.openInputStream(apkUri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        } catch (e: Exception) {
            println("Error copiando Uri a archivo temporal: ${e.message}")
            tempFile.delete()
            null
        }
    }

    // Función: Calcula el hash SHA-256

    private fun calculateSha256(tempFile: File): String {
        return try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            tempFile.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            println("Error calculando SHA-256: ${e.message}")
            "SHA-256_ERROR"
        }
    }

    // --- 1. FUNCIÓN: Extraer permisos ---
    private suspend fun extractPermissionsWithApkParser(context: Context, apkUri: Uri): List<String> = withContext(Dispatchers.IO) {
        val permissions = mutableListOf<String>()
        var tempFile: File? = null
        try {
            tempFile = copyUriToTempFile(context, apkUri, "perms") ?: return@withContext listOf("Error: No se pudo preparar el archivo APK.")

            net.dongliu.apk.parser.ApkFile(tempFile).use { apkFile ->
                val apkMeta: ApkMeta = apkFile.apkMeta
                permissions.addAll(apkMeta.usesPermissions)
            }
        } catch (e: Exception) {
            println("Error general en extractPermissions: ${e.message}")
            permissions.add("Error al leer archivo APK (Permisos)")
        } finally {
            tempFile?.delete()
        }
        return@withContext permissions
    }


    // --- 2. FUNCIÓN: Extraer URLs/IPs (Escaneo del DEX y CLASIFICACIÓN) ---
    private suspend fun extractUrlsAndIps(context: Context, apkUri: Uri): List<String> = withContext(Dispatchers.IO) {
        val rawFindings = mutableSetOf<String>()
        var tempFile: File? = null

        val urlPattern = Pattern.compile("""https?://[a-zA-Z0-9./\-_:%#?=&~]+""")
        val ipPattern = Pattern.compile("""\b(?:[0-9]{1,3}\.){3}[0-9]{1,3}\b""")

        val categorizedFindings = mutableMapOf<String, MutableSet<String>>()

        try {
            tempFile = copyUriToTempFile(context, apkUri, "urls") ?: return@withContext listOf("Error en escaneo DEX: No se pudo copiar el archivo.")

            ZipFile(tempFile).use { zipFile ->
                val dexEntry = zipFile.getEntry("classes.dex")

                if (dexEntry != null) {
                    zipFile.getInputStream(dexEntry).use { inputStream ->
                        val rawContent = inputStream.bufferedReader(Charsets.UTF_8).readText()

                        urlPattern.matcher(rawContent).results().forEach { matchResult ->
                            rawFindings.add(matchResult.group())
                        }
                        ipPattern.matcher(rawContent).results().forEach { matchResult ->
                            val ip = matchResult.group()
                            if (!ip.startsWith("0.") && ip.split('.').all { it.toIntOrNull() in 0..255 }) {
                                rawFindings.add(ip)
                            }
                        }
                    }
                } else {
                    rawFindings.add("ADVERTENCIA: classes.dex no encontrado.")
                }
            }

            // FASE DE CLASIFICACIÓN
            rawFindings.forEach { finding ->
                val category = when {
                    finding.contains("127.0.0.1") || finding.contains("localhost") || finding.startsWith("224.") -> "IPs Locales y Multicast"
                    finding.contains("http://schemas.android.com") || finding.contains("w3.org") || finding.contains("xml.org") || finding.contains("log4j") -> "Ruido Técnico y Esquemas"
                    finding.contains("google.com") || finding.contains("developer.android.com") || finding.contains("github.com") || finding.contains("mozilla.org") -> "Puntos de Contacto Legítimos"
                    finding.contains(".cloud") || finding.contains("issue") || finding.contains("api.") || finding.contains("exfil") -> "Servicios Cloud/API"
                    else -> "URLs de Terceros (Revisar)"
                }
                categorizedFindings.getOrPut(category) { mutableSetOf() }.add(finding)
            }

        } catch (e: Exception) {
            println("Error general en extractUrlsAndIps (Lectura Cruda): ${e.message}")
            e.printStackTrace()
            return@withContext listOf("Error en escaneo DEX: ${e.message}")
        } finally {
            tempFile?.delete()
        }

        // --- FASE DE FORMATO DE SALIDA ---
        val finalReportList = mutableListOf<String>()
        val categoryOrder = listOf("URLs de Terceros (Revisar)", "Servicios Cloud/API", "IPs Locales y Multicast", "Puntos de Contacto Legítimos", "Ruido Técnico y Esquemas")

        categoryOrder.forEach { category ->
            val items = categorizedFindings[category]
            if (!items.isNullOrEmpty()) {
                finalReportList.add("CAT_START:$category")
                finalReportList.addAll(items.toList().sorted())
            }
        }

        return@withContext finalReportList.toList()
    }

    // --- LÓGICA DEL VEREDICTO FINAL (SCORING con Umbrales Anti-FP) ---
    private fun calculateVerdict(permissions: List<String>, urls: List<String>): Pair<String, String> {
        var score = 0
        var bonusDeConfianza = 0

        val MALICIOUS_THRESHOLD = 10
        val SUSPICIOUS_THRESHOLD = 4
        val explanation = StringBuilder()

        // --- 1. Scoring de Permisos con Justificación ---
        explanation.append("El veredicto se basa en la Puntuación de Riesgo Acumulado:\n\n")
        explanation.append("--- INDICADORES POR PERMISOS ---\n")
        permissions.forEach { perm ->
            when {
                // RIESGO CRÍTICO (5 puntos)
                perm.contains("READ_SMS") -> { score += 5; explanation.append("- [ALTO]: Permiso crítico de lectura de SMS.\n") }
                perm.contains("BIND_DEVICE_ADMIN") -> { score += 5; explanation.append("- [ALTO]: Permiso crítico de administrador (control total).\n") }

                // RIESGO MEDIO (2 puntos)
                perm.contains("CALL_LOG") -> { score += 2; explanation.append("- [MEDIO]: Acceso al registro de llamadas.\n") }
                perm.contains("LOCATION") -> { score += 2; explanation.append("- [MEDIO]: Acceso a la ubicación del dispositivo.\n") }

                // RIESGO BAJO (1 punto - Reducido para evitar FPs)
                perm.contains("CAMERA") -> { score += 1; explanation.append("- [BAJO]: Uso de la cámara.\n") }
                perm.contains("CONTACTS") -> { score += 1; explanation.append("- [BAJO]: Acceso a contactos.\n") }
            }
        }
        explanation.append("\n")


        // --- 2. Scoring de URLs/IPs con Justificación ---
        explanation.append("--- INDICADORES DE RED ---\n")
        urls.forEach { url ->
            when {
                // REDUCCIÓN DE RIESGO por fuente confiable
                url.contains("play.google.com") || url.contains("googleapis.com") -> {
                    bonusDeConfianza -= 3;
                    explanation.append("- [CONFIANZA]: Conexión a servicios oficiales de Google/Play Store (Riesgo REDUCIDO).\n")
                }

                // Patrones de Riesgo
                url.contains("exfil") || url.contains("c2server") -> { score += 5; explanation.append("- [ALTO]: Keyword C2/Exfil (Posible Comando y Control).\n") }
                url.matches(Regex("""\b(192\.168\.|172\.(1[6-9]|2[0-9]|3[0-1])\.|10\.)\d{1,3}\.\d{1,3}\b""")) -> { score += 2; explanation.append("- [MEDIO]: IP Privada (Comunicación no pública).\n") }
                url.length > 50 -> { score += 1; explanation.append("- [BAJO]: URL Larga (Posible ofuscación).\n") }
            }
        }
        explanation.append("\n")

        // Factor de confianza al score base
        val finalScore = score + bonusDeConfianza

        // 3. Determinación del Veredicto
        val verdict = when {
            finalScore >= MALICIOUS_THRESHOLD -> "MALICIOSO"
            finalScore >= SUSPICIOUS_THRESHOLD -> "Sospechoso"
            else -> "Benigno"
        }

        // 4. Finalizamos la explicación
        explanation.insert(0, "Puntuación de Riesgo Total: $finalScore\n\nVEREDICTO FINAL: $verdict\n\n")

        return Pair(verdict, explanation.toString())
    }


    // --- 4. FUNCIÓN: performStaticAnalysis (Orquestación) ---
    suspend fun performStaticAnalysis(context: Context, apkUri: Uri, jobId: String): StaticReport {
        println("---- Extrayendo datos REALES (Última versión estable) ----")

        // 1. Calcular el Hash SHA-256 (RF-3)
        // Se requiere copiar el archivo APK una vez más para el cálculo del hash.
        val tempFileForHash = copyUriToTempFile(context, apkUri, "hash")
        val sha256Hash = if (tempFileForHash != null) {
            // Calcula el hash y luego borra el archivo temporal
            calculateSha256(tempFileForHash).also { tempFileForHash.delete() }
        } else {
            "SHA-256_FAILED"
        }

        // 2. Extraer Permisos y URLs (Análisis)
        val realPermissions = extractPermissionsWithApkParser(context, apkUri)
        val realUrlsIps = extractUrlsAndIps(context, apkUri)

        // 3. Calcular Veredicto y Justificación
        val (verdict, verdictDetails) = calculateVerdict(realPermissions, realUrlsIps)

        delay(200)

        // 4. Devolver el Reporte
        return StaticReport(
            jobId = jobId,
            sha256 = sha256Hash,
            verdict = verdict,
            permissions = realPermissions,
            urls = realUrlsIps,
            verdictDetails = verdictDetails
        )
    }
}