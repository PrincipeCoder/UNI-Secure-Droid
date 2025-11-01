package com.example.unisecuredroid.data

import android.content.Context
import android.net.Uri
import com.example.unisecuredroid.data.models.StaticReport
import com.example.unisecuredroid.data.TFLiteModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.dongliu.apk.parser.bean.ApkMeta
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.regex.Pattern
import java.util.zip.ZipFile


object StaticAnalyzer {

    // Mapa de Características para el modelo TFLite (Debe coincidir con el entrenamiento)
    private val FEATURE_MAP = mapOf(
        // Permisos (solo una selección, el modelo real podría tener más)
        "Permission::android.permission.INTERNET" to 0,
        "Permission::android.permission.READ_SMS" to 1,
        "Permission::android.permission.SEND_SMS" to 2,
        "Permission::android.permission.ACCESS_FINE_LOCATION" to 3,
        "Permission::android.permission.WRITE_EXTERNAL_STORAGE" to 4,
        "Permission::android.permission.CAMERA" to 5,
        "Permission::android.permission.RECORD_AUDIO" to 6,
        // APIs/Intents (selección)
        "APIcall::java.lang.reflect.Method.invoke" to 7,
        "APIcall::android.telephony.SmsManager" to 8,
        "APIcall::Runtime.exec" to 9,
        "APIcall::android.content.Intent.ACTION_BOOT_COMPLETED" to 10,
        "APIcall::view/MotionEvent.obtain" to 11,
        "APIcall::location/LocationManager.getLastKnownLocation" to 12,
        "APIcall::crypto/Cipher.getInstance" to 13,
        "APIcall::os/PowerManager.newWakeLock" to 14,
        "APIcall::telephony/TelephonyManager.getDeviceId" to 15,
        "APIcall::telephony/TelephonyManager.getSubscriberId" to 16,
        "APIcall::net/wifi/WifiManager.setWifiEnabled" to 17,
        "APIcall::os/Process.killProcess" to 18,
        "APIcall::os/Debug.isDebuggerConnected" to 19,
        "APIcall::dex/DexClassLoader" to 20,
        "APIcall::content/ContentResolver.query" to 21,
        "APIcall::content/ContentResolver.registerContentObserver" to 22,
        "APIcall::app/NotificationManager.notify" to 23,
        "APIcall::app/admin/DevicePolicyManager" to 24,
        "APIcall::accounts/AccountManager.getAccounts" to 25,
        "APIcall::bluetooth/BluetoothAdapter.getDefaultAdapter" to 26,
        "APIcall::hardware/Camera.open" to 27,
        "APIcall::graphics/Bitmap.recycle() or BitmapTracker.recycle()" to 28,
        "APIcall::view/LinearLayout.requestFocus()" to 29,
        "APIcall::view/ViewTreeObserver.removeGlobalOnLayoutListener()" to 30,
        "APIcall::view/ViewGroup.isEditMode()" to 31,
        "APIcall::os/Bundle.getBoolean()" to 32,
        "APIcall::view/View.isShow" to 33,
        // Último índice para la característica numérica de conteo
        "URL_COUNT_FEATURE" to 34
    )

    const val FEATURE_SIZE = 35

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
                        // Lectura de contenido binario/crudo para buscar URLs/IPs
                        val rawContent = inputStream.bufferedReader(Charsets.UTF_8).readText()

                        // CORRECCIÓN para API < 34: Usar while(matcher.find()) en lugar de .results()

                        // Extracción de URLs
                        val urlMatcher = urlPattern.matcher(rawContent)
                        while (urlMatcher.find()) {
                            rawFindings.add(urlMatcher.group())
                        }

