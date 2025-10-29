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

class ReportViewModel : ViewModel() {

    // Estado de la UI
    sealed class ReportState {
        object Idle : ReportState()
        object Loading : ReportState()
        data class Success(val report: StaticReport) : ReportState()
        data class Error(val message: String) : ReportState()
    }

    private val _reportState = MutableStateFlow<ReportState>(ReportState.Idle)
    val reportState: StateFlow<ReportState> = _reportState

    object TempDataHolder {
        var lastApkUri: Uri? = null
        var lastJobId: String? = null
    }

    fun fetchReport(context: Context, jobId: String) {
        viewModelScope.launch {
            _reportState.value = ReportState.Loading
            try {
                // Leemos el Uri guardado temporalmente
                val apkUri = TempDataHolder.lastApkUri
                // Validamos que el Uri exista y corresponda al JobId
                if (apkUri == null || TempDataHolder.lastJobId != jobId) {
                    throw Exception("Error interno: No se encontró el archivo a analizar.")
                }

                // LLAMADA AL StaticAnalyzer
                val report = StaticAnalyzer.performStaticAnalysis(context, apkUri, jobId)

                _reportState.value = ReportState.Success(report)
            } catch (e: Exception) {
                // Manejo de errores (del análisis o del TempDataHolder)
                _reportState.value = ReportState.Error(e.message ?: "Error desconocido durante el análisis")
            }
        }
    }
}