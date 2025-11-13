package com.example.unisecuredroid.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AnalysisReport(
    val jobId: String,
    val sha256: String,
    val verdict: String,
    val risk: String,
    val family: String,
    val aiProbability: Float,
    val staticAnalysis: StaticAnalysisData,
    val dynamicFeatures: DynamicFeaturesData,
    val timestamp: String,
    val analysisTimeSeconds: Int
) : Parcelable

@Parcelize
data class StaticAnalysisData(
    val permissions: List<String>,
    val urls: List<String>,
    val apisDetected: List<String>,
    val topSignals: List<String>
) : Parcelable

@Parcelize
data class DynamicFeaturesData(
    val network: NetworkData,
    val fileOperations: List<FileOperation>,
    val syscalls: List<String>
) : Parcelable

@Parcelize
data class NetworkData(
    val connections: List<String>,
    val dnsQueries: List<String>,
    val dataSentKb: Float
) : Parcelable

@Parcelize
data class FileOperation(
    val action: String,
    val path: String
) : Parcelable
