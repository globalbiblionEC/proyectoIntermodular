package com.example.globalbiblion

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale
import android.net.Uri
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.firestore.FieldValue
import com.google.firebase.storage.FirebaseStorage
import com.bumptech.glide.Glide
class Notificaciones : Bars() {


    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var tvNotificaciones: TextView
    private lateinit var btnVolver: ImageButton
    private lateinit var tvNombreUsuario: TextView
    private lateinit var ivPerfil: ImageView
    private lateinit var btnSubirCertificadoNuevo: Button
    private lateinit var storage: FirebaseStorage
    private var uriCertificadoNuevo: Uri? = null
    private lateinit var llAvisosCorrector: LinearLayout
    private var requestIdCorreccionActual: String = ""
    private var reviewNotesActual: String = ""

    private val seleccionarNuevoCertificado =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                uriCertificadoNuevo = uri
                subirNuevoCertificado(uri)
            } else {
                Toast.makeText(this, "No seleccionaste ningún PDF", Toast.LENGTH_SHORT).show()
            }
        }

    private val seleccionarPdfCorreccion =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                subirPdfCorreccion(uri)
            } else {
                Toast.makeText(this, "No seleccionaste ningún PDF", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notificaciones)

        configurarBottomBar()

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        tvNotificaciones = findViewById(R.id.tvNotificaciones)
        btnVolver = findViewById(R.id.btnVolver)
        tvNombreUsuario = findViewById(R.id.tvNombreUsuario)
        ivPerfil = findViewById(R.id.ivPerfil)
        btnSubirCertificadoNuevo = findViewById(R.id.btnSubirCertificadoNuevo)
        llAvisosCorrector = findViewById(R.id.llAvisosCorrector)

        btnSubirCertificadoNuevo.visibility = View.GONE

        btnVolver.setOnClickListener {
            finish()
        }

        ivPerfil.setOnClickListener {
            startActivity(Intent(this, PerfilUsuario::class.java))
        }

        btnSubirCertificadoNuevo.setOnClickListener {
            seleccionarNuevoCertificado.launch("application/pdf")
        }

        cargarNombreUsuario()
        cargarNotificaciones()
        //marcarNotificacionesComoLeidas()

    }


    private fun cargarNotificaciones() {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            Toast.makeText(this, "Debes iniciar sesión", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        tvNotificaciones.text = "Cargando notificaciones..."
        llAvisosCorrector.removeAllViews()

        val listaNotificaciones = mutableListOf<NotificacionesItem>()

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->

                val ultimaLectura = doc.getTimestamp("notificationsLastReadAt")
                val estado = doc.getString("roleVerificationStatus") ?: ""
                val mensaje = doc.getString("notificationMessage") ?: ""
                val fechaRevision = doc.getTimestamp("adminReviewedAt")
                val fechaCertificado = doc.getTimestamp("certificateUpdatedAt") ?: fechaRevision

                when (estado) {
                    "pending_review", "prevalidated" -> {
                        btnSubirCertificadoNuevo.visibility = View.GONE

                        listaNotificaciones.add(
                            NotificacionesItem(
                                texto = "⏳ Tu certificado está pendiente de verificación.\nCuando el administrador lo revise, recibirás una notificación.",
                                fecha = fechaCertificado,
                                esNueva = esNotificacionNueva(fechaCertificado, ultimaLectura)
                            )
                        )
                    }

                    "verified" -> {
                        btnSubirCertificadoNuevo.visibility = View.GONE

                        listaNotificaciones.add(
                            NotificacionesItem(
                                texto = "✅ Tu certificado ha sido validado.\nFecha: ${formatearFecha(fechaRevision)}",
                                fecha = fechaRevision,
                                esNueva = esNotificacionNueva(fechaRevision, ultimaLectura)
                            )
                        )
                    }

                    "rejected" -> {
                        btnSubirCertificadoNuevo.visibility = View.VISIBLE

                        val motivo = doc.getString("reviewNotes") ?: "Sin motivo indicado"

                        listaNotificaciones.add(
                            NotificacionesItem(
                                texto = "❌ Tu certificado ha sido rechazado.\nFecha: ${formatearFecha(fechaRevision)}\nMotivo: $motivo",
                                fecha = fechaRevision,
                                esNueva = esNotificacionNueva(fechaRevision, ultimaLectura)
                            )
                        )
                    }
                }

                if (mensaje.isNotEmpty()) {
                    val fechaMensaje = doc.getTimestamp("lastNotificationAt")
                        ?: fechaRevision
                        ?: fechaCertificado

                    listaNotificaciones.add(
                        NotificacionesItem(
                            texto = "Aviso: $mensaje",
                            fecha = fechaMensaje,
                            esNueva = esNotificacionNueva(fechaMensaje, ultimaLectura)
                        )
                    )
                }

                cargarNotificacionesSolicitudes(uid, listaNotificaciones, ultimaLectura)
            }
            .addOnFailureListener {
                tvNotificaciones.text = "Error al cargar notificaciones"
            }
    }

    private fun cargarNotificacionesSolicitudes(
        uid: String,
        listaNotificaciones: MutableList<NotificacionesItem>,
        ultimaLectura: com.google.firebase.Timestamp?
    ) {
        db.collection("contribution_requests")
            .whereEqualTo("translatorId", uid)
            .whereIn(
                "status",
                listOf(
                    "waiting_for_proofreader",
                    "proofreader_approved",
                    "proofreader_rejected",
                    "changes_requested",
                    "published",
                    "translation_vacancy_open"
                )
            )
            .get()
            .addOnSuccessListener { documentos ->

                for (doc in documentos) {
                    val titulo = doc.getString("bookTitle") ?: "Libro"
                    val status = doc.getString("status") ?: ""
                    val corrector = doc.getString("proofreaderName") ?: "Corrector"
                    val notasCorrector = doc.getString("reviewNotes") ?: ""
                    val notasAdmin = doc.getString("adminNotes") ?: ""

                    val fecha = doc.getTimestamp("adminReviewedAt")
                        ?: doc.getTimestamp("proofreadAt")
                        ?: doc.getTimestamp("uploadedAt")
                        ?: doc.getTimestamp("createdAt")

                    when (status) {
                        "proofreader_approved" -> {
                            listaNotificaciones.add(
                                NotificacionesItem(
                                    texto = "✅ Tu traducción de '$titulo' ha sido verificada por $corrector.\nAhora está pendiente de revisión del administrador.\nFecha: ${formatearFecha(fecha)}",
                                    fecha = fecha,
                                    esNueva = esNotificacionNueva(fecha, ultimaLectura)
                                )
                            )
                        }

                        "proofreader_rejected" -> {
                            listaNotificaciones.add(
                                NotificacionesItem(
                                    texto = "❌ Tu traducción de '$titulo' ha sido rechazada por $corrector.\nMotivo: ${notasCorrector.ifBlank { "Sin motivo indicado" }}\nAhora pasará al administrador.\nFecha: ${formatearFecha(fecha)}",
                                    fecha = fecha,
                                    esNueva = esNotificacionNueva(fecha, ultimaLectura)
                                )
                            )
                        }

                        "published" -> {
                            listaNotificaciones.add(
                                NotificacionesItem(
                                    texto = "📚 ¡Tu traducción de '$titulo' ha sido publicada!\nYa está disponible para los usuarios.\nFecha: ${formatearFecha(fecha)}",
                                    fecha = fecha,
                                    esNueva = esNotificacionNueva(fecha, ultimaLectura)
                                )
                            )
                        }

                        "translation_vacancy_open" -> {
                            listaNotificaciones.add(
                                NotificacionesItem(
                                    texto = "⚠️ Tu traducción de '$titulo' no ha sido publicada.\nLa vacante se ha reabierto para otro traductor.\nMotivo: ${notasAdmin.ifBlank { "Sin motivo indicado" }}\nFecha: ${formatearFecha(fecha)}",
                                    fecha = fecha,
                                    esNueva = esNotificacionNueva(fecha, ultimaLectura)
                                )
                            )
                        }

                        "waiting_for_proofreader" -> {
                            listaNotificaciones.add(
                                NotificacionesItem(
                                    texto = "⏳ Tu traducción de '$titulo' está pendiente de verificación por un corrector.\nIdioma destino: ${doc.getString("targetLanguage") ?: ""}\nFecha: ${formatearFecha(fecha)}",
                                    fecha = fecha,
                                    esNueva = esNotificacionNueva(fecha, ultimaLectura)
                                )
                            )
                        }

                        "changes_requested" -> {
                            val correctionUrl = doc.getString("correctionUrl") ?: ""
                            val correctionPath = doc.getString("correctionPath") ?: ""
                            val bookId = doc.getString("bookId") ?: ""
                            val sourceLanguage = doc.getString("sourceLanguage") ?: ""
                            val targetLanguage = doc.getString("targetLanguage") ?: ""
                            val requestId = doc.id

                            val motivo = notasCorrector.ifBlank {
                                notasAdmin.ifBlank { "Sin motivo indicado" }
                            }

                            listaNotificaciones.add(
                                NotificacionesItem(
                                    texto = "🔁 $corrector ha pedido cambios en tu traducción de '$titulo'." +
                                            "\nMotivo: $motivo\n" +
                                            "Fecha: ${formatearFecha(fecha)}",
                                    fecha = fecha,
                                    esNueva = esNotificacionNueva(fecha, ultimaLectura),
                                    textoBoton = "Ver corrección y subir nueva traducción",
                                    accion = {
                                        mostrarDetalleCambiosTraductor(
                                            requestId = requestId,
                                            bookId = bookId,
                                            bookTitle = titulo,
                                            sourceLanguage = sourceLanguage,
                                            targetLanguage = targetLanguage,
                                            corrector = corrector,
                                            motivo = motivo,
                                            correctionUrl = correctionUrl,
                                            correctionPath = correctionPath
                                        )
                                    }
                                )
                            )
                        }
                    }
                }

                cargarNotificacionesCorrector(uid, listaNotificaciones, ultimaLectura)
            }
            .addOnFailureListener { e ->
                tvNotificaciones.text = "Error cargando notificaciones del traductor: ${e.message}"
            }
    }

    private fun cargarNotificacionesCorrector(
        uid: String,
        listaNotificaciones: MutableList<NotificacionesItem>,
        ultimaLectura: com.google.firebase.Timestamp?
    ) {
        db.collection("contribution_requests")
            .whereEqualTo("proofreaderId", uid)
            .whereIn(
                "status",
                listOf(
                    "proofreader_approved",
                    "proofreader_rejected",
                    "published",
                    "changes_requested",
                    "translation_vacancy_open"
                )
            )
            .get()
            .addOnSuccessListener { documentos ->

                for (doc in documentos) {
                    val titulo = doc.getString("bookTitle") ?: "Libro"
                    val status = doc.getString("status") ?: ""
                    val traductor = doc.getString("translatorName") ?: "Traductor"
                    val notasAdmin = doc.getString("adminNotes") ?: ""
                    val notasCorrector = doc.getString("reviewNotes") ?: ""

                    val fecha = doc.getTimestamp("adminReviewedAt")
                        ?: doc.getTimestamp("proofreadAt")
                        ?: doc.getTimestamp("uploadedAt")
                        ?: doc.getTimestamp("createdAt")

                    val texto = when (status) {
                        "proofreader_approved" ->
                            "✅ Has verificado la traducción de '$titulo'.\nTraductor: $traductor\nAhora está pendiente del administrador.\nFecha: ${formatearFecha(fecha)}"

                        "proofreader_rejected" ->
                            "❌ Has rechazado la traducción de '$titulo'.\nTraductor: $traductor\nMotivo: ${notasCorrector.ifBlank { "Sin motivo indicado" }}\nAhora está pendiente del administrador.\nFecha: ${formatearFecha(fecha)}"

                        "published" ->
                            "📚 El administrador ha publicado la traducción de '$titulo'.\nTu corrección fue aceptada.\nFecha: ${formatearFecha(fecha)}"

                        "changes_requested" ->
                            "🔁 El administrador ha aceptado tu corrección de '$titulo'.\nLa traducción volverá al traductor para que suba una nueva versión.\nFecha: ${formatearFecha(fecha)}"

                        "translation_vacancy_open" ->
                            "⚠️ El administrador ha rechazado la traducción de '$titulo'.\nLa vacante se ha reabierto.\nNotas: ${notasAdmin.ifBlank { "Sin notas" }}\nFecha: ${formatearFecha(fecha)}"

                        else -> ""
                    }

                    if (texto.isNotBlank()) {
                        listaNotificaciones.add(
                            NotificacionesItem(
                                texto = texto,
                                fecha = fecha,
                                esNueva = esNotificacionNueva(fecha, ultimaLectura)
                            )
                        )
                    }
                }

                cargarTraduccionesPendientesCorrector(uid, listaNotificaciones, ultimaLectura)
            }
            .addOnFailureListener { e ->
                tvNotificaciones.text = "Error cargando notificaciones del corrector: ${e.message}"
            }
    }

    private fun formatearFecha(timestamp: com.google.firebase.Timestamp?): String {

        if (timestamp == null) {
            return "Fecha desconocida"
        }

        val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        return formato.format(timestamp.toDate())
    }

    private fun marcarNotificacionesComoLeidas() {

        val uid = auth.currentUser?.uid ?: return

        db.collection("users")
            .document(uid)
            .update(
                mapOf(
                    "notificationPending" to false,
                    "notificationsLastReadAt" to FieldValue.serverTimestamp()
                )
            )
    }

    private fun cargarNombreUsuario() {

        val uid = auth.currentUser?.uid ?: return

        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->

                val nombre = doc.getString("nombre") ?: ""
                val apellidos = doc.getString("apellidos") ?: ""

                val profileImageUrl =
                    doc.getString("profileImageUrl") ?: ""

                val profileImagePath =
                    doc.getString("profileImagePath") ?: ""

                tvNombreUsuario.text = when {

                    nombre.isNotEmpty() && apellidos.isNotEmpty() ->
                        "$nombre $apellidos"

                    nombre.isNotEmpty() ->
                        nombre

                    else ->
                        "Usuario"
                }

                // FOTO PERFIL
                when {

                    profileImageUrl.isNotBlank() -> {

                        Glide.with(this)
                            .load(profileImageUrl)
                            .placeholder(R.drawable.usuarioleyendocfmenuprinc)
                            .error(R.drawable.usuarioleyendocfmenuprinc)
                            .circleCrop()
                            .into(ivPerfil)
                    }

                    profileImagePath.isNotBlank() -> {

                        storage.reference.child(profileImagePath)
                            .downloadUrl
                            .addOnSuccessListener { uri ->

                                Glide.with(this)
                                    .load(uri.toString())
                                    .placeholder(R.drawable.usuarioleyendocfmenuprinc)
                                    .error(R.drawable.usuarioleyendocfmenuprinc)
                                    .circleCrop()
                                    .into(ivPerfil)
                            }
                            .addOnFailureListener {

                                ivPerfil.setImageResource(
                                    R.drawable.usuarioleyendocfmenuprinc
                                )
                            }
                    }

                    else -> {

                        ivPerfil.setImageResource(
                            R.drawable.usuarioleyendocfmenuprinc
                        )
                    }
                }
            }
            .addOnFailureListener {

                tvNombreUsuario.text = "Usuario"

                ivPerfil.setImageResource(
                    R.drawable.usuarioleyendocfmenuprinc
                )
            }
    }

    /*private fun cargarNombreUsuario() {

        val uid = auth.currentUser?.uid ?: return

        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->

                val nombre = doc.getString("nombre") ?: ""
                val apellidos = doc.getString("apellidos") ?: ""

                val profileImageUrl =
                    doc.getString("profileImageUrl") ?: ""

                tvNombreUsuario.text = when {

                    nombre.isNotEmpty() && apellidos.isNotEmpty() ->
                        "$nombre $apellidos"

                    nombre.isNotEmpty() ->
                        nombre

                    else ->
                        "Usuario"
                }

                // FOTO PERFIL
                if (profileImageUrl.isNotEmpty()) {

                    Glide.with(this)
                        .load(profileImageUrl)
                        .placeholder(R.drawable.usuarioleyendocfmenuprinc)
                        .error(R.drawable.usuarioleyendocfmenuprinc)
                        .circleCrop()
                        .into(ivPerfil)

                } else {

                    ivPerfil.setImageResource(
                        R.drawable.usuarioleyendocfmenuprinc
                    )
                }
            }
            .addOnFailureListener {

                tvNombreUsuario.text = "Usuario"

                ivPerfil.setImageResource(
                    R.drawable.usuarioleyendocfmenuprinc
                )
            }
    }*/
    /*private fun cargarNombreUsuario() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val nombre = doc.getString("nombre") ?: ""
                val apellidos = doc.getString("apellidos") ?: ""

                tvNombreUsuario.text = when {
                    nombre.isNotEmpty() && apellidos.isNotEmpty() -> "$nombre $apellidos"
                    nombre.isNotEmpty() -> nombre
                    else -> "Usuario"
                }
            }
            .addOnFailureListener {
                tvNombreUsuario.text = "Usuario"
            }
    }*/

    private fun subirNuevoCertificado(uri: Uri) {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            Toast.makeText(this, "No se pudo identificar al usuario", Toast.LENGTH_SHORT).show()
            return
        }

        val certificatePath = "users/$uid/certificates/certificado_nuevo_${System.currentTimeMillis()}.pdf"
        val ref = storage.reference.child(certificatePath)

        Toast.makeText(this, "Subiendo certificado...", Toast.LENGTH_SHORT).show()

        ref.putFile(uri)
            .addOnSuccessListener {
                ref.downloadUrl
                    .addOnSuccessListener { url ->

                        db.collection("users").document(uid)
                            .update(
                                mapOf(
                                    "certificateUrl" to url.toString(),
                                    "roleCertificatePath" to certificatePath,
                                    "roleVerificationStatus" to "pending_review",
                                    "reviewNotes" to "",
                                    "notificationPending" to true,
                                    "notificationMessage" to "Tu nuevo certificado se ha subido y queda pendiente de revisión.",
                                    "certificateUpdatedAt" to FieldValue.serverTimestamp()
                                )
                            )
                            .addOnSuccessListener {
                                Toast.makeText(
                                    this,
                                    "Certificado subido. Queda pendiente de revisión.",
                                    Toast.LENGTH_LONG
                                ).show()

                                btnSubirCertificadoNuevo.visibility = View.GONE
                                cargarNotificaciones()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    this,
                                    "Error al actualizar usuario: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Error al subir certificado: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun cargarAvisosCorrector(uid: String) {
        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { userDoc ->

                val rol = userDoc.getString("rol") ?: ""
                val estado = userDoc.getString("roleVerificationStatus") ?: ""

                val idiomaCertificado =
                    userDoc.getString("nativeLanguage") ?: ""

                if (rol != "proofreader" || estado != "verified") {
                    return@addOnSuccessListener
                }

                db.collection("contribution_requests")
                    .whereEqualTo("status", "waiting_for_proofreader")
                    .whereEqualTo("targetLanguage", idiomaCertificado)
                    .get()
                    .addOnSuccessListener { documentos ->

                        llAvisosCorrector.removeAllViews()

                        if (documentos.isEmpty) {
                            val tv = TextView(this)
                            tv.text = "No tienes traducciones pendientes de corregir."
                            tv.textSize = 16f
                            llAvisosCorrector.addView(tv)
                            return@addOnSuccessListener
                        }

                        for (doc in documentos) {
                            crearCardAvisoCorrector(
                                requestId = doc.id,
                                bookTitle = doc.getString("bookTitle") ?: "Libro",
                                translatorName = doc.getString("translatorName") ?: "Traductor",
                                sourceLanguage = doc.getString("sourceLanguage") ?: "",
                                targetLanguage = doc.getString("targetLanguage") ?: "",
                                translationUrl = doc.getString("translationUrl") ?: "",
                                uidCorrector = uid
                            )
                        }
                    }
            }
    }

    private fun crearCardAvisoCorrector(
        requestId: String,
        bookTitle: String,
        translatorName: String,
        sourceLanguage: String,
        targetLanguage: String,
        translationUrl: String,
        uidCorrector: String
    ) {
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.setPadding(20, 20, 20, 20)
        card.setBackgroundColor(android.graphics.Color.WHITE)

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 0, 0, 20)
        card.layoutParams = params

        val tvInfo = TextView(this)
        tvInfo.text =
            "📘 Nueva traducción pendiente\n\n" +
                    "Libro: $bookTitle\n" +
                    "Traductor: $translatorName\n" +
                    "Idioma origen: $sourceLanguage\n" +
                    "Idioma destino: $targetLanguage"

        tvInfo.textSize = 15f
        tvInfo.setTextColor(android.graphics.Color.BLACK)

        val btnVerPdf = Button(this)
        btnVerPdf.text = "Ver PDF traducido"
        btnVerPdf.setOnClickListener {
            abrirPdf(translationUrl)
        }

        val btnAceptar = Button(this)
        btnAceptar.text = "Validar traducción"
        btnAceptar.setOnClickListener {
            aceptarCorreccion(requestId, uidCorrector)
        }

        val btnRechazar = Button(this)
        btnRechazar.text = "Rechazar y subir corrección"
        btnRechazar.setOnClickListener {
            pedirMotivoYSubirCorreccion(requestId)
        }

        card.addView(tvInfo)
        card.addView(btnVerPdf)
        card.addView(btnAceptar)
        card.addView(btnRechazar)

        llAvisosCorrector.addView(card)
    }

    private fun aceptarCorreccion(requestId: String, uidCorrector: String) {
        db.collection("users")
            .document(uidCorrector)
            .get()
            .addOnSuccessListener { userDoc ->

                val nombre = userDoc.getString("nombre") ?: "Corrector"
                val apellidos = userDoc.getString("apellidos") ?: ""
                val nombreCompleto = "$nombre $apellidos".trim()

                db.collection("contribution_requests")
                    .document(requestId)
                    .update(
                        mapOf(
                            "status" to "proofreader_approved",
                            "proofreaderId" to uidCorrector,
                            "proofreaderName" to nombreCompleto,
                            "proofreadAt" to FieldValue.serverTimestamp()
                        )
                    )
                    .addOnSuccessListener {
                        Toast.makeText(
                            this,
                            "Traducción validada. Pasará al panel del administrador.",
                            Toast.LENGTH_LONG
                        ).show()

                        val uid = auth.currentUser?.uid ?: return@addOnSuccessListener
                        cargarAvisosCorrector(uid)
                        cargarNotificaciones()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            this,
                            "Error al validar: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
    }

    private fun pedirMotivoYSubirCorreccion(requestId: String) {
        val input = EditText(this)
        input.hint = "Motivo del rechazo opcional"
        input.minLines = 3

        android.app.AlertDialog.Builder(this)
            .setTitle("Rechazar traducción")
            .setMessage("Puedes escribir un motivo, subir un PDF corregido, o ambas cosas.")
            .setView(input)
            .setPositiveButton("Subir PDF corregido") { _, _ ->
                requestIdCorreccionActual = requestId
                reviewNotesActual = input.text.toString().trim()
                seleccionarPdfCorreccion.launch("application/pdf")
            }
            .setNegativeButton("Rechazar sin PDF") { _, _ ->
                val motivo = input.text.toString().trim()

                if (motivo.isEmpty()) {
                    Toast.makeText(
                        this,
                        "Debes escribir un motivo si no subes PDF",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    rechazarSinPdf(requestId, motivo)
                }
            }
            .setNeutralButton("Cancelar", null)
            .show()
    }

    private fun subirPdfCorreccion(uri: Uri) {
        val uidCorrector = auth.currentUser?.uid

        if (uidCorrector == null) {
            Toast.makeText(this, "Debes iniciar sesión", Toast.LENGTH_SHORT).show()
            return
        }

        if (requestIdCorreccionActual.isBlank()) {
            Toast.makeText(this, "No se encontró la solicitud", Toast.LENGTH_SHORT).show()
            return
        }

        val rutaCorreccion = "contribution_uploads/$requestIdCorreccionActual/correccion.pdf"
        val ref = storage.reference.child(rutaCorreccion)

        Toast.makeText(this, "Subiendo corrección...", Toast.LENGTH_SHORT).show()

        ref.putFile(uri)
            .addOnSuccessListener {
                ref.downloadUrl
                    .addOnSuccessListener { downloadUri ->

                        db.collection("users")
                            .document(uidCorrector)
                            .get()
                            .addOnSuccessListener { userDoc ->

                                val nombre = userDoc.getString("nombre") ?: "Corrector"
                                val apellidos = userDoc.getString("apellidos") ?: ""
                                val nombreCompleto = "$nombre $apellidos".trim()

                                val requestRef = db.collection("contribution_requests")
                                    .document(requestIdCorreccionActual)

                                requestRef.update(
                                    mapOf(
                                        //"status" to "changes_requested",
                                        "status" to "proofreader_rejected",
                                        "proofreaderId" to uidCorrector,
                                        "proofreaderName" to nombreCompleto,
                                        "reviewNotes" to reviewNotesActual,
                                        "correctionPath" to rutaCorreccion,
                                        "correctionUrl" to downloadUri.toString(),
                                        "proofreadAt" to FieldValue.serverTimestamp()
                                    )
                                )
                                    .addOnSuccessListener {

                                        requestRef.get()
                                            .addOnSuccessListener { requestDoc ->
                                                val translatorId =
                                                    requestDoc.getString("translatorId") ?: ""

                                                if (translatorId.isNotBlank()) {
                                                    db.collection("users")
                                                        .document(translatorId)
                                                        .update(
                                                            mapOf(
                                                                "notificationPending" to true,
                                                                "notificationMessage" to "El corrector ha solicitado cambios en tu traducción.",
                                                                "lastNotificationAt" to FieldValue.serverTimestamp()
                                                            )
                                                        )
                                                }
                                            }

                                        Toast.makeText(
                                            this,
                                            "Corrección subida. El traductor recibirá la notificación.",
                                            Toast.LENGTH_LONG
                                        ).show()

                                        requestIdCorreccionActual = ""
                                        reviewNotesActual = ""

                                        val uid = auth.currentUser?.uid ?: return@addOnSuccessListener
                                        cargarAvisosCorrector(uid)
                                        cargarNotificaciones()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(
                                            this,
                                            "Error al actualizar solicitud: ${e.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                            }
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Error al subir corrección: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun abrirPdf(urlPdf: String) {
        if (urlPdf.isBlank()) {
            Toast.makeText(this, "No se encontró el PDF", Toast.LENGTH_LONG).show()
            return
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(urlPdf), "application/pdf")
            addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            startActivity(Intent.createChooser(intent, "Abrir PDF con"))
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo abrir el PDF", Toast.LENGTH_LONG).show()
        }
    }

    private fun rechazarSinPdf(requestId: String, motivo: String) {
        val uidCorrector = auth.currentUser?.uid ?: return

        db.collection("users")
            .document(uidCorrector)
            .get()
            .addOnSuccessListener { userDoc ->

                val nombre = userDoc.getString("nombre") ?: "Corrector"
                val apellidos = userDoc.getString("apellidos") ?: ""
                val nombreCompleto = "$nombre $apellidos".trim()

                val requestRef = db.collection("contribution_requests")
                    .document(requestId)

                requestRef.update(
                    mapOf(
                        // "status" to "changes_requested",
                        "status" to "proofreader_rejected",
                        "proofreaderId" to uidCorrector,
                        "proofreaderName" to nombreCompleto,
                        "reviewNotes" to motivo,
                        "correctionPath" to "",
                        "correctionUrl" to "",
                        "proofreadAt" to FieldValue.serverTimestamp()
                    )
                )
                    .addOnSuccessListener {

                        requestRef.get()
                            .addOnSuccessListener { requestDoc ->

                                val translatorId =
                                    requestDoc.getString("translatorId") ?: ""

                                if (translatorId.isNotBlank()) {

                                    db.collection("users")
                                        .document(translatorId)
                                        .update(
                                            mapOf(
                                                "notificationPending" to true,
                                                "notificationMessage" to "El corrector ha solicitado cambios en tu traducción.",
                                                "lastNotificationAt" to FieldValue.serverTimestamp()
                                            )
                                        )
                                }
                            }

                        Toast.makeText(
                            this,
                            "Traducción rechazada. El traductor recibirá la notificación.",
                            Toast.LENGTH_LONG
                        ).show()

                        cargarAvisosCorrector(uidCorrector)
                        cargarNotificaciones()
                    }
                    .addOnFailureListener { e ->

                        Toast.makeText(
                            this,
                            "Error al actualizar solicitud: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
    }

    private fun abrirPdfDesdeStorage(rutaPdf: String) {
        if (rutaPdf.isBlank()) {
            Toast.makeText(this, "No se encontró el PDF de corrección", Toast.LENGTH_LONG).show()
            return
        }

        storage.reference.child(rutaPdf).downloadUrl
            .addOnSuccessListener { uri ->
                abrirPdf(uri.toString())
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Error al abrir PDF de corrección: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun pintarNotificacionesOrdenadas(lista: MutableList<NotificacionesItem>) {
        llAvisosCorrector.removeAllViews()

        val ordenadas = lista.sortedByDescending {
            it.fecha?.toDate()?.time ?: 0L
        }

        if (ordenadas.isEmpty()) {
            tvNotificaciones.text = "No tienes notificaciones."
            return
        }

        tvNotificaciones.text = "Tus notificaciones"

        for (notificacion in ordenadas) {
            llAvisosCorrector.addView(crearCardNotificacion(notificacion))
        }
        marcarNotificacionesComoLeidas()
    }

    private fun crearCardNotificacion(notificacion: NotificacionesItem): LinearLayout {
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.setPadding(24, 24, 24, 24)

        if (notificacion.esNueva) {
            card.background = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.WHITE)
                setStroke(5, android.graphics.Color.parseColor("#333333"))
                cornerRadius = 18f
            }
        } else {
            card.background = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.WHITE)
                setStroke(1, android.graphics.Color.parseColor("#DDDDDD"))
                cornerRadius = 18f
            }
        }

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(24, 0, 24, 18)
        card.layoutParams = params

        val tvTexto = TextView(this)
        tvTexto.text = notificacion.texto
        tvTexto.textSize = 15f
        tvTexto.setTextColor(android.graphics.Color.parseColor("#222222"))

        card.addView(tvTexto)

        if (notificacion.accion != null && notificacion.textoBoton.isNotBlank()) {
            val btnAccion = Button(this)
            btnAccion.text = notificacion.textoBoton
            btnAccion.isAllCaps = false
            btnAccion.setOnClickListener {
                notificacion.accion.invoke()
            }

            card.addView(btnAccion)
        }

        return card
    }

    private fun mostrarDetalleCambiosTraductor(
        requestId: String,
        bookId: String,
        bookTitle: String,
        sourceLanguage: String,
        targetLanguage: String,
        corrector: String,
        motivo: String,
        correctionUrl: String,
        correctionPath: String
    ) {
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(32, 16, 32, 8)

        val tvInfo = TextView(this)
        tvInfo.text =
            """
        Libro: $bookTitle
        Corrector: $corrector
        
        Comentario:
        ${motivo.ifBlank { "Sin comentario indicado" }}
        """.trimIndent()
        tvInfo.textSize = 15f

        layout.addView(tvInfo)

        if (correctionUrl.isNotBlank() || correctionPath.isNotBlank()) {
            val btnVerPdf = Button(this)
            btnVerPdf.text = "Ver PDF del corrector"
            btnVerPdf.isAllCaps = false

            btnVerPdf.setOnClickListener {
                if (correctionUrl.isNotBlank()) {
                    abrirPdf(correctionUrl)
                } else {
                    abrirPdfDesdeStorage(correctionPath)
                }
            }

            layout.addView(btnVerPdf)
        }

        val btnSubirNueva = Button(this)
        btnSubirNueva.text = "Subir traducción corregida"
        btnSubirNueva.isAllCaps = false

        btnSubirNueva.setOnClickListener {
            val intent = Intent(this, SolicitudTraduccion::class.java).apply {
                putExtra("bookId", bookId)
                putExtra("bookTitle", bookTitle)
                putExtra("sourceLanguage", sourceLanguage)
                putExtra("targetLanguage", targetLanguage)
                putExtra("requestId", requestId)
                putExtra("modo", "subir_cambios")
            }
            startActivity(intent)
        }

        layout.addView(btnSubirNueva)

        AlertDialog.Builder(this)
            .setTitle("Corrección solicitada")
            .setView(layout)
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun esNotificacionNueva(
        fecha: com.google.firebase.Timestamp?,
        ultimaLectura: com.google.firebase.Timestamp?
    ): Boolean {
        if (fecha == null) return false
        if (ultimaLectura == null) return true

        return fecha.toDate().time > ultimaLectura.toDate().time
    }

    private fun cargarTraduccionesPendientesCorrector(
        uid: String,
        listaNotificaciones: MutableList<NotificacionesItem>,
        ultimaLectura: com.google.firebase.Timestamp?
    ) {
        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { userDoc ->

                val rol = userDoc.getString("rol") ?: ""
                val estado = userDoc.getString("roleVerificationStatus") ?: ""
                val idiomaCertificado = userDoc.getString("nativeLanguage") ?: ""

                if (rol != "proofreader" || estado != "verified") {
                    pintarNotificacionesOrdenadas(listaNotificaciones)
                    return@addOnSuccessListener
                }

                db.collection("contribution_requests")
                    .whereEqualTo("status", "waiting_for_proofreader")
                    .whereEqualTo("targetLanguage", idiomaCertificado)
                    .get()
                    .addOnSuccessListener { documentos ->

                        for (doc in documentos) {
                            val requestId = doc.id
                            val titulo = doc.getString("bookTitle") ?: "Libro"
                            val traductor = doc.getString("translatorName") ?: "Traductor"
                            val sourceLanguage = doc.getString("sourceLanguage") ?: ""
                            val targetLanguage = doc.getString("targetLanguage") ?: ""
                            val translationUrl = doc.getString("translationUrl") ?: ""

                            val fecha = doc.getTimestamp("uploadedAt")
                                ?: doc.getTimestamp("createdAt")

                            listaNotificaciones.add(
                                NotificacionesItem(
                                    texto = "📘 Nueva traducción pendiente de corregir.\nLibro: $titulo\nTraductor: $traductor\nIdioma origen: $sourceLanguage\nIdioma destino: $targetLanguage\nFecha: ${formatearFecha(fecha)}",
                                    fecha = fecha,
                                    esNueva = esNotificacionNueva(fecha, ultimaLectura),
                                    textoBoton = "Corregir traducción",
                                    accion = {
                                        mostrarDetalleCorreccionPendiente(
                                            requestId = requestId,
                                            bookTitle = titulo,
                                            translatorName = traductor,
                                            sourceLanguage = sourceLanguage,
                                            targetLanguage = targetLanguage,
                                            translationUrl = translationUrl,
                                            uidCorrector = uid
                                        )
                                    }
                                )
                            )
                        }

                        pintarNotificacionesOrdenadas(listaNotificaciones)
                    }
            }
    }

    private fun mostrarDetalleCorreccionPendiente(
        requestId: String,
        bookTitle: String,
        translatorName: String,
        sourceLanguage: String,
        targetLanguage: String,
        translationUrl: String,
        uidCorrector: String
    ) {
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(32, 16, 32, 8)

        val tvInfo = TextView(this)
        tvInfo.text =
            """
        Libro: $bookTitle
        Traductor: $translatorName
        Idioma origen: $sourceLanguage
        Idioma destino: $targetLanguage
        """.trimIndent()
        tvInfo.textSize = 15f

        val btnVerPdf = Button(this)
        btnVerPdf.text = "Ver PDF traducido"
        btnVerPdf.isAllCaps = false
        btnVerPdf.setOnClickListener {
            abrirPdf(translationUrl)
        }

        val btnAceptar = Button(this)
        btnAceptar.text = "Validar traducción"
        btnAceptar.isAllCaps = false
        btnAceptar.setOnClickListener {
            aceptarCorreccion(requestId, uidCorrector)
        }

        val btnRechazar = Button(this)
        btnRechazar.text = "Rechazar / añadir comentario o PDF"
        btnRechazar.isAllCaps = false
        btnRechazar.setOnClickListener {
            pedirMotivoYSubirCorreccion(requestId)
        }

        layout.addView(tvInfo)
        layout.addView(btnVerPdf)
        layout.addView(btnAceptar)
        layout.addView(btnRechazar)

        AlertDialog.Builder(this)
            .setTitle("Corrección pendiente")
            .setView(layout)
            .setNegativeButton("Cerrar", null)
            .show()
    }
}