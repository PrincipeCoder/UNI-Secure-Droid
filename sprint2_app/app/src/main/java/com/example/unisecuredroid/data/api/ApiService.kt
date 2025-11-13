package com.example.unisecuredroid.data.api

import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

data class UploadResponse(
    val job_id: String,
    val hash: String,
    val status: String,
    val message: String
)

data class StatusResponse(
    val job_id: String,
    val status: String,
    val progress: Int
)

data class ReportResponse(
    val job_id: String,
    val sha256: String,
    val verdict: String,
    val risk: String,
    val family: String,
    val ai_probability: Float,
    val static_analysis: StaticAnalysisData,
    val dynamic_features: DynamicFeaturesData,
    val timestamp: String,
    val analysis_time_seconds: Int
)

data class StaticAnalysisData(
    val permissions: List<String>,
    val urls: List<String>,
    val apis_detected: List<String>,
    val top_signals: List<String>
)

data class DynamicFeaturesData(
    val network: NetworkData,
    val file_operations: List<FileOperation>,
    val syscalls: List<String>
)

data class NetworkData(
    val connections: List<String>,
    val dns_queries: List<String>,
    val data_sent_kb: Float
)

data class FileOperation(
    val action: String,
    val path: String
)

interface ApiService {
    @Multipart
    @POST("api/upload")
    suspend fun uploadApk(
        @Part file: MultipartBody.Part
    ): Response<UploadResponse>

    @GET("api/status/{job_id}")
    suspend fun getJobStatus(
        @Path("job_id") jobId: String
    ): Response<StatusResponse>

    @GET("api/report/{job_id}")
    suspend fun getReport(
        @Path("job_id") jobId: String
    ): Response<ReportResponse>
}

object RetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:8000/"
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
