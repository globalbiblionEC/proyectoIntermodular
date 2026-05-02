package com.example.globalbiblion

data class LibroRanking(
    val id: String,
    val titulo: String,
    val autor: String,
    val pdfPath: String,
    val coverPath: String,
    val positivos: Int,
    val negativos: Int,
    val valor: Double
)