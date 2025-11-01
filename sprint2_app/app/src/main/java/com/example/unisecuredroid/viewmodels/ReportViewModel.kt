package com.example.unisecuredroid.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unisecuredroid.data.models.StaticReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ReportViewModel : ViewModel() {

    // --- Data Holder Centralizada para el Reporte (Consumidor) ---
    object TempDataHolder {
        var lastReport: StaticReport? = null
    }

    // Estado de la UI
    sealed class ReportState {
        object Idle : ReportState()
        object Loading : ReportState()
        data class Success(val report: StaticReport) : ReportState()
        data class Error(val message: String) : ReportState()
    }

    private val _reportState = MutableStateFlow<ReportState>(ReportState.Idle)
    val reportState: StateFlow<ReportState> = _reportState

    fun fetchReport(jobId: String) {
        viewModelScope.launch {
            _reportState.value = ReportState.Loading

            val report = TempDataHolder.lastReport

            try {
                // Validación de Datos (Comprueba que el ID coincide y el reporte existe)
                if (report == null || report.jobId != jobId) {
                    throw IllegalStateException("Error interno: Reporte no encontrado para el Job ID $jobId. Posiblemente se perdió el estado.")
                }

                _reportState.value = ReportState.Success(report)

                // Limpiar la referencia para evitar fugas de memoria, pero solo si coincide
                TempDataHolder.lastReport = null

            } catch (e: Exception) {
                _reportState.value = ReportState.Error(e.message ?: "Error al recuperar el reporte.")
            }
        }
    }
}