package com.example.uni_secured

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import kotlinx.coroutines.delay
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import retrofit2.awaitResponse
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    // ⚠️ API KEY
    private val API_KEY_VT = BuildConfig.VT_KEY
    private val MODEL_FILE = "malware_model.tflite"

    // UI
    private lateinit var btnSelect: Button
    private lateinit var btnPdf: Button
    private lateinit var txtStatus: TextView
    private lateinit var txtResult: TextView
    private lateinit var txtProbabilidad: TextView

    private var tfliteInterpreter: Interpreter? = null

    // --- VARIABLES GLOBALES DEL REPORTE ---
    private var currentHash: String = "N/A"
    private var currentFileName: String = "Desconocido" // Aquí guardaremos el nombre real
    private var currentAnalysisDate: String = ""

    // Datos para el PDF
    private var currentVerdict: String = "PENDIENTE"
    private var currentProbability: Float = 0f
    private var staticAnalysisDesc: String = ""
    private var dynamicAnalysisDesc: String = ""
    private var aiDesc: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)

        btnSelect = findViewById(R.id.btnSelectFile)
        btnPdf = findViewById(R.id.btnDownloadPdf)
        txtStatus = findViewById(R.id.txtStatus)
        txtResult = findViewById(R.id.txtResult)
        txtProbabilidad = findViewById(R.id.txtProbabilidad)

        try {
            tfliteInterpreter = Interpreter(loadModelFile())
            txtStatus.text = "Motor UNI-SecureDroid"
        } catch (e: Exception) {
            txtStatus.text = "Error Motor IA: ${e.message}"
            Log.e("UNI-Secure", "Error TFLite", e)
        }

        btnSelect.setOnClickListener { abrirSelectorArchivos() }
        btnPdf.setOnClickListener { generarReportePDF() }
    }

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri -> procesarArchivo(uri) }
        }
    }

    private fun abrirSelectorArchivos() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        filePickerLauncher.launch(intent)
    }

    // --- LÓGICA PRINCIPAL CORREGIDA ---
    private fun procesarArchivo(uri: Uri) {
        btnPdf.visibility = View.GONE
        txtStatus.text = "⏳ Iniciando proceso de análisis ..."
        txtResult.text = ""
        txtProbabilidad.text = ""

        currentFileName = obtenerNombreReal(uri)
        currentAnalysisDate = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val file = uriToFile(uri)

                // 1. ANÁLISIS ESTÁTICO
                reportarEstado("🔍 Ejecutando Análisis Estático ...")
                currentHash = ApkAnalyzer.calcularHash(file)
                val apiMin = ApkAnalyzer.obtenerApiMin(applicationContext, file.absolutePath)
                val apiCount = ApkAnalyzer.estimarComplejidadApi(file)

                staticAnalysisDesc = "Inspección Local: El archivo '$currentFileName' fue procesado.\n" +
                        " - Huella Digital (SHA256) calculada.\n" +
                        " - Nivel de API Mínimo: Android $apiMin\n" +
                        " - Índice de Complejidad de Código: $apiCount puntos."

                // 2. ANÁLISIS DINÁMICO (VIRUSTOTAL)
                reportarEstado("🌍 Ejecutando Análisis Dinámico ...")
                var vtScore = 0
                var azScore = 0
                var conexionExitosa = false

                // Variable para guardar por qué falló la nube (si falla)
                var motivoFalloNube = "No se pudo conectar con el servidor."

                // Límite de 32 MB en Bytes
                val LIMITE_VT = 32 * 1024 * 1024L

                if (currentHash.isNotEmpty()) {
                    try {
                        // A) Consulta por Hash (Siempre se hace, no importa el peso)
                        val response = RetrofitClient.instance.getFileReport(API_KEY_VT, currentHash).awaitResponse()

                        if (response.isSuccessful) {
                            val stats = response.body()?.data?.attributes?.last_analysis_stats
                            vtScore = stats?.malicious ?: 0
                            conexionExitosa = true
                            Log.d("UNI-Secure", "Reporte encontrado. Score: $vtScore")

                        } else if (response.code() == 404) {
                            // B) No existe. Verificamos PESO antes de subir.
                            if (file.length() > LIMITE_VT) {
                                // 🛑 ES MUY GRANDE
                                reportarEstado("⚠️ Archivo > 32MB. Omitiendo subida...")
                                motivoFalloNube = "El archivo excede el límite (32MB) de VirusTotal."
                                // conexionExitosa se queda en false
                            } else {
                                // ✅ TIENE BUEN TAMAÑO -> SUBIMOS
                                reportarEstado("⚠️ Archivo nuevo. Subiendo a Nube...")
                                val requestFile = RequestBody.create("application/vnd.android.package-archive".toMediaTypeOrNull(), file)
                                val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

                                val uploadResponse = RetrofitClient.instance.uploadFile(API_KEY_VT, body).awaitResponse()

                                if (uploadResponse.isSuccessful) {
                                    val analysisId = uploadResponse.body()?.data?.id
                                    if (analysisId != null) {
                                        reportarEstado("⏳ Analizando en nube...")
                                        vtScore = esperarAnalisis(analysisId)
                                        conexionExitosa = true
                                    }
                                } else {
                                    motivoFalloNube = "Error al subir archivo: Código ${uploadResponse.code()}"
                                }
                            }
                        }

                        if (vtScore > 2) azScore = 1

                    } catch (e: Exception) {
                        Log.e("UNI-Secure", "Error VT: ${e.message}")
                        motivoFalloNube = "Error de conexión: ${e.message}"
                    }
                }

                // Generamos la descripción del Análisis Dinámico
                if (conexionExitosa) {
                    dynamicAnalysisDesc = "Consulta VirusTotal Exitosa:\n" +
                            " - Motores Antivirus: $vtScore detecciones positivas.\n" +
                            " - Heurística Avanzada: ${if (azScore == 1) "ALERTA" else "NORMAL"}"
                } else {
                    dynamicAnalysisDesc = "⚠️ Análisis de Nube No Disponible:\n" +
                            " - $motivoFalloNube\n" +
                            " - Se realizó únicamente análisis local."
                }

                // 3. IA (VEREDICTO)
                reportarEstado("🤖 IA Calculando Probabilidades...")

                val inputs = floatArrayOf(apiMin.toFloat(), apiCount.toFloat(), vtScore.toFloat(), vtScore.toFloat(), azScore.toFloat())

                ejecutarIA(inputs)

                aiDesc = "Modelo Random Forest (TFLite):\n" +
                        " - Probabilidad de Malware: ${(currentProbability * 100).toInt()}%\n" +
                        " - Decisión: $currentVerdict"

                // Advertencia extra en la sección de IA si no hubo nube
                if (!conexionExitosa) {
                    aiDesc += "\n\n(Nota: Análisis basado en heurística local por falta de datos en nube)"
                }

                withContext(Dispatchers.Main) {
                    mostrarResultadoFinal()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    txtStatus.text = "Error Crítico: ${e.message}"
                }
            }
        }
    }

    // --- NUEVA FUNCIÓN AUXILIAR PARA OBTENER NOMBRE REAL ---
    private fun obtenerNombreReal(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    // Buscamos la columna de nombre para mostrar
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        result = it.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result ?: "archivo_desconocido.apk"
    }

    private fun ejecutarIA(inputs: FloatArray) {
        if (tfliteInterpreter == null) return

        val inputBuffer = Array(1) { inputs }
        val outputBuffer = Array(1) { FloatArray(1) }

        tfliteInterpreter?.run(inputBuffer, outputBuffer)

        currentProbability = outputBuffer[0][0]
        currentVerdict = if (currentProbability > 0.5) "MALICIOSO" else "BENIGNO"
    }

    private fun mostrarResultadoFinal() {
        txtStatus.text = "Análisis Completado"
        txtResult.text = currentVerdict

        val porcentaje = String.format("%.1f%%", currentProbability * 100)
        txtProbabilidad.text = "Probabilidad de Malware: $porcentaje"

        val colorRes = if (currentVerdict == "MALICIOSO") R.color.red_danger else R.color.green_safe
        txtResult.setTextColor(ContextCompat.getColor(this, colorRes))
        txtProbabilidad.setTextColor(ContextCompat.getColor(this, R.color.uni_purple_dark))

        btnPdf.visibility = View.VISIBLE
        Toast.makeText(this, "Reporte listo para descargar.", Toast.LENGTH_LONG).show()
    }

    // --- GENERADOR DE PDF ---
    private fun generarReportePDF() {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val titlePaint = Paint()
        val subTitlePaint = Paint()
        val bodyPaint = Paint()

        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Estilos
        titlePaint.textSize = 24f
        titlePaint.isFakeBoldText = true
        titlePaint.textAlign = Paint.Align.CENTER
        titlePaint.color = ContextCompat.getColor(this, R.color.uni_purple_dark)

        subTitlePaint.textSize = 16f
        subTitlePaint.isFakeBoldText = true
        subTitlePaint.color = Color.BLACK

        bodyPaint.textSize = 12f
        bodyPaint.color = Color.DKGRAY

        // Cabecera
        canvas.drawText("REPORTE DETALLADO DE ANÁLISIS DEL ARCHIVO", (595/2).toFloat(), 60f, titlePaint)
        canvas.drawText("UNI-SecureDroid", (595/2).toFloat(), 90f, bodyPaint)

        val linePaint = Paint()
        linePaint.color = Color.LTGRAY
        linePaint.strokeWidth = 2f
        canvas.drawLine(50f, 100f, 545f, 100f, linePaint)

        var y = 140f
        val x = 50f

        // 1. Info General
        canvas.drawText("DATOS DEL ANÁLISIS:", x, y, subTitlePaint)
        y += 25f
        canvas.drawText("• Fecha: $currentAnalysisDate", x, y, bodyPaint)
        y += 20f
        // AQUÍ SE MOSTRARÁ EL NOMBRE REAL CORRECTAMENTE
        canvas.drawText("• Archivo: $currentFileName", x, y, bodyPaint)
        y += 20f
        canvas.drawText("• Hash SHA-256: $currentHash", x, y, Paint().apply { textSize=10f })
        y += 40f

        // 2. Análisis Estático
        canvas.drawText("1. ANÁLISIS ESTÁTICO (LOCAL):", x, y, subTitlePaint)
        y += 25f
        for (line in staticAnalysisDesc.split("\n")) {
            canvas.drawText(line, x + 10, y, bodyPaint)
            y += 20f
        }
        y += 20f

        // 3. Análisis Dinámico
        canvas.drawText("2. ANÁLISIS DINÁMICO (NUBE):", x, y, subTitlePaint)
        y += 25f
        for (line in dynamicAnalysisDesc.split("\n")) {
            canvas.drawText(line, x + 10, y, bodyPaint)
            y += 20f
        }
        y += 20f

        // 4. Veredicto IA
        canvas.drawText("3. VEREDICTO INTELIGENCIA ARTIFICIAL:", x, y, subTitlePaint)
        y += 25f
        for (line in aiDesc.split("\n")) {
            canvas.drawText(line, x + 10, y, bodyPaint)
            y += 20f
        }
        y += 40f

        // RESULTADO FINAL
        val verdictPaint = Paint()
        verdictPaint.textSize = 30f
        verdictPaint.isFakeBoldText = true
        verdictPaint.textAlign = Paint.Align.CENTER
        verdictPaint.color = if (currentVerdict == "MALICIOSO") Color.RED else Color.parseColor("#4CAF50")

        val bgPaint = Paint()
        bgPaint.color = Color.LTGRAY
        bgPaint.alpha = 50
        canvas.drawRect(50f, y - 40f, 545f, y + 50f, bgPaint)

        canvas.drawText(currentVerdict, (595/2).toFloat(), y, verdictPaint)
        y += 35f
        canvas.drawText("Probabilidad: ${(currentProbability*100).toInt()}%", (595/2).toFloat(), y, bodyPaint)

        // Footer
        canvas.drawText("Generado por UNI-SecureDroid", (595/2).toFloat(), 800f, Paint().apply {
            textSize = 10f
            textAlign = Paint.Align.CENTER
            color = Color.GRAY
        })

        pdfDocument.finishPage(page)

        // Guardar
        val fileName = "Reporte_${currentFileName}_${System.currentTimeMillis()}.pdf"
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
                if (uri != null) {
                    contentResolver.openOutputStream(uri)?.use { pdfDocument.writeTo(it) }
                    Toast.makeText(this, "PDF Guardado en Descargas", Toast.LENGTH_LONG).show()
                }
            } else {
                val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
                pdfDocument.writeTo(FileOutputStream(file))
                Toast.makeText(this, "PDF Guardado", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e("UNI-Secure", "Error PDF", e)
            Toast.makeText(this, "Error guardando PDF", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }
    }

    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor = assets.openFd(MODEL_FILE)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.declaredLength)
    }

    private suspend fun reportarEstado(mensaje: String) {
        withContext(Dispatchers.Main) { txtStatus.text = mensaje }
    }

    private fun uriToFile(uri: Uri): File {
        val inputStream = contentResolver.openInputStream(uri) ?: throw Exception("Error stream")
        val tempFile = File(cacheDir, "analisis_temp.apk") // Nombre temporal interno (invisible para el usuario)
        if (tempFile.exists()) tempFile.delete()
        val outputStream = FileOutputStream(tempFile)
        inputStream.copyTo(outputStream)
        outputStream.close()
        inputStream.close()
        return tempFile
    }

    // Función que espera a que VirusTotal termine de analizar
    private suspend fun esperarAnalisis(analysisId: String): Int {
        var intentos = 0
        while (intentos < 10) { // Intentamos por 30-40 segundos máximo
            delay(4000) // Esperar 4 segundos entre preguntas
            val response = RetrofitClient.instance.getAnalysisReport(API_KEY_VT, analysisId).awaitResponse()

            if (response.isSuccessful) {
                val status = response.body()?.data?.attributes?.status
                if (status == "completed") {
                    return response.body()?.data?.attributes?.last_analysis_stats?.malicious ?: 0
                }
            }
            intentos++
        }
        return 0 // Si tarda mucho, asumimos 0 para no bloquear la app eternamente
    }

}

