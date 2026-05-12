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
import com.google.firebase.firestore.QueryDocumentSnapshot
import android.content.Intent
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage

class PanelAdministrador : BottomBar() {


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
            finish()
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
                for (doc in documentos) {
                    listaCertificados.add(convertirCertificado(doc))
                }

                rvAdmin.adapter = CertificadoAdapter(listaCertificados) { certificado ->
                    mostrarDialogoCertificado(certificado)
                }

                progressBar.visibility = View.GONE
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Error al cargar certificados", Toast.LENGTH_SHORT).show()
            }
    }

    private fun convertirCertificado(doc: QueryDocumentSnapshot): CertificadoPendiente {
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
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { documentos ->
                for (doc in documentos) {
                    listaSolicitudes.add(
                        SolicitudPendiente(
                            id = doc.id,
                            bookTitle = doc.getString("bookTitle") ?: "",
                            userName = doc.getString("userName") ?: "",
                            requestType = doc.getString("requestType") ?: "",
                            sourceLanguage = doc.getString("sourceLanguage") ?: "",
                            targetLanguage = doc.getString("targetLanguage") ?: "",
                            createdAt = doc.getTimestamp("createdAt")?.toDate().toString(),
                            status = doc.getString("status") ?: "",
                            message = doc.getString("message") ?: "",
                            fileUrl = doc.getString("fileUrl")
                                ?: doc.getString("pdfUrl")
                                ?: doc.getString("translationUrl")
                                ?: ""
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
            Usuario: ${solicitud.userName}
            Tipo: ${solicitud.requestType}
            Idioma origen: ${solicitud.sourceLanguage}
            Idioma destino: ${solicitud.targetLanguage}
            Estado: ${solicitud.status}

            Mensaje:
            ${solicitud.message}
            """.trimIndent()
            )
            .setPositiveButton("Aprobar") { _, _ ->
                aprobarSolicitud(solicitud.id)
            }
            .setNegativeButton("Rechazar") { _, _ ->
                pedirMotivoRechazoSolicitud(solicitud.id)
            }
            .setNeutralButton("Ver PDF") { _, _ ->
                abrirPdf(solicitud.fileUrl)
            }
            .show()
    }

    private fun aprobarSolicitud(requestId: String) {
        val uidAdmin = auth.currentUser?.uid ?: return

        db.collection("contribution_requests").document(requestId)
            .update(
                mapOf(
                    "status" to "approved",
                    "adminReviewedBy" to uidAdmin,
                    "adminReviewedAt" to FieldValue.serverTimestamp(),
                    "notificationPending" to true,
                    "notificationMessage" to "Tu solicitud ha sido aprobada."
                )
            )
            .addOnSuccessListener {
                Toast.makeText(this, "Solicitud aprobada", Toast.LENGTH_SHORT).show()
                cargarSolicitudesPendientes()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al aprobar solicitud", Toast.LENGTH_SHORT).show()
            }
    }

    private fun pedirMotivoRechazoSolicitud(requestId: String) {
        val input = EditText(this)
        input.hint = "Escribe el motivo del rechazo"
        input.minLines = 3

        AlertDialog.Builder(this)
            .setTitle("Rechazar solicitud")
            .setMessage("Este motivo se guardará para avisar al usuario más adelante.")
            .setView(input)
            .setPositiveButton("Rechazar") { _, _ ->
                val motivo = input.text.toString().trim()

                if (motivo.isEmpty()) {
                    Toast.makeText(this, "Debes escribir un motivo", Toast.LENGTH_SHORT).show()
                } else {
                    rechazarSolicitud(requestId, motivo)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun rechazarSolicitud(requestId: String, nota: String) {
        val uidAdmin = auth.currentUser?.uid ?: return

        db.collection("contribution_requests").document(requestId)
            .update(
                mapOf(
                    "status" to "rejected",
                    "adminReviewedBy" to uidAdmin,
                    "adminReviewedAt" to FieldValue.serverTimestamp(),
                    "reviewNotes" to nota,
                    "notificationPending" to true,
                    "notificationMessage" to "Tu solicitud ha sido rechazada. Motivo: $nota"
                )
            )
            .addOnSuccessListener {
                Toast.makeText(this, "Solicitud rechazada", Toast.LENGTH_SHORT).show()
                cargarSolicitudesPendientes()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al rechazar solicitud", Toast.LENGTH_SHORT).show()
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

                    listaHistorial.add(
                        HistorialAdmin(
                            id = doc.id,
                            tipo = "Certificado",
                            titulo = nombre,
                            usuario = doc.getString("email") ?: "",
                            estado = doc.getString("roleVerificationStatus") ?: "",
                            motivo = doc.getString("reviewNotes") ?: "Sin motivo registrado"
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
            .whereIn("status", listOf("approved", "rejected"))
            .get()
            .addOnSuccessListener { documentos ->

                for (doc in documentos) {
                    listaHistorial.add(
                        HistorialAdmin(
                            id = doc.id,
                            tipo = "Solicitud",
                            titulo = doc.getString("bookTitle") ?: "Solicitud",
                            usuario = doc.getString("userName") ?: "",
                            estado = doc.getString("status") ?: "",
                            motivo = doc.getString("reviewNotes") ?: "Sin motivo registrado"
                        )
                    )
                }

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
        if (item.estado == "rejected") {
            AlertDialog.Builder(this)
                .setTitle("${item.tipo} rechazado")
                .setMessage(
                    """
                ${item.titulo}
                
                Usuario: ${item.usuario}
                
                Motivo del rechazo:
                ${item.motivo}
                """.trimIndent()
                )
                .setPositiveButton("Cerrar", null)
                .show()
        } else {
            AlertDialog.Builder(this)
                .setTitle("${item.tipo} aprobado")
                .setMessage(
                    """
                ${item.titulo}
                
                Usuario: ${item.usuario}
                
                Estado: aprobado correctamente.
                """.trimIndent()
                )
                .setPositiveButton("Cerrar", null)
                .show()
        }
    }
}