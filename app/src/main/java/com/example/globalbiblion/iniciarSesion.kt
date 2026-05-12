package com.example.globalbiblion

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class iniciarSesion : AppCompatActivity() {

    // Firebase Auth
    private lateinit var auth: FirebaseAuth //Para la autenticación del usuario

    // Elementos que ve el usuario
    private lateinit var etCorreo: EditText
    private lateinit var etContrasenia: EditText
    private lateinit var btnIniciarSesion: MaterialButton //Usamos material button para hacerlo más personalizable
    private  lateinit var btnRegistrarse: Button
    private var btnVolver: ImageButton? = null //la variable puede ser null
    private var btnIdiomas: LinearLayout? = null
    private lateinit var db: FirebaseFirestore


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_iniciar_sesion)

        // Inicializar Firebase Auth
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        //Find views by id
        etCorreo = findViewById(R.id.etCorreo)
        etContrasenia = findViewById(R.id.etContrasenia)
        btnIniciarSesion = findViewById(R.id.btnIniciarSesion)
        btnVolver = findViewById(R.id.btnVolver)
        btnIdiomas= findViewById(R.id.btnIdiomas)
        btnRegistrarse=findViewById(R.id.btnRegistrarse)

        // Al presionar iniciar sesió  enviaremos todo a Firebase Auth
        btnIniciarSesion.setOnClickListener {
            val correo = etCorreo.text.toString().trim()//quitamos los espacios en blanco que puedan haber
            val contrasenia = etContrasenia.text.toString().trim()

            // Verificamos que los campos no estén vacíos
            if (correo.isEmpty()) {
                etCorreo.error = "Introduce tu correo"
                etCorreo.requestFocus()//Señalamos el campo que falta por rellenar
                return@setOnClickListener
            }

            if (contrasenia.isEmpty()) {
                etContrasenia.error = "Introduce tu contraseña"
                etContrasenia.requestFocus()
                return@setOnClickListener
            }

            //Hacemos el inicio de sesión con Firebase Auth
            auth.signInWithEmailAndPassword(correo, contrasenia)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {//Si está todo bien, realizamos el if
                        Toast.makeText(
                            this,
                            "Inicio de sesión correcto",
                            Toast.LENGTH_SHORT
                        ).show()
                        comprobarRolUsuario()
                    } else {
                        // Error de autenticación
                        Toast.makeText(
                            this,
                            "Error al iniciar sesión: ",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }

        btnRegistrarse.setOnClickListener {
            val intent=Intent(this, Registro::class.java)
            startActivity(intent)
        }

        btnVolver?.setOnClickListener {
            finish() // vuelve a la pantalla anterior (MainActivity)
        }

        // Botón idiomas abajo
        btnIdiomas?.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_LOCALE_SETTINGS)
                startActivity(intent)
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }
        }
    }

    private fun comprobarRolUsuario() {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            Toast.makeText(this, "No se pudo identificar al usuario", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { documento ->

                val rol = documento.getString("rol")

                if (rol == "admin") {
                    val intent = Intent(this, PanelAdministrador::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    val intent = Intent(this, Menuprincipal::class.java)
                    intent.putExtra("uid", uid)
                    intent.putExtra("isGuest", false)
                    startActivity(intent)
                    finish()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al comprobar el rol", Toast.LENGTH_SHORT).show()
            }
    }
}
