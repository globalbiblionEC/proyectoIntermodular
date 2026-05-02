package com.example.globalbiblion

import android.os.Bundle
import android.content.Intent
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PerfilUsuario : BottomBar() {
    //Variables de Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    //Variables de la interfaz de usuario
    private lateinit var tvNombre: TextView
    private lateinit var tvApellido: TextView
    private lateinit var tvFechaNacimiento: TextView
    private lateinit var tvLocalidad: TextView
    private lateinit var tvMunicipio: TextView
    private lateinit var tvPais: TextView


    // Botón volver
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
        tvApellido = findViewById(R.id.tvApellido)
        tvFechaNacimiento = findViewById(R.id.tvFechaNacimiento)
        tvLocalidad = findViewById(R.id.tvLocalidad)
        tvMunicipio = findViewById(R.id.tvMunicipio)
        tvPais = findViewById(R.id.tvPais)

       // tvNumeroPuntuaciones = findViewById(R.id.tvNumeroPuntuaciones)

        btnVolver = findViewById(R.id.btnVolver)

        // --- Botón volver (solo cerrar Activity) ---
        btnVolver.setOnClickListener {
            finish()
        }

        // Cargar datos desde Firebase
        cargarDatosUsuario()
        //cargarNumeroPuntuaciones()
    }

    // ------------------ INFO PERSONAL ------------------

    private fun cargarDatosUsuario() {
        val uid = auth.currentUser?.uid //Obtenemos la uid del usuario actual
        if (uid == null) {
            Toast.makeText(this, "Debes iniciar sesión para ver tu perfil", Toast.LENGTH_LONG).show()
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
                    val apellidos = doc.getString("apellidos") ?: ""
                    val fechaNac = doc.getString("fechaNacimiento") ?: ""
                    val localidad = doc.getString("localidad") ?: ""
                    val municipio = doc.getString("municipio") ?: ""
                    val pais = doc.getString("pais") ?: ""

                    //asignamos los nombres, sino, un valor por defecto
                    tvNombre.text = if (nombre.isNotEmpty()) nombre else "Nombre"
                    tvApellido.text = if (apellidos.isNotEmpty()) apellidos else "Apellido"

                    tvFechaNacimiento.text =
                        if (fechaNac.isNotEmpty()) "Fecha de nacimiento : $fechaNac"
                        else "Fecha de nacimiento :"

                    tvLocalidad.text = if (localidad.isNotEmpty()) localidad else "Localidad"
                    tvMunicipio.text = if (municipio.isNotEmpty()) municipio else "Municipio"
                    tvPais.text = if (pais.isNotEmpty()) pais else "País"
                } else {
                    Toast.makeText(this, "No se encontraron datos de usuario", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al cargar usuario: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

}