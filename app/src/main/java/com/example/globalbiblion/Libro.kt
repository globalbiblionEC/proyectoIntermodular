package com.example.globalbiblion
//Datos de los libros que tenemos en PDF en la carpeta de assets
data class Libro(
    val idLibro: String,
    val titulo: String,
    val autor: String,
    val nombrePDF: String,
    val paginasTotales: Int,
    val portadaResId: Int
)
