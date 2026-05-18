package com.example.globalbiblion
import android.content.Intent
import android.os.Bundle
import android.view.View //Para poder mostrar los campos según el rol
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.firestore.FieldValue
import com.google.firebase.storage.FirebaseStorage
import com.bumptech.glide.Glide //Para imágenes
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import android.util.Log
import com.google.firebase.functions.FirebaseFunctionsException
import android.os.Handler
import android.os.Looper

//Esta Activity es para mostrar el perfil del usuario
class PerfilUsuario : Bars() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
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
    private lateinit var ivPerfil: ImageView
    private lateinit var btnVolver: ImageButton
    private lateinit var btnSubirCertificadoNuevo: Button
    private lateinit var btnExportarDatos: Button
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
        btnExportarDatos = findViewById(R.id.btnExportarDatos)
        ivPerfil= findViewById(R.id.ivPerfilTopBar)

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
        btnExportarDatos.setOnClickListener {
            exportarDatosUsuario()
        }

        // Cargar datos desde Firebase
        cargarDatosUsuario()
        cargarContribuciones()
    }

    private fun cargarDatosUsuario() {

        val uid = auth.currentUser?.uid

        if (uid == null) {
            Toast.makeText(
                this,
                "Debes iniciar sesión para ver tu perfil",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

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
                    val profileImageUrl = doc.getString("profileImageUrl") ?: ""

                    tvNombre.text =
                        if (nombre.isNotEmpty()) nombre else "Nombre"

                    tvEmail.text =
                        if (email.isNotEmpty()) email else "Email"

                    if (profileImageUrl.isNotEmpty()) {

                        Glide.with(this)
                            .load(profileImageUrl)
                            .placeholder(R.drawable.usuarioleyendocfmenuprinc)
                            .error(R.drawable.usuarioleyendocfmenuprinc)
                            .circleCrop()
                            .into(ivPerfil)
                    }

                    if (rol == "reader") {

                        tvRolTitulo.visibility = View.VISIBLE
                        tvRolTitulo.text = obtenerNombreRol(rol)

                        tvRol.visibility = View.GONE
                        tvIdiomaNativo.visibility = View.GONE
                        tvEstadoVerificacion.visibility = View.GONE
                        btnSubirCertificadoNuevo.visibility = View.GONE

                        cardTraducciones.visibility = View.GONE

                    } else {

                        tvRolTitulo.visibility = View.VISIBLE
                        tvRolTitulo.text = obtenerNombreRol(rol)

                        tvRol.visibility = View.GONE
                        tvIdiomaNativo.visibility = View.VISIBLE
                        tvEstadoVerificacion.visibility = View.VISIBLE

                        cardTraducciones.visibility = View.VISIBLE

                        tvIdiomaNativo.text =
                            if (idiomaNativo.isNotEmpty())
                                "Idioma nativo: $idiomaNativo"
                            else
                                "Idioma nativo"

                        mostrarEstadoVerificacion(estadoVerificacion)
                    }

                } else {

                    Toast.makeText(
                        this,
                        "No se encontraron datos de usuario",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .addOnFailureListener { e ->

                Toast.makeText(
                    this,
                    "Error al cargar usuario: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
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

        db.collectionGroup("reviews")
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { documentos ->
                tvNumeroResenas.text = documentos.size().toString()
            }
            .addOnFailureListener { e ->
                tvNumeroResenas.text = "0"
                Log.e("FIREBASE_REVIEWS", "Error contando reseñas", e)
                Toast.makeText(
                    this,
                    "Error al contar reseñas: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }

        // SOLICITUDES COMO TRADUCTOR
        db.collection("contribution_requests")
            .whereEqualTo("translatorId", uid)
            .get()
            .addOnSuccessListener { solicitudesTraductor ->

                val totalSolicitudesTraductor = solicitudesTraductor.size()

                // SOLICITUDES COMO CORRECTOR
                db.collection("contribution_requests")
                    .whereEqualTo("proofreaderId", uid)
                    .get()
                    .addOnSuccessListener { solicitudesCorrector ->

                        val totalSolicitudesCorrector = solicitudesCorrector.size()
                        val totalSolicitudes = totalSolicitudesTraductor + totalSolicitudesCorrector

                        tvNumeroSolicitudes.text = totalSolicitudes.toString()
                    }
                    .addOnFailureListener {
                        tvNumeroSolicitudes.text = totalSolicitudesTraductor.toString()
                    }
            }
            .addOnFailureListener {
                tvNumeroSolicitudes.text = "0"
            }

        // TRADUCCIONES SUBIDAS POR EL TRADUCTOR
        db.collection("contribution_requests")
            .whereEqualTo("translatorId", uid)
            .whereIn(
                "status",
                listOf(
                    "waiting_for_proofreader",
                    "proofreader_approved",
                    "proofreader_rejected",
                    "waiting_for_admin",
                    "published",
                    "changes_requested",
                    "translation_vacancy_open"
                )
            )
            .get()
            .addOnSuccessListener { documentos ->
                tvNumeroTraducciones.text = documentos.size().toString()
            }
            .addOnFailureListener { e ->
                tvNumeroTraducciones.text = "0"
                Log.e("FIREBASE_TRANSLATIONS", "Error contando traducciones", e)
                Toast.makeText(
                    this,
                    "Error al contar traducciones: ${e.message}",
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

                if (e is FirebaseFunctionsException) {
                    Log.e("EXPORTAR_DATOS", "Código: ${e.code}", e)
                    Log.e("EXPORTAR_DATOS", "Detalles: ${e.details}")
                    Log.e("EXPORTAR_DATOS", "Mensaje: ${e.message}")

                    Toast.makeText(
                        this,
                        "Error al exportar datos: ${e.code} - ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Log.e("EXPORTAR_DATOS", "Error desconocido", e)

                    Toast.makeText(
                        this,
                        "Error al exportar datos: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
            }
    }}

    private fun obtenerNombreRol(rol: String): String {
        return when (rol) {
            "reader" -> "Lector"
            "translator" -> "Traductor"
            "proofreader" -> "Corrector"
            "admin" -> "Administrador"
            else -> rol
        }
    }

    /*private fun exportarDatosUsuario() {

        val user = auth.currentUser

        Log.d("EXPORTAR_DATOS", "UID: ${user?.uid}")
        Log.d("EXPORTAR_DATOS", "EMAIL: ${user?.email}")

        if (user == null) {
            Toast.makeText(this, "Debes iniciar sesión", Toast.LENGTH_LONG).show()
            return
        }

        Toast.makeText(this, "Generando exportación...", Toast.LENGTH_SHORT).show()

        val functions = Firebase.functions("europe-west1")

        functions
            .getHttpsCallable("exportar_datos_usuario")
            .call()
            .addOnSuccessListener { result ->

                Log.d("EXPORTAR_DATOS", "Respuesta completa: ${result.data}")

                val datos = result.data as? Map<*, *>
                val urlDescarga = datos?.get("urlDescarga")?.toString() ?: ""

                if (urlDescarga.isNotBlank()) {
                    Toast.makeText(this, "Exportación generada correctamente", Toast.LENGTH_LONG).show()

                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlDescarga))
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "No se pudo obtener la descarga", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->

                Log.e("EXPORTAR_DATOS", "Error completo al llamar la función", e)

                if (e is com.google.firebase.functions.FirebaseFunctionsException) {
                    Log.e("EXPORTAR_DATOS", "Código: ${e.code}")
                    Log.e("EXPORTAR_DATOS", "Detalles: ${e.details}")
                    Log.e("EXPORTAR_DATOS", "Mensaje: ${e.message}")

                    Toast.makeText(
                        this,
                        "Error: ${e.code} - ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        this,
                        "Error al exportar datos: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }*/
    private fun exportarDatosUsuario() {

        Toast.makeText(this, "Generando exportación...", Toast.LENGTH_SHORT).show()

        val functions = Firebase.functions("us-central1")

        Log.d("EXPORTAR_DATOS", "Llamando a exportar_datos_usuario")
        Log.d("EXPORTAR_DATOS", "UID actual: ${auth.currentUser?.uid}")
        Log.d("EXPORTAR_DATOS", "Email actual: ${auth.currentUser?.email}")

        var respondio = false

        Handler(Looper.getMainLooper()).postDelayed({
            if (!respondio) {
                Log.e("EXPORTAR_DATOS", "La función no respondió después de 20 segundos")
                Toast.makeText(
                    this,
                    "La exportación está tardando demasiado",
                    Toast.LENGTH_LONG
                ).show()
            }
        }, 20000)

        functions
            .getHttpsCallable("exportar_datos_usuario")
            .call()
            .addOnSuccessListener { result ->

                respondio = true

                Log.d("EXPORTAR_DATOS", "Respuesta completa: ${result.data}")

                val datos = result.data as? Map<*, *>
                val urlDescarga = datos?.get("urlDescarga")?.toString() ?: ""

                if (urlDescarga.isNotBlank()) {
                    Toast.makeText(this, "Exportación generada correctamente", Toast.LENGTH_LONG).show()
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(urlDescarga)))
                } else {
                    Toast.makeText(this, "No se pudo obtener la descarga", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->

                respondio = true

                Log.e("EXPORTAR_DATOS", "Error completo al exportar", e)

                Toast.makeText(
                    this,
                    "Error al exportar datos: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}