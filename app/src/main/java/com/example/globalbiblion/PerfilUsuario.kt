package com.example.globalbiblion

import android.os.Bundle
import android.util.Log
import android.view.View //Para poder mostrar los campos según el rol
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.firestore.FieldValue
import com.google.firebase.storage.FirebaseStorage
class PerfilUsuario : BottomBar() {
    //Variables de Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    //Variables de la interfaz de usuario
    private lateinit var tvNombre: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvRol: TextView
    private lateinit var tvIdiomaNativo: TextView
    private lateinit var tvEstadoVerificacion: TextView
    private lateinit var tvRolTitulo: TextView
    private lateinit var tvNumeroResenas: TextView
    private lateinit var tvNumeroSolicitudes: TextView
    private lateinit var tvNumeroTraducciones: TextView
    private lateinit var cardSolicitudes: LinearLayout
    private lateinit var cardTraducciones: LinearLayout

    private lateinit var btnVolver: ImageButton
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
        setContentView(R.layout.activity_perfil_usuario)
        configurarBottomBar()

        // Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        // --- FindViewById ---
        tvNombre = findViewById(R.id.tvNombre)
        tvEmail = findViewById(R.id.tvEmail)
        tvRol = findViewById(R.id.tvRol)
        tvIdiomaNativo = findViewById(R.id.tvIdiomaNativo)
        tvEstadoVerificacion = findViewById(R.id.tvEstadoVerificacion)
        tvRolTitulo = findViewById(R.id.tvRolTitulo)
        tvNumeroResenas = findViewById(R.id.tvNumeroResenas)
        tvNumeroSolicitudes = findViewById(R.id.tvNumeroSolicitudes)
        tvNumeroTraducciones = findViewById(R.id.tvNumeroTraducciones)
        btnSubirCertificadoNuevo = findViewById(R.id.btnSubirCertificadoNuevo)
        btnSubirCertificadoNuevo.visibility = View.GONE

        btnVolver = findViewById(R.id.btnVolver)

        cardTraducciones = findViewById(R.id.cardTraducciones)
        cardSolicitudes = findViewById(R.id.cardSolicitudes)

        btnVolver.setOnClickListener {
            finish()
        }
        btnSubirCertificadoNuevo.setOnClickListener {
            seleccionarNuevoCertificado.launch("application/pdf")
        }

        // Cargar datos desde Firebase
        cargarDatosUsuario()
        cargarContribuciones()
    }

    // ------------------ INFO PERSONAL ------------------

    private fun cargarDatosUsuario() {
        val uid = auth.currentUser?.uid //Obtenemos la uid del usuario actual

        if (uid == null) {
            Toast.makeText(
                this,
                "Debes iniciar sesión para ver tu perfil",
                Toast.LENGTH_LONG).show()
            finish()
            return
        }
        //ruta de Firebase
        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    val nombre = doc.getString("nombre") ?: ""
                    val email = doc.getString("email") ?: auth.currentUser?.email ?: ""
                    val rol = doc.getString("rol") ?: ""
                    val idiomaNativo = doc.getString("nativeLanguage") ?: ""
                    val estadoVerificacion = doc.getString("roleVerificationStatus") ?: ""

                    tvNombre.text = if (nombre.isNotEmpty()) nombre else "Nombre"
                    tvEmail.text = if (email.isNotEmpty()) email else "Email"

                    if (rol == "reader") {
                        tvRol.visibility = View.GONE
                        tvRolTitulo.visibility = View.GONE
                        tvIdiomaNativo.visibility = View.GONE
                        tvEstadoVerificacion.visibility = View.GONE

                        cardTraducciones.visibility = View.GONE
                    } else {
                        tvRol.visibility = View.VISIBLE
                        tvRolTitulo.visibility = View.VISIBLE
                        tvIdiomaNativo.visibility = View.VISIBLE
                        tvEstadoVerificacion.visibility = View.VISIBLE

                        cardTraducciones.visibility = View.VISIBLE

                        tvRol.text = "Rol: $rol"
                        tvIdiomaNativo.text =
                            if (idiomaNativo.isNotEmpty()) "Idioma nativo: $idiomaNativo"
                            else "Idioma nativo"

                        mostrarEstadoVerificacion(estadoVerificacion)
                    }

                } else {
                    Toast.makeText(this, "No se encontraron datos de usuario", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al cargar usuario: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }


    private fun cargarContribuciones() {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            tvNumeroResenas.text = "0"
            tvNumeroSolicitudes.text = "0"
            tvNumeroTraducciones.text = "0"
            return
        }

        // Reseñas
        db.collectionGroup("reviews")
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { documentos ->
                tvNumeroResenas.text = documentos.size().toString()
            }
            .addOnFailureListener {e ->
                tvNumeroResenas.text = "0"
                Toast.makeText(
                    this,
                    "Error al contar reseñas: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                Log.e("FIREBASE_REVIEWS", "Error completo", e)
            }

        // Solicitudes hechas por el usuario
        db.collection("contribution_requests")
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { documentos ->
                tvNumeroSolicitudes.text = documentos.size().toString()
            }
            .addOnFailureListener {
                tvNumeroSolicitudes.text = "0"
            }

        // Traducciones/correcciones subidas
        db.collection("contribution_requests")
            .whereEqualTo("userId", uid)
            .whereEqualTo("status", "uploaded")
            .get()
            .addOnSuccessListener { documentos ->
                tvNumeroTraducciones.text = documentos.size().toString()
            }
            .addOnFailureListener {e ->
                tvNumeroTraducciones.text = "0"
                Toast.makeText(
                    this,
                    "Error al contar reseñas: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
    private fun mostrarEstadoVerificacion(estado: String) {
        when (estado) {
            "rejected" -> {
                tvEstadoVerificacion.text = "Estado de verificación: rechazado"
                tvEstadoVerificacion.setTextColor(android.graphics.Color.RED)
                btnSubirCertificadoNuevo.visibility = View.VISIBLE
            }

            "verified" -> {
                tvEstadoVerificacion.text = "Estado de verificación: verificado"
                tvEstadoVerificacion.setTextColor(android.graphics.Color.parseColor("#2E7D32"))
                btnSubirCertificadoNuevo.visibility = View.GONE
            }

            "pending_review", "prevalidated" -> {
                tvEstadoVerificacion.text = "Estado de verificación: pendiente de revisión"
                tvEstadoVerificacion.setTextColor(android.graphics.Color.parseColor("#F9A825"))
                btnSubirCertificadoNuevo.visibility = View.GONE
            }

            else -> {
                tvEstadoVerificacion.text = "Estado de verificación"
                tvEstadoVerificacion.setTextColor(android.graphics.Color.DKGRAY)
                btnSubirCertificadoNuevo.visibility = View.GONE
            }
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
                                cargarDatosUsuario()
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