                        // Extracción de IPs
                        val ipMatcher = ipPattern.matcher(rawContent)
                        while (ipMatcher.find()) {
                            val ip = ipMatcher.group()
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

    // --- 3. FUNCIÓN: Extraer APIs/Intents (Simulación) ---
    private suspend fun extractApisAndIntents(context: Context, apkUri: Uri): List<String> = withContext(Dispatchers.IO) {
        // SIMULACIÓN para usar en la vectorización TFLite.
        delay(50)
        return@withContext listOf(
            "android.telephony.SmsManager.sendTextMessage", // Para conteo de APIs críticas
            "java.lang.reflect.Method.invoke", // Feature mapeada: APIcall::java.lang.reflect.Method.invoke
            "android.content.Intent.ACTION_BOOT_COMPLETED",
            "android.view.MotionEvent.obtain" // Feature mapeada: APIcall::view/MotionEvent.obtain
        )
    }


    // --- 4. FUNCIÓN: Vectorización de Características (Input para TFLite) ---
    private fun vectorizeFeatures(
        permissions: List<String>,
        urls: List<String>,
        apis: List<String>
    ): FloatArray {
        val featureVector = FloatArray(FEATURE_SIZE) // Vector inicializado a 0.0f

        fun markFeature(name: String) {
            // Buscamos si el nombre del perm/api coincide con alguna clave del mapa
            FEATURE_MAP.entries.find { (key, _) ->
                // Buscamos la coincidencia del nombre completo o parcial
                name.contains(key.substringAfter("::")) || name.contains(key)
            }?.let { entry ->
                featureVector[entry.value] = 1.0f // Marcar como presente (1.0f)
            }
        }

        // 1. Mapeo Binario de Permisos y APIs
        permissions.forEach { perm ->
            markFeature("Permission::$perm")
        }

        apis.forEach { api ->
            // Normalizamos el nombre para el mapeo
            markFeature("APIcall::$api")
        }

        // 2. Características Numéricas Adicionales (Conteo de URLs, Índice 34)
        var thirdPartyUrlCount = 0
        urls.forEach { url ->
            // Contamos solo los ítems que NO son encabezados de categoría
            if (!url.startsWith("CAT_START:")) {
                thirdPartyUrlCount++
            }
        }

        // Usamos el último índice para la característica numérica de conteo
        val urlCountIndex = FEATURE_MAP["URL_COUNT_FEATURE"]!!

        // Normalización simple: dividir por 50.0f (ejemplo de escala 0-1)
        featureVector[urlCountIndex] = thirdPartyUrlCount.toFloat() / 50.0f

        return featureVector
    }

    // --- 5. FUNCIÓN: runTfLiteInference (Ejecución del Modelo IA) ---
    private fun runTfLiteInference(
        context: Context,
        featureVector: FloatArray
    ): Triple<String, String, Float?> {
        // Asume que TFLiteModel está definido correctamente en TFLiteModel.kt
        val tfliteModel = TFLiteModel(context)

        if (tfliteModel.loadModel().not()) {
            return Triple("ERROR IA", "Fallo al cargar el modelo TFLite. Verifique 'assets/model.tflite'.", null)
        }

        val probability = tfliteModel.runInference(featureVector)
        tfliteModel.close()

        return if (probability != null) {
            val verdict = when {
                probability >= 0.90f -> "MALICIOSO (IA: ${String.format(Locale.US, "%.2f", probability * 100)}%)"
                probability >= 0.50f -> "Sospechoso (IA: ${String.format(Locale.US, "%.2f", probability * 100)}%)"
                else -> "Benigno (IA: ${String.format(Locale.US, "%.2f", probability * 100)}%)"
            }
            val details = "Veredicto basado en IA (TFLite).\nProbabilidad de ser Malicioso: ${String.format(Locale.US, "%.2f", probability * 100)}%"
            Triple(verdict, details, probability)
        } else {
            Triple("ERROR IA", "Fallo en la inferencia TFLite. Vector de entrada incorrecto.", null)
        }
    }


    // --- 6. FUNCIÓN: performStaticAnalysis (Orquestación) ---
    suspend fun performStaticAnalysis(context: Context, apkUri: Uri, jobId: String): StaticReport {
        val startTime = System.currentTimeMillis()

        // 1. Hash SHA-256
        val tempFileForHash = copyUriToTempFile(context, apkUri, "hash")
        val sha256Hash = if (tempFileForHash != null) {
            calculateSha256(tempFileForHash).also { tempFileForHash.delete() }
        } else {
            "SHA-256_FAILED"
        }

        // 2. Extracción de Características
        val realPermissions = extractPermissionsWithApkParser(context, apkUri)
        val realUrlsIps = extractUrlsAndIps(context, apkUri)
        val detectedApis = extractApisAndIntents(context, apkUri)

        // 3. Vectorización e Inferencia IA
        val featureVector = vectorizeFeatures(realPermissions, realUrlsIps, detectedApis)
        val (verdict, verdictDetails, aiProbability) = runTfLiteInference(context, featureVector)

        val totalTime = System.currentTimeMillis() - startTime
        println("Análisis completado en: ${totalTime}ms.")

        // 4. Devolver el Reporte (Asumiendo que StaticReport ha sido actualizado para incluir aiProbability y apisDetected)
        return StaticReport(
            jobId = jobId,
            sha256 = sha256Hash,
            verdict = verdict,
            aiProbability = aiProbability,
            permissions = realPermissions,
            urls = realUrlsIps,
            apisDetected = detectedApis,
            verdictDetails = "$verdictDetails\nVector de Entrada (Debug): ${featureVector.joinToString(separator = ", ", limit = 10)}"
        )
    }
}
