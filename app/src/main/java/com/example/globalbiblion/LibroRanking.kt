package com.example.globalbiblion

//Data class para los elementos que necesitamos de cada libro que se encuetra en el ranking
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