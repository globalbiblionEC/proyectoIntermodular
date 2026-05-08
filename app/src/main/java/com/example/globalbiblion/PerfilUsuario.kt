package com.example.globalbiblion

import android.os.Bundle
import android.view.View //Para poder mostrar los campos según el rol
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil_usuario)
        configurarBottomBar()

        // Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

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

        btnVolver = findViewById(R.id.btnVolver)

        cardTraducciones = findViewById(R.id.cardTraducciones)
        cardSolicitudes = findViewById(R.id.cardSolicitudes)


        // --- Botón volver (solo cerrar Activity) ---
        btnVolver.setOnClickListener {
            finish()
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

                        tvEstadoVerificacion.text =
                            if (estadoVerificacion.isNotEmpty()) {
                                "Estado de verificación: $estadoVerificacion"
                            } else {
                                "Estado de verificación"
                            }
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

        // Reseñas: books/{idLibro}/reviews/{uid}
        db.collectionGroup("reviews")
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { documentos ->
                val totalResenas = documentos.size()
                tvNumeroResenas.text = totalResenas.toString()
            }
            .addOnFailureListener {
                tvNumeroResenas.text = "0"
            }

        db.collection("translationRequests")
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { documentos ->
                tvNumeroSolicitudes.text = documentos.size().toString()
            }
            .addOnFailureListener {
                tvNumeroSolicitudes.text = "0"
            }

        db.collection("translations")
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { documentos ->
                tvNumeroTraducciones.text = documentos.size().toString()
            }
            .addOnFailureListener {
                tvNumeroTraducciones.text = "0"
            }
}
}