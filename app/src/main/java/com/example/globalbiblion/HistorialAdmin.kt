package com.example.globalbiblion

data class HistorialAdmin(
    val id: String,
    val tipo: String, // Certificado o Solicitud
    val titulo: String,
    val usuario: String,
    val estado: String,
    val motivo: String,
    val fecha: String,
    val fechaMillis:Long
)