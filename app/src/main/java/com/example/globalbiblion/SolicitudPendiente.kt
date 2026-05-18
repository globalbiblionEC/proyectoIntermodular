package com.example.globalbiblion

//Data Class para definir los elementos de cada registro de solicitud pendiente
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