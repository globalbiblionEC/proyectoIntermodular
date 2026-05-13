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

    private val seleccionarNuevoCertificado =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                uriCertificadoNuevo = uri
                subirNuevoCertificado(uri)
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
    }

    private fun cargarNotificacionesSolicitudes(uid: String, texto: StringBuilder) {
        db.collection("contribution_requests")
            .whereEqualTo("userId", uid)
            .whereIn("status", listOf("approved", "rejected"))
            .get()
            .addOnSuccessListener { documentos ->

                for (doc in documentos) {
                    val titulo = doc.getString("bookTitle") ?: "Solicitud"
                    val status = doc.getString("status") ?: ""
                    val motivo = doc.getString("reviewNotes") ?: "Sin motivo indicado"
                    val fecha = doc.getTimestamp("adminReviewedAt")

                    if (status == "approved") {
                        texto.append(  "✅ Solicitud aprobada: $titulo\n" +
                                "Fecha: ${formatearFecha(fecha)}\n\n")
                    }

                    if (status == "rejected") {
                        texto.append( "❌ Solicitud rechazada: $titulo\n" +
                                "Fecha: ${formatearFecha(fecha)}\n" +
                                "Motivo: $motivo\n\n")
                    }
                }

                if (texto.isEmpty()) {
                    texto.append("No tienes notificaciones.")
                }

                tvNotificaciones.text = texto.toString()
            }
            .addOnFailureListener {
                tvNotificaciones.text = "No se pudieron cargar las notificaciones."
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
}