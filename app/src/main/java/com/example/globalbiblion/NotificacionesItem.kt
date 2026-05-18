package com.example.globalbiblion
//Data class para los elementos que debe tener cada item del panel de notificaciones
data class NotificacionesItem(
    val texto: String,
    val fecha: com.google.firebase.Timestamp?,
    val accion: (() -> Unit)? = null,
    val textoBoton: String="",
    val esNueva: Boolean = false
)
