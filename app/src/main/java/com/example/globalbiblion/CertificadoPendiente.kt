package com.example.globalbiblion

data class CertificadoPendiente(
    val uid: String,
    val nombreCompleto: String,
    val email: String,
    val rol: String,
    val estado: String,
    val certificateUrl: String,
    val roleCertificatePath: String,
    val emisor: String,
    val idioma: String,
    val nivel: String,
    val institucionValida: Boolean,
    val idiomaValido: Boolean,
    val nivelValido: Boolean,
    val fechaValida: Boolean,
    val codigoPresente: Boolean,
    val mensaje: String
)