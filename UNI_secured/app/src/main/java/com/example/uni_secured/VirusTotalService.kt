package com.example.uni_secured

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

// --- MODELOS DE DATOS ---
data class VtResponse(val data: VtData)
data class VtData(val id: String, val type: String, val attributes: VtAttributes)
data class VtAttributes(val last_analysis_stats: VtStats?, val status: String?)
data class VtStats(val malicious: Int, val suspicious: Int)

// --- INTERFAZ API ---
interface VirusTotalApi {
    // 1. Consultar por Hash (Rápido)
    @GET("files/{file_hash}")
    fun getFileReport(
        @Header("x-apikey") apiKey: String,
        @Path("file_hash") fileHash: String
    ): Call<VtResponse>

    // 2. Subir Archivo Nuevo (Lento - Solo si no existe)
    @Multipart
    @POST("files")
    fun uploadFile(
        @Header("x-apikey") apiKey: String,
        @Part file: MultipartBody.Part
    ): Call<VtResponse>

    // 3. Consultar Estado del Análisis (Para ver si ya terminó de escanearse)
    @GET("analyses/{analysis_id}")
    fun getAnalysisReport(
        @Header("x-apikey") apiKey: String,
        @Path("analysis_id") analysisId: String
    ): Call<VtResponse>
}

// --- CLIENTE ---
object RetrofitClient {
    private const val BASE_URL = "https://www.virustotal.com/api/v3/"

    val instance: VirusTotalApi by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(VirusTotalApi::class.java)
    }
}