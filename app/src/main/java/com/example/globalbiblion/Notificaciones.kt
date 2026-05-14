package com.example.globalbiblion

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
class Notificaciones : BottomBar() {


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
        marcarNotificacionesComoLeidas()
        cargarNotificaciones()

    }


    private fun cargarNotificaciones() {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            Toast.makeText(this, "Debes iniciar sesión", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val estado = doc.getString("roleVerificationStatus") ?: ""
                val mensaje = doc.getString("notificationMessage") ?: ""
                val fechaRevision = doc.getTimestamp("adminReviewedAt")

                val texto = StringBuilder()

                if (estado == "verified") {
                    btnSubirCertificadoNuevo.visibility = View.GONE
                    texto.append(
                        "✅ Tu certificado ha sido validado.\n" +
                                "Fecha: ${formatearFecha(fechaRevision)}\n\n"
                    )
                }

                if (estado == "rejected") {
                    btnSubirCertificadoNuevo.visibility = View.VISIBLE
                    val motivo = doc.getString("reviewNotes") ?: "Sin motivo indicado"
                    texto.append( "❌ Tu certificado ha sido rechazado.\n" +
                            "Fecha: ${formatearFecha(fechaRevision)}\n" +
                            "Motivo: $motivo\n\n")
                }

                if (mensaje.isNotEmpty()) {
                    texto.append("Aviso: $mensaje\n\n")
                }

                cargarNotificacionesSolicitudes(uid, texto)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar notificaciones", Toast.LENGTH_SHORT).show()
            }
        cargarAvisosCorrector(uid)
    }

    private fun cargarNotificacionesSolicitudes(uid: String, texto: StringBuilder) {

        db.collection("contribution_requests")
            .whereEqualTo("translatorId", uid)
            .whereIn(
                "status",
                listOf(
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
                    val titulo = doc.getString("bookTitle") ?: "Solicitud"
                    val status = doc.getString("status") ?: ""
                    val notas = doc.getString("reviewNotes")
                        ?: doc.getString("adminNotes")
                        ?: "Sin motivo indicado"
                    val fecha = doc.getTimestamp("adminReviewedAt")
                        ?: doc.getTimestamp("proofreadAt")

                    when (status) {
                        "proofreader_approved" -> {
                            texto.append(
                                "✅ Tu traducción de '$titulo' fue validada por el corrector.\n" +
                                        "Fecha: ${formatearFecha(fecha)}\n\n"
                            )
                        }

                        "proofreader_rejected" -> {
                            texto.append(
                                "❌ Tu traducción de '$titulo' fue rechazada por el corrector.\n" +
                                        "Motivo: $notas\n" +
                                        "Fecha: ${formatearFecha(fecha)}\n\n"
                            )
                        }

                        "changes_requested" -> {
                            texto.append(
                                "🔁 El admin solicita cambios en tu traducción de '$titulo'.\n" +
                                        "Motivo: $notas\n" +
                                        "Fecha: ${formatearFecha(fecha)}\n\n"
                            )
                        }

                        "published" -> {
                            texto.append(
                                "📚 Tu traducción de '$titulo' ha sido publicada.\n" +
                                        "Fecha: ${formatearFecha(fecha)}\n\n"
                            )
                        }

                        "translation_vacancy_open" -> {
                            texto.append(
                                "⚠️ La traducción de '$titulo' fue descartada y la vacante se ha reabierto.\n" +
                                        "Motivo: $notas\n" +
                                        "Fecha: ${formatearFecha(fecha)}\n\n"
                            )
                        }
                    }
                }

                cargarNotificacionesCorrector(uid, texto)
            }
            .addOnFailureListener { e ->
                tvNotificaciones.text = "Error cargando notificaciones del traductor: ${e.message}"
            }
    }

    private fun cargarNotificacionesCorrector(uid: String, texto: StringBuilder) {

        db.collection("contribution_requests")
            .whereEqualTo("proofreaderId", uid)
            .whereIn(
                "status",
                listOf(
                    "proofreader_approved",
                    "proofreader_rejected",
                    "published",
                    "changes_requested",
                    "waiting_for_proofreader"
                )
            )
            .get()
            .addOnSuccessListener { documentos ->

                for (doc in documentos) {
                    val titulo = doc.getString("bookTitle") ?: "Solicitud"
                    val status = doc.getString("status") ?: ""
                    val notas = doc.getString("adminNotes") ?: "Sin notas"
                    val fecha = doc.getTimestamp("adminReviewedAt")
                        ?: doc.getTimestamp("proofreadAt")

                    when (status) {
                        "proofreader_approved" -> {
                            texto.append(
                                "✅ Validaste la traducción de '$titulo'. Pendiente del admin.\n" +
                                        "Fecha: ${formatearFecha(fecha)}\n\n"
                            )
                        }

                        "proofreader_rejected" -> {
                            texto.append(
                                "❌ Rechazaste la traducción de '$titulo'. Pendiente del admin.\n" +
                                        "Fecha: ${formatearFecha(fecha)}\n\n"
                            )
                        }

                        "published" -> {
                            texto.append(
                                "📚 El admin publicó la traducción de '$titulo'.\n" +
                                        "Fecha: ${formatearFecha(fecha)}\n\n"
                            )
                        }

                        "changes_requested" -> {
                            texto.append(
                                "🔁 El admin aceptó tu rechazo de '$titulo'. Se devolverá al traductor.\n" +
                                        "Fecha: ${formatearFecha(fecha)}\n\n"
                            )
                        }

                        "waiting_for_proofreader" -> {
                            texto.append(
                                "🔎 El admin no aceptó el rechazo de '$titulo'. Buscará otro corrector.\n" +
                                        "Notas: $notas\n\n"
                            )
                        }
                    }
                }

                if (texto.isEmpty()) {
                    texto.append("No tienes notificaciones.")
                }

                tvNotificaciones.text = texto.toString()
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
                "notificationPending",
                false
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

                tvNombreUsuario.text = when {
                    nombre.isNotEmpty() && apellidos.isNotEmpty() -> "$nombre $apellidos"
                    nombre.isNotEmpty() -> nombre
                    else -> "Usuario"
                }
            }
            .addOnFailureListener {
                tvNombreUsuario.text = "Usuario"
            }
    }

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

                val certificateValidation = userDoc.get("certificateValidation") as? Map<*, *>
                val idiomaCertificado = certificateValidation?.get("idioma")?.toString() ?: ""

                if (rol != "proofreader" || estado != "verified") {
                    llAvisosCorrector.removeAllViews()
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

                                db.collection("contribution_requests")
                                    .document(requestIdCorreccionActual)
                                    .update(
                                        mapOf(
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
                                        Toast.makeText(
                                            this,
                                            "Corrección subida. Pasará al panel del administrador.",
                                            Toast.LENGTH_LONG
                                        ).show()

                                        requestIdCorreccionActual = ""
                                        reviewNotesActual = ""

                                        val uid = auth.currentUser?.uid ?: return@addOnSuccessListener
                                        cargarAvisosCorrector(uid)
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

                db.collection("contribution_requests")
                    .document(requestId)
                    .update(
                        mapOf(
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
                        Toast.makeText(
                            this,
                            "Traducción rechazada. Pasará al panel del administrador.",
                            Toast.LENGTH_LONG
                        ).show()

                        cargarAvisosCorrector(uidCorrector)
                    }
            }
    }
}