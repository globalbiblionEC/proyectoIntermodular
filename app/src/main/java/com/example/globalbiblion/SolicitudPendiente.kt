package com.example.globalbiblion

data class SolicitudPendiente(
    val id: String,
    val bookTitle: String,
    val userName: String,
    val requestType: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val createdAt: String,
    val status: String,
    val message: String,
    val fileUrl: String = ""
)