package com.example.unisecuredroid.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unisecuredroid.data.api.RetrofitClient
import com.example.unisecuredroid.data.models.AnalysisReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ReportViewModel : ViewModel() {

    sealed class ReportState {
        object Idle : ReportState()
        object Loading : ReportState()
        data class Success(val report: AnalysisReport) : ReportState()
        data class Error(val message: String) : ReportState()
    }

    private val _reportState = MutableStateFlow<ReportState>(ReportState.Idle)
    val reportState: StateFlow<ReportState> = _reportState

    fun fetchReport(jobId: String) {
        viewModelScope.launch {
            _reportState.value = ReportState.Loading

            try {
                val response = RetrofitClient.apiService.getReport(jobId)
                
                if (response.isSuccessful && response.body() != null) {
                    val reportResponse = response.body()!!
                    
                    val report = AnalysisReport(
                        jobId = reportResponse.job_id,
                        sha256 = reportResponse.sha256,
                        verdict = reportResponse.verdict,
                        risk = reportResponse.risk,
                        family = reportResponse.family,
                        aiProbability = reportResponse.ai_probability,
                        staticAnalysis = com.example.unisecuredroid.data.models.StaticAnalysisData(
                            permissions = reportResponse.static_analysis.permissions,
                            urls = reportResponse.static_analysis.urls,
                            apisDetected = reportResponse.static_analysis.apis_detected,
                            topSignals = reportResponse.static_analysis.top_signals
                        ),
                        dynamicFeatures = com.example.unisecuredroid.data.models.DynamicFeaturesData(
                            network = com.example.unisecuredroid.data.models.NetworkData(
                                connections = reportResponse.dynamic_features.network.connections,
                                dnsQueries = reportResponse.dynamic_features.network.dns_queries,
                                dataSentKb = reportResponse.dynamic_features.network.data_sent_kb
                            ),
                            fileOperations = reportResponse.dynamic_features.file_operations.map {
                                com.example.unisecuredroid.data.models.FileOperation(it.action, it.path)
                            },
                            syscalls = reportResponse.dynamic_features.syscalls
                        ),
                        timestamp = reportResponse.timestamp,
                        analysisTimeSeconds = reportResponse.analysis_time_seconds
                    )
                    
                    _reportState.value = ReportState.Success(report)
                } else {
                    _reportState.value = ReportState.Error("Error al obtener reporte: ${response.code()}")
                }

            } catch (e: Exception) {
                _reportState.value = ReportState.Error(e.message ?: "Error al recuperar el reporte.")
            }
        }
    }
}