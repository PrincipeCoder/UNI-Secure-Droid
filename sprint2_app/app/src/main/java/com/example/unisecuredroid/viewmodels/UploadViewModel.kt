package com.example.unisecuredroid.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unisecuredroid.data.api.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

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

    object TempDataHolder {
        var lastApkUri: Uri? = null
        var lastJobId: String? = null
    }

    fun setApkUri(uri: Uri?) {
        if (uri == null) {
            _analysisState.value = AnalysisState.Error("URI del archivo no válida.")
            _analysisState.value = AnalysisState.Idle
        } else {
            TempDataHolder.lastApkUri = uri
            _analysisState.value = AnalysisState.FileSelected
        }
    }

    fun startStaticAnalysis(context: Context) {
        if (_analysisState.value == AnalysisState.Analyzing) return

        val apkUri = TempDataHolder.lastApkUri
        if (apkUri == null) {
            _analysisState.value = AnalysisState.Error("No se ha seleccionado un archivo APK.")
            return
        }

        _analysisState.value = AnalysisState.Analyzing

        viewModelScope.launch {
            try {
                val tempFile = File(context.cacheDir, "temp_upload.apk")
                context.contentResolver.openInputStream(apkUri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }

                val requestBody = tempFile.asRequestBody("application/vnd.android.package-archive".toMediaTypeOrNull())
                val filePart = MultipartBody.Part.createFormData("file", tempFile.name, requestBody)

                val response = RetrofitClient.apiService.uploadApk(filePart)
                tempFile.delete()

                if (response.isSuccessful && response.body() != null) {
                    val uploadResponse = response.body()!!
                    val jobId = uploadResponse.job_id
                    TempDataHolder.lastJobId = jobId

                    pollJobStatus(jobId)
                } else {
                    _analysisState.value = AnalysisState.Error("Error al subir archivo: ${response.code()}")
                }

            } catch (e: Exception) {
                _analysisState.value = AnalysisState.Error("Error: ${e.message ?: "Desconocido"}")
            }
        }
    }

    private suspend fun pollJobStatus(jobId: String) {
        var attempts = 0
        val maxAttempts = 60

        while (attempts < maxAttempts) {
            try {
                val statusResponse = RetrofitClient.apiService.getJobStatus(jobId)
                if (statusResponse.isSuccessful && statusResponse.body() != null) {
                    val status = statusResponse.body()!!.status
                    when (status) {
                        "completed" -> {
                            _analysisState.value = AnalysisState.Success(jobId)
                            return
                        }
                        "failed" -> {
                            _analysisState.value = AnalysisState.Error("Análisis falló en el servidor")
                            return
                        }
                    }
                }
                delay(2000)
                attempts++
            } catch (e: Exception) {
                _analysisState.value = AnalysisState.Error("Error al consultar estado: ${e.message}")
                return
            }
        }
        _analysisState.value = AnalysisState.Error("Timeout: análisis tomó demasiado tiempo")
    }

    fun resetState() {
        _analysisState.value = AnalysisState.Idle
        TempDataHolder.lastApkUri = null
        TempDataHolder.lastJobId = null
    }
}