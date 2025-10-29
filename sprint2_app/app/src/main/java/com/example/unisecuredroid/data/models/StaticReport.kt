package com.example.unisecuredroid.data.models

data class StaticReport(
    val jobId: String,
    val verdict: String,
    val permissions: List<String>,
    val urls: List<String>,
    val verdictDetails: String
)