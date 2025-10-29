package com.example.unisecuredroid.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unisecuredroid.data.StaticAnalyzer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class UploadViewModel : ViewModel() {

    // Estado de la UI
    sealed class UploadState {
        object Idle : UploadState()
        object Loading : UploadState()
        data class Success(val jobId: String) : UploadState()
        data class Error(val message: String) : UploadState()
    }

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState

    // Lógica de Subida
    fun uploadFile(context: Context, fileName: String, uri: Uri) {
        viewModelScope.launch {
            _uploadState.value = UploadState.Loading
            try {
                if (!fileName.endsWith(".apk", ignoreCase = true)) {
                    throw Exception("Formato no válido. Solo se aceptan archivos .apk.")
                }

                // Generamos Job ID
                val jobId = "job_" + System.currentTimeMillis().toString()
                println("Job $jobId creado para $fileName. Estado: 'Queued'")
                ReportViewModel.TempDataHolder.lastApkUri = uri
                ReportViewModel.TempDataHolder.lastJobId = jobId

                _uploadState.value = UploadState.Success(jobId)

            } catch (e: Exception) {
                _uploadState.value = UploadState.Error(e.message ?: "Error al procesar archivo")
            }
        }
    }

    fun resetState() {
        _uploadState.value = UploadState.Idle
    }
}