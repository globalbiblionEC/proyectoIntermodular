package com.example.globalbiblion

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent
import android.net.Uri
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class SolicitudTraduccion : BottomBar() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private lateinit var ivPortadaLibro: ImageView
    private lateinit var tvTituloLibro: TextView
    private lateinit var tvIdiomaOriginal: TextView
    private lateinit var spinnerIdiomaDestino: Spinner
    private lateinit var btnSolicitarTraduccion: Button
    private lateinit var btnSolicitarCorreccion: Button
    private lateinit var btnSubirDocumento: Button
    private lateinit var btnVolver: ImageButton

    private var bookId = ""
    private var bookTitle = ""
    private var sourceLanguage = ""
    private var coverPath = ""

    private var requestIdAprobada = ""

    private val seleccionarPdfLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                subirDocumento(uri)
            } else {
                Toast.makeText(this, "No seleccionaste ningún PDF", Toast.LENGTH_SHORT).show()
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_solicitud_traduccion)

        configurarBottomBar()

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        ivPortadaLibro = findViewById(R.id.ivPortadaLibro)
        tvTituloLibro = findViewById(R.id.tvTituloLibro)
        tvIdiomaOriginal = findViewById(R.id.tvIdiomaOriginal)
        spinnerIdiomaDestino = findViewById(R.id.spinnerIdiomaDestino)
        btnSolicitarTraduccion = findViewById(R.id.btnSolicitarTraduccion)
        btnSolicitarCorreccion = findViewById(R.id.btnSolicitarCorreccion)
        btnSubirDocumento = findViewById(R.id.btnSubirDocumento)
        btnVolver = findViewById(R.id.btnVolver)

        bookId = intent.getStringExtra("bookId") ?: ""
        bookTitle = intent.getStringExtra("bookTitle") ?: "Libro"
        sourceLanguage = intent.getStringExtra("sourceLanguage") ?: "Idioma no indicado"

        tvTituloLibro.text = bookTitle
        tvIdiomaOriginal.text = "Idioma original: $sourceLanguage"

        val idiomasDestino = listOf("Spanish", "English", "French", "Portuguese")
        spinnerIdiomaDestino.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            idiomasDestino
        )

        cargarPortadaLibro()

        btnVolver.setOnClickListener {
            finish()
        }

        btnSolicitarTraduccion.setOnClickListener {
            crearSolicitud("translation")
        }

        btnSolicitarCorreccion.setOnClickListener {
            crearSolicitud("correction")
        }

        btnSubirDocumento.setOnClickListener {
            comprobarSolicitudAprobada()
        }
    }

    private fun cargarPortadaLibro() {
        if (bookId.isBlank()) return

        db.collection("books")
            .document(bookId)
            .get()
            .addOnSuccessListener { doc ->
                coverPath = doc.getString("coverPath") ?: ""

                if (coverPath.isNotBlank()) {
                    storage.reference.child(coverPath).downloadUrl
                        .addOnSuccessListener { uri ->
                            Glide.with(this)
                                .load(uri.toString())
                                .placeholder(R.drawable.logogbsinfondo)
                                .error(R.drawable.logogbsinfondo)
                                .into(ivPortadaLibro)
                        }
                } else {
                    ivPortadaLibro.setImageResource(R.drawable.logogbsinfondo)
                }
            }
            .addOnFailureListener {
                ivPortadaLibro.setImageResource(R.drawable.logogbsinfondo)
            }
    }

    private fun crearSolicitud(tipoSolicitud: String) {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            Toast.makeText(this, "Debes iniciar sesión", Toast.LENGTH_SHORT).show()
            return
        }

        if (bookId.isBlank()) {
            Toast.makeText(this, "Error: no se recibió el ID del libro", Toast.LENGTH_LONG).show()
            return
        }

        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { userDoc ->

                if (!userDoc.exists()) {
                    Toast.makeText(this, "No se encontraron datos del usuario", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val rol = userDoc.getString("rol") ?: ""
                val estado = userDoc.getString("roleVerificationStatus") ?: ""

                val rolNecesario = if (tipoSolicitud == "translation") {
                    "translator"
                } else {
                    "proofreader"
                }

                if (rol != rolNecesario) {
                    Toast.makeText(
                        this,
                        "No tienes el rol necesario para esta solicitud",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addOnSuccessListener
                }

                if (estado != "verified") {
                    Toast.makeText(
                        this,
                        "Tu cuenta todavía no está verificada",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addOnSuccessListener
                }

                val nombre = userDoc.getString("nombre") ?: "Usuario"
                val apellidos = userDoc.getString("apellidos") ?: ""
                val userName = "$nombre $apellidos".trim()

                val idiomaDestino = spinnerIdiomaDestino.selectedItem.toString()

                val datosSolicitud = hashMapOf(
                    "bookId" to bookId,
                    "bookTitle" to bookTitle,
                    "userId" to uid,
                    "userName" to userName,
                    "requestType" to tipoSolicitud,
                    "sourceLanguage" to sourceLanguage,
                    "targetLanguage" to idiomaDestino,
                    "status" to "pending",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "adminReviewedBy" to null,
                    "adminReviewedAt" to null,
                    "translationPath" to "",
                    "translationUrl" to "",
                    "message" to ""
                )

                db.collection("contribution_requests")
                    .add(datosSolicitud)
                    .addOnSuccessListener {
                        Toast.makeText(
                            this,
                            "Solicitud enviada. Espera aprobación del admin.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            this,
                            "Error al crear solicitud: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error leyendo usuario: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun comprobarSolicitudAprobada() {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            Toast.makeText(this, "Debes iniciar sesión", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("contribution_requests")
            .whereEqualTo("userId", uid)
            .whereEqualTo("bookId", bookId)
            .whereEqualTo("status", "approved")
            .limit(1)
            .get()
            .addOnSuccessListener { documentos ->
                if (documentos.isEmpty) {
                    Toast.makeText(
                        this,
                        "No tienes una solicitud aprobada para este libro",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addOnSuccessListener
                }

                requestIdAprobada = documentos.documents[0].id
                seleccionarPdfLauncher.launch(arrayOf("application/pdf"))
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Error comprobando solicitud: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun subirDocumento(uriPdf: Uri) {
        if (requestIdAprobada.isBlank()) {
            Toast.makeText(this, "No hay solicitud aprobada", Toast.LENGTH_SHORT).show()
            return
        }

        val rutaStorage = "contribution_uploads/$requestIdAprobada/documento.pdf"

        storage.reference.child(rutaStorage)
            .putFile(uriPdf)
            .addOnSuccessListener {
                storage.reference.child(rutaStorage).downloadUrl
                    .addOnSuccessListener { downloadUri ->

                        val datosActualizados = mapOf(
                            "translationPath" to rutaStorage,
                            "translationUrl" to downloadUri.toString(),
                            "status" to "uploaded",
                            "uploadedAt" to FieldValue.serverTimestamp()
                        )

                        db.collection("contribution_requests")
                            .document(requestIdAprobada)
                            .update(datosActualizados)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    this,
                                    "Documento subido correctamente",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    this,
                                    "Error guardando datos: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Error subiendo PDF: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}