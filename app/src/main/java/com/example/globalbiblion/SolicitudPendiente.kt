package com.example.globalbiblion

data class SolicitudPendiente(
    val id: String,
    val bookTitle: String,

    val translatorName: String,
    val proofreaderName: String,

    val requestType: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val createdAt: String,
    val status: String,
    val message: String,

    val translationUrl: String = "",
    val correctionUrl: String = "",
    val reviewNotes: String = ""
)