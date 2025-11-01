package com.example.unisecuredroid.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unisecuredroid.data.StaticAnalyzer
import com.example.unisecuredroid.data.models.StaticReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class UploadViewModel : ViewModel() {

    // 1. Estados del Proceso de Análisis
    sealed class AnalysisState {
        object Idle : AnalysisState()
        object FileSelected : AnalysisState()
        object Analyzing : AnalysisState()
        data class Success(val jobId: String) : AnalysisState()
        data class Error(val message: String) : AnalysisState()
    }

    private val _analysisState = MutableStateFlow<AnalysisState>(AnalysisState.Idle)
    val analysisState: StateFlow<AnalysisState> = _analysisState

    // 2. Data temporal compartida (El Reporte completo se almacena aquí)
    object TempDataHolder {
        var lastApkUri: Uri? = null // Guardamos el URI seleccionado
        var lastJobId: String? = null
    }

    // 3. Función para recibir el APK
    fun setApkUri(uri: Uri?) {
        if (uri == null) {
            _analysisState.value = AnalysisState.Error("URI del archivo no válida.")
            // Resetear el estado a Idle si la URI es nula
            _analysisState.value = AnalysisState.Idle
        } else {
            TempDataHolder.lastApkUri = uri
            _analysisState.value = AnalysisState.FileSelected
            // Limpiamos errores anteriores si el archivo es válido

        }
    }

    // 4. Función para iniciar el Análisis Estático y la Inferencia de IA
    fun startStaticAnalysis(context: Context) {
        if (_analysisState.value == AnalysisState.Analyzing) return // Evitar doble ejecución

        val apkUri = TempDataHolder.lastApkUri
        if (apkUri == null) {
            _analysisState.value = AnalysisState.Error("No se ha seleccionado un archivo APK.")
            return
        }

        // 5. Cambio de estado a Análisis
        _analysisState.value = AnalysisState.Analyzing

        viewModelScope.launch {
            val jobId = UUID.randomUUID().toString()
            TempDataHolder.lastJobId = jobId

            try {
                // LLAMADA CLAVE AL StaticAnalyzer
                val report = StaticAnalyzer.performStaticAnalysis(context, apkUri, jobId)

                // Guardar el reporte completo para que ReportViewModel lo pueda recuperar
                ReportViewModel.TempDataHolder.lastReport = report

                _analysisState.value = AnalysisState.Success(jobId)

            } catch (e: Exception) {
                _analysisState.value = AnalysisState.Error("Error en el análisis estático: ${e.message ?: "Desconocido"}")
                // Volver a FileSelected para permitir reintentar
                _analysisState.value = AnalysisState.FileSelected
            }
        }
    }

    // Función para limpiar el estado de navegación
    fun resetState() {
        _analysisState.value = AnalysisState.Idle
        TempDataHolder.lastApkUri = null
        TempDataHolder.lastJobId = null
    }
}