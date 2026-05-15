package com.example.globalbiblion

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import android.content.Intent
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.firestore.Query

class PanelAdministrador : Bars() {


    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var btnCertificados: Button
    private lateinit var btnSolicitudes: Button
    private lateinit var rvAdmin: RecyclerView
    private lateinit var tvTituloSeccion: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnVolver: ImageButton
    private lateinit var btnHistorial: Button
    private val listaHistorial = mutableListOf<HistorialAdmin>()
    private val listaCertificados = mutableListOf<CertificadoPendiente>()
    private val listaSolicitudes = mutableListOf<SolicitudPendiente>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_panel_administrador)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        btnCertificados = findViewById(R.id.btnCertificados)
        btnSolicitudes = findViewById(R.id.btnSolicitudes)
        rvAdmin = findViewById(R.id.rvAdmin)
        tvTituloSeccion = findViewById(R.id.tvTituloSeccion)
        progressBar = findViewById(R.id.progressBar)
        btnVolver = findViewById(R.id.btnVolver)
        btnHistorial = findViewById(R.id.btnHistorial)

        rvAdmin.layoutManager = LinearLayoutManager(this)

        comprobarSiEsAdmin()

        btnCertificados.setOnClickListener {
            cargarCertificadosPendientes()
        }

        btnSolicitudes.setOnClickListener {
            cargarSolicitudesPendientes()
        }
        btnHistorial.setOnClickListener {
            cargarHistorialAdmin()
        }
        btnVolver.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Cerrar sesión")
                .setMessage("¿Seguro que deseas cerrar sesión?")
                .setPositiveButton("Sí") { _, _ ->

                    auth.signOut()

                    val intent = Intent(this, MainActivity::class.java)

                    intent.flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TASK

                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun comprobarSiEsAdmin() {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            Toast.makeText(this, "Acceso denegado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        db.collection("users").document(uid).get()
            .addOnSuccessListener { documento ->
                val rol = documento.getString("rol")

                if (rol == "admin") {
                    cargarCertificadosPendientes()
                } else {
                    Toast.makeText(this, "Acceso denegado", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al comprobar permisos", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun cargarCertificadosPendientes() {
        tvTituloSeccion.text = "Certificados pendientes"
        progressBar.visibility = View.VISIBLE
        listaCertificados.clear()

        db.collection("users")
            .whereIn("roleVerificationStatus", listOf("pending_review", "prevalidated"))
            .get()
            .addOnSuccessListener { documentos ->

                val docsOrdenados = documentos.documents.sortedByDescending { doc ->
                    doc.getTimestamp("certificateUpdatedAt")?.toDate()?.time
                        ?: doc.getTimestamp("createdAt")?.toDate()?.time
                        ?: 0L
                }

                for (doc in docsOrdenados) {
                    listaCertificados.add(convertirCertificado(doc))
                }

                rvAdmin.adapter = CertificadoAdapter(listaCertificados) { certificado ->
                    mostrarDialogoCertificado(certificado)
                }

                progressBar.visibility = View.GONE

                if (listaCertificados.isEmpty()) {
                    Toast.makeText(this, "No hay certificados pendientes", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Toast.makeText(
                    this,
                    "Error al cargar certificados: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

   private fun convertirCertificado(doc: com.google.firebase.firestore.DocumentSnapshot): CertificadoPendiente {
        val validation = doc.get("certificateValidation") as? Map<*, *>

        return CertificadoPendiente(
            uid = doc.id,
            nombreCompleto = doc.getString("nombreCompleto")
                ?: "${doc.getString("nombre") ?: ""} ${doc.getString("apellidos") ?: ""}",
            email = doc.getString("email") ?: "",
            rol = doc.getString("rol") ?: "",
            estado = doc.getString("roleVerificationStatus") ?: "",
            certificateUrl = doc.getString("certificateUrl") ?: "",
            roleCertificatePath = doc.getString("roleCertificatePath") ?: "",
            emisor = validation?.get("emisor")?.toString() ?: "",
            idioma = validation?.get("idioma")?.toString() ?: "",
            nivel = validation?.get("nivel")?.toString() ?: "",
            institucionValida = validation?.get("institucion_valida") as? Boolean ?: false,
            idiomaValido = validation?.get("idioma_valido") as? Boolean ?: false,
            nivelValido = validation?.get("nivel_valido") as? Boolean ?: false,
            fechaValida = validation?.get("fecha_valida") as? Boolean ?: false,
            codigoPresente = validation?.get("codigo_presente") as? Boolean ?: false,
            mensaje = validation?.get("mensaje")?.toString() ?: ""
        )
    }

    private fun mostrarDialogoCertificado(certificado: CertificadoPendiente) {
        AlertDialog.Builder(this)
            .setTitle(certificado.nombreCompleto)
            .setMessage(
                """
            Email: ${certificado.email}
            Rol: ${certificado.rol}
            Estado: ${certificado.estado}

            Emisor: ${certificado.emisor}
            Idioma: ${certificado.idioma}
            Nivel: ${certificado.nivel}

            Institución válida: ${certificado.institucionValida}
            Idioma válido: ${certificado.idiomaValido}
            Nivel válido: ${certificado.nivelValido}
            Fecha válida: ${certificado.fechaValida}
            Código presente: ${certificado.codigoPresente}

            Mensaje:
            ${certificado.mensaje}
            """.trimIndent()
            )
            .setPositiveButton("Validar") { _, _ ->
                aprobarCertificado(certificado.uid)
            }
            .setNegativeButton("Rechazar") { _, _ ->
                pedirMotivoRechazoCertificado(certificado.uid)
            }
            .setNeutralButton("Ver PDF")  { _, _ ->
                val pdf = if (certificado.certificateUrl.isNotBlank()) {
                    certificado.certificateUrl
                } else {
                    certificado.roleCertificatePath
                }

                abrirPdf(pdf)
            }
            .show()
    }

    private fun aprobarCertificado(uidUsuario: String) {
        db.collection("users").document(uidUsuario)
            .update(
                mapOf(
                    "roleVerificationStatus" to "verified",
                    "adminReviewedAt" to FieldValue.serverTimestamp(),
                    "notificationPending" to true,
                    "notificationMessage" to "Tu certificado ha sido aprobado."
                )
            )
            .addOnSuccessListener {
                Toast.makeText(this, "Certificado aprobado", Toast.LENGTH_SHORT).show()
                cargarCertificadosPendientes()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al aprobar", Toast.LENGTH_SHORT).show()
            }
    }

    private fun pedirMotivoRechazoCertificado(uidUsuario: String) {
        val input = EditText(this)
        input.hint = "Escribe el motivo del rechazo"
        input.minLines = 3

        AlertDialog.Builder(this)
            .setTitle("Rechazar certificado")
            .setMessage("Este motivo se guardará para avisar al usuario más adelante.")
            .setView(input)
            .setPositiveButton("Rechazar") { _, _ ->
                val motivo = input.text.toString().trim()

                if (motivo.isEmpty()) {
                    Toast.makeText(this, "Debes escribir un motivo", Toast.LENGTH_SHORT).show()
                } else {
                    rechazarCertificado(uidUsuario, motivo)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun rechazarCertificado(uidUsuario: String, nota: String) {
        db.collection("users").document(uidUsuario)
            .update(
                mapOf(
                    "roleVerificationStatus" to "rejected",
                    "adminReviewedAt" to FieldValue.serverTimestamp(),
                    "reviewNotes" to nota,
                    "notificationPending" to true,
                    "notificationMessage" to "Tu certificado ha sido rechazado. Motivo: $nota"
                )
            )
            .addOnSuccessListener {
                Toast.makeText(this, "Certificado rechazado", Toast.LENGTH_SHORT).show()
                cargarCertificadosPendientes()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al rechazar", Toast.LENGTH_SHORT).show()
            }
    }

    private fun cargarSolicitudesPendientes() {
        tvTituloSeccion.text = "Solicitudes pendientes"
        progressBar.visibility = View.VISIBLE
        listaSolicitudes.clear()

        db.collection("contribution_requests")
            //.whereIn("status", listOf("proofreader_approved", "proofreader_rejected"))
            //.get()
            .whereIn("status", listOf("proofreader_approved", "proofreader_rejected"))
            .orderBy("proofreadAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documentos ->
                for (doc in documentos) {
                    listaSolicitudes.add(
                        SolicitudPendiente(
                            id = doc.id,
                            bookTitle = doc.getString("bookTitle") ?: "",
                            translatorName = doc.getString("translatorName") ?: "Sin traductor",
                            proofreaderName = doc.getString("proofreaderName") ?: "Sin corrector",
                            requestType = doc.getString("requestType") ?: "",
                            sourceLanguage = doc.getString("sourceLanguage") ?: "",
                            targetLanguage = doc.getString("targetLanguage") ?: "",
                            createdAt = doc.getTimestamp("createdAt")?.toDate().toString(),
                            status = doc.getString("status") ?: "",
                           // message = doc.getString("message") ?: "",
                            message = doc.getString("reviewNotes")
                                ?: doc.getString("adminNotes")
                                ?: doc.getString("message")
                                ?: "",
                            translationUrl = doc.getString("translationUrl") ?: "",
                            correctionUrl = doc.getString("correctionUrl") ?: "",
                            reviewNotes = doc.getString("reviewNotes") ?: ""
                        )
                    )
                }

                rvAdmin.adapter = SolicitudAdapter(listaSolicitudes) { solicitud ->
                    mostrarDialogoSolicitud(solicitud)
                }

                progressBar.visibility = View.GONE
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Error al cargar solicitudes", Toast.LENGTH_SHORT).show()
            }
    }

    private fun mostrarDialogoSolicitud(solicitud: SolicitudPendiente) {
        AlertDialog.Builder(this)
            .setTitle(solicitud.bookTitle)
            .setMessage(
                """
            Traductor: ${solicitud.translatorName}
            Corrector: ${solicitud.proofreaderName}
            Tipo: ${solicitud.requestType}
            Idioma origen: ${solicitud.sourceLanguage}
            Idioma destino: ${solicitud.targetLanguage}
            Estado: ${solicitud.status}

            Mensaje:
            ${solicitud.message}
            """.trimIndent()
            )
            .setPositiveButton(
                if (solicitud.status == "proofreader_approved") "Publicar"
                else "Aceptar rechazo"
            ) { _, _ ->
                aprobarSolicitud(solicitud.id, solicitud.status)
            }
            .setNegativeButton(
                if (solicitud.status == "proofreader_approved") "No publicar"
                else "No aceptar rechazo"
            ) { _, _ ->
                pedirMotivoRechazoSolicitud(solicitud.id, solicitud.status)
            }
            .setNeutralButton("Ver PDF") { _, _ ->
                //abrirPdf(solicitud.translationUrl)
                mostrarOpcionesPdfSolicitud(solicitud)
            }
            .show()
    }

    private fun aprobarSolicitud(requestId: String, statusActual: String) {
        val uidAdmin = auth.currentUser?.uid ?: return

        val nuevosDatos = when (statusActual) {
            "proofreader_approved" -> mapOf(
                "status" to "published",
                "adminReviewedBy" to uidAdmin,
                "adminReviewedAt" to FieldValue.serverTimestamp(),
                "notificationPending" to true,
                "notificationMessage" to "Tu traducción ha sido publicada."
            )

            "proofreader_rejected" -> mapOf(
                "status" to "changes_requested",
                "adminReviewedBy" to uidAdmin,
                "adminReviewedAt" to FieldValue.serverTimestamp(),
                "notificationPending" to true,
                "notificationMessage" to "El administrador ha aceptado la corrección. Debes subir una nueva versión."
            )

            else -> return
        }

        db.collection("contribution_requests")
            .document(requestId)
            .update(nuevosDatos)
            .addOnSuccessListener {
                Toast.makeText(this, "Solicitud actualizada", Toast.LENGTH_SHORT).show()
                cargarSolicitudesPendientes()
            }
    }

    private fun pedirMotivoRechazoSolicitud(requestId: String, statusActual: String) {
        val input = EditText(this)
        input.hint = "Escribe una nota para justificar la decisión"
        input.minLines = 3

        AlertDialog.Builder(this)
            .setTitle("Revisar solicitud")
            .setView(input)
            .setPositiveButton("Confirmar") { _, _ ->
                val nota = input.text.toString().trim()
                rechazarSolicitud(requestId, statusActual, nota)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun rechazarSolicitud(requestId: String, statusActual: String, nota: String) {
        val uidAdmin = auth.currentUser?.uid ?: return

        val nuevosDatos = when (statusActual) {
            "proofreader_approved" -> mapOf(
                "status" to "waiting_for_proofreader",
                "proofreaderId" to "",
                "proofreaderName" to "",
                "adminNotes" to nota,
                "adminReviewedBy" to uidAdmin,
                "adminReviewedAt" to FieldValue.serverTimestamp(),
                "notificationPending" to true,
                "notificationMessage" to "El administrador no publicó la traducción. Se buscará otro corrector."
            )

            "proofreader_rejected" -> mapOf(
                "status" to "waiting_for_proofreader",
                "proofreaderId" to "",
                "proofreaderName" to "",
                "adminNotes" to nota,
                "adminReviewedBy" to uidAdmin,
                "adminReviewedAt" to FieldValue.serverTimestamp(),
                "notificationPending" to true,
                "notificationMessage" to "El administrador no aceptó el rechazo. Se buscará otro corrector."
            )

            else -> return
        }

        db.collection("contribution_requests")
            .document(requestId)
            .update(nuevosDatos)
            .addOnSuccessListener {
                Toast.makeText(this, "Solicitud devuelta a correctores", Toast.LENGTH_SHORT).show()
                cargarSolicitudesPendientes()
            }
    }
    private fun abrirPdf(pdf: String) {
        if (pdf.isBlank()) {
            Toast.makeText(this, "No se ha encontrado el PDF", Toast.LENGTH_LONG).show()
            return
        }

        if (pdf.startsWith("http")) {
            abrirUrlPdf(pdf)
        } else {
            FirebaseStorage.getInstance()
                .reference
                .child(pdf)
                .downloadUrl
                .addOnSuccessListener { uri ->
                    abrirUrlPdf(uri.toString())
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        this,
                        "No se pudo obtener el PDF: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }

    private fun abrirUrlPdf(urlPdf: String) {
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

    private fun cargarHistorialAdmin() {
        tvTituloSeccion.text = "Historial administrador"
        progressBar.visibility = View.VISIBLE
        listaHistorial.clear()

        cargarHistorialCertificados()
    }

    private fun cargarHistorialCertificados() {
        db.collection("users")
            .whereIn("roleVerificationStatus", listOf("verified", "rejected"))
            .get()
            .addOnSuccessListener { documentos ->

                for (doc in documentos) {
                    val nombre = doc.getString("nombreCompleto")
                        ?: "${doc.getString("nombre") ?: ""} ${doc.getString("apellidos") ?: ""}"

                    val fechaTimestamp = doc.getTimestamp("adminReviewedAt")

                    listaHistorial.add(
                        HistorialAdmin(
                            id = doc.id,
                            tipo = "Certificado",
                            titulo = nombre,
                            usuario = doc.getString("email") ?: "",
                            estado = doc.getString("roleVerificationStatus") ?: "",
                            motivo = doc.getString("reviewNotes") ?: "Sin motivo registrado",
                            fecha = formatearFecha(fechaTimestamp),
                            fechaMillis = fechaTimestamp?.toDate()?.time ?: 0L
                        )
                    )
                }

                cargarHistorialSolicitudes()
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Error al cargar historial de certificados", Toast.LENGTH_SHORT).show()
            }
    }

    private fun cargarHistorialSolicitudes() {
        db.collection("contribution_requests")
            .whereIn(
                "status",
                listOf(
                    "published",
                    "changes_requested",
                    "waiting_for_proofreader",
                    "translation_vacancy_open"
                )
            )
            .get()
            .addOnSuccessListener { documentos ->

                for (doc in documentos) {
                    val fechaTimestamp =
                        doc.getTimestamp("adminReviewedAt")
                            ?: doc.getTimestamp("proofreadAt")
                            ?: doc.getTimestamp("uploadedAt")
                            ?: doc.getTimestamp("createdAt")

                    listaHistorial.add(
                        HistorialAdmin(
                            id = doc.id,
                            tipo = "Solicitud",
                            titulo = doc.getString("bookTitle") ?: "Solicitud",
                            usuario = doc.getString("translatorName") ?: "",
                            estado = doc.getString("status") ?: "",
                            motivo = doc.getString("adminNotes")
                                ?: doc.getString("reviewNotes")
                                ?: "Sin motivo registrado",
                            fecha = formatearFecha(fechaTimestamp),
                            fechaMillis = fechaTimestamp?.toDate()?.time ?: 0L
                        )
                    )
                }

                listaHistorial.sortByDescending { it.fechaMillis }

                rvAdmin.adapter = HistorialAdminAdapter(listaHistorial) { item ->
                    mostrarDialogoHistorial(item)
                }

                progressBar.visibility = View.GONE
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Error al cargar historial de solicitudes", Toast.LENGTH_SHORT).show()
            }
    }

    private fun mostrarDialogoHistorial(item: HistorialAdmin) {
        AlertDialog.Builder(this)
            .setTitle("${item.tipo}: ${item.estado}")
            .setMessage(
                """
            ${item.titulo}
            
            Usuario: ${item.usuario}
            Fecha: ${item.fecha}
            
            Motivo / notas:
            ${item.motivo}
            """.trimIndent()
            )
            .setPositiveButton("Cerrar", null)
            .show()
    }
    private fun mostrarOpcionesPdfSolicitud(solicitud: SolicitudPendiente) {
        val opciones = mutableListOf<String>()
        val pdfs = mutableListOf<String>()

        if (solicitud.translationUrl.isNotBlank()) {
            opciones.add("Ver PDF del traductor")
            pdfs.add(solicitud.translationUrl)
        }

        if (solicitud.correctionUrl.isNotBlank()) {
            opciones.add("Ver PDF del corrector")
            pdfs.add(solicitud.correctionUrl)
        }

        if (opciones.isEmpty()) {
            Toast.makeText(this, "No hay PDFs disponibles", Toast.LENGTH_LONG).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Selecciona el PDF")
            .setItems(opciones.toTypedArray()) { _, posicion ->
                abrirPdf(pdfs[posicion])
            }
            .show()
    }
    private fun formatearFecha(timestamp: com.google.firebase.Timestamp?): String {
        if (timestamp == null) return "Fecha desconocida"

        val formato = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
        return formato.format(timestamp.toDate())
    }
}