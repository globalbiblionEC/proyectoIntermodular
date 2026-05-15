package com.example.globalbiblion

data class NotificacionesItem(
    val texto: String,
    val fecha: com.google.firebase.Timestamp?,
    val accion: (() -> Unit)? = null,
    val textoBoton: String=""

)
