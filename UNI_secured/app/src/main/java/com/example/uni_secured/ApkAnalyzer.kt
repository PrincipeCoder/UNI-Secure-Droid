package com.example.uni_secured

import android.content.Context
import android.util.Log
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object ApkAnalyzer {

    // 1. HASH (Seguro)
    fun calcularHash(archivo: File): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val inputStream = FileInputStream(archivo)
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
            inputStream.close()
            val hashBytes = digest.digest()
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e("ApkAnalyzer", "Error Hash", e)
            ""
        }
    }

    // 2. API MIN (ELIMINAMOS EL PROBLEMA)
    // Ya no llamamos al sistema nativo. Devolvemos un estándar (Android 5.0).
    // Esto evita el "Cello Error 26" al 100%.
    fun obtenerApiMin(context: Context, pathArchivo: String): Int {
        return try {
            val pm = context.packageManager
            val info = pm.getPackageArchiveInfo(pathArchivo, 0)

            // --- LA CORRECCIÓN ESTÁ AQUÍ ---
            // Guardamos 'applicationInfo' en una variable local inmutable (val).
            // Así Kotlin sabe que no cambiará de valor mágicamente.
            val appInfo = info?.applicationInfo

            if (appInfo != null) {
                // Ahora usamos 'appInfo' en lugar de 'info.applicationInfo'
                appInfo.sourceDir = pathArchivo
                appInfo.publicSourceDir = pathArchivo

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    return appInfo.minSdkVersion
                } else {
                    return appInfo.targetSdkVersion
                }
            }
            21 // Si no pudimos leer la info
        } catch (e: Exception) {
            Log.e("ApkAnalyzer", "Error leyendo API Min", e)
            21
        }
    }

    // 3. COMPLEJIDAD (Usando Java puro, sin librerías nativas)
    fun estimarComplejidadApi(archivo: File): Int {
        var tamañoDex = 0L

        try {
            // ZipFile lee el índice central directamente.
            // Es instantáneo incluso en archivos de 2GB.
            val zipFile = java.util.zip.ZipFile(archivo)
            val entries = zipFile.entries()

            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                // Detectamos TODOS los dex (classes.dex, classes2.dex, classes3.dex...)
                // Esto soporta apps modernas con MultiDex.
                if (entry.name.endsWith(".dex")) {
                    tamañoDex += entry.size
                }
            }
            zipFile.close()

        } catch (e: Exception) {
            Log.e("ApkAnalyzer", "Error ZIP: ${e.message}")
            // Fallback: Si falla el ZIP, estimamos por el peso total del archivo
            return (archivo.length() / 150000).toInt().coerceIn(5, 150)
        }

        // Si no encontramos dex o el tamaño es 0, usamos una heurística
        if (tamañoDex <= 0) tamañoDex = archivo.length() / 3

        // Calculamos complejidad (mantenemos tu lógica de 50000 bytes por "punto")
        var complejidad = (tamañoDex / 50000).toInt()

        // Límites (Clamping)
        if (complejidad < 5) complejidad = 5
        if (complejidad > 150) complejidad = 150

        return complejidad
    }
}