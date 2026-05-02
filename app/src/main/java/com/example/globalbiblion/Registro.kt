package com.example.globalbiblion

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.Patterns
import android.widget.*
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.storage.FirebaseStorage


class Registro : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore //db por database

    // Campos de texto
    private lateinit var etNombre: EditText
    private lateinit var etApellidos: EditText
    private lateinit var etFechaNacimiento: EditText
    private lateinit var etLocalidad: EditText
    private lateinit var etMunicipio: EditText
    private lateinit var etPais: EditText
    private lateinit var etCorreo: EditText
    private lateinit var etRepetirCorreo: EditText
    private lateinit var etContrasenia: EditText
    private lateinit var etRepetirContrasenia: EditText
    private lateinit var spinnerRole: Spinner
    private lateinit var spinnerNativeLanguage: Spinner

    // Botones
    private lateinit var btnUploadCertificate: MaterialButton
    private lateinit var btnRegistrar: MaterialButton
    private lateinit var storage: FirebaseStorage
    private var certificateUri: Uri? = null
    private var certificatePath: String = ""
    private var btnVolver: ImageButton? = null

    //Para seleccionar el PDF
    private val seleccionarPdfLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                certificateUri = uri
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                Toast.makeText(this, "Certificado PDF seleccionado", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No seleccionaste ningún PDF", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        //Variables Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()


        // Find views by Ids
        etNombre = findViewById(R.id.etNombre)
        etApellidos = findViewById(R.id.etApellidos)
        etFechaNacimiento = findViewById(R.id.etFechaNacimiento)
        etLocalidad = findViewById(R.id.etLocalidad)
        etMunicipio = findViewById(R.id.etMunicipio)
        etPais = findViewById(R.id.etPais)
        etCorreo = findViewById(R.id.etCorreo)
        etRepetirCorreo = findViewById(R.id.etRepetirCorreo)
        etContrasenia = findViewById(R.id.etContrasenia)
        etRepetirContrasenia = findViewById(R.id.etRepetirContrasenia)
        spinnerRole = findViewById(R.id.spinnerRole)
        spinnerNativeLanguage = findViewById(R.id.spinnerNativeLanguage)
        btnUploadCertificate = findViewById(R.id.btn_UploadCertificate)

        val roles = listOf("reader", "translator", "editor")
        spinnerRole.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, roles)

        val idiomas = listOf("Spanish", "English", "French", "Portuguese")
        spinnerNativeLanguage.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, idiomas)

        btnUploadCertificate.setOnClickListener {
            seleccionarPdfLauncher.launch(arrayOf("application/pdf"))
        }
        btnRegistrar = findViewById(R.id.btnRegistrar)
        btnVolver = findViewById(R.id.btnVolver)

        // Flecha volver
        btnVolver?.setOnClickListener {
            finish()
        }

        // Botón REGISTRARSE (Firebase Auth + Firestore)
        btnRegistrar.setOnClickListener {
            registrarUsuario()
        }

    }
    //Funcion para registrar el usuario (aca hacemos validaciones tambien)
    private fun registrarUsuario() {
        val nombre = etNombre.text.toString().trim()
        val apellidos = etApellidos.text.toString().trim()
        val fechaNac = etFechaNacimiento.text.toString().trim()
        val localidad = etLocalidad.text.toString().trim()
        val municipio = etMunicipio.text.toString().trim()
        val pais = etPais.text.toString().trim()
        val correo = etCorreo.text.toString().trim()
        val correo2 = etRepetirCorreo.text.toString().trim()
        val contrasenia1 = etContrasenia.text.toString().trim()
        val contrasenia2 = etRepetirContrasenia.text.toString().trim()
        val rol = spinnerRole.selectedItem.toString()
        val idiomaNativo = spinnerNativeLanguage.selectedItem.toString()

        if (nombre.isEmpty()) {
            etNombre.error = "Introduce tu nombre"
            etNombre.requestFocus()
            return
        }

        if (correo.isEmpty()) {
            etCorreo.error = "Introduce tu correo"
            etCorreo.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            etCorreo.error = "Correo no válido"
            etCorreo.requestFocus()
            return
        }

        if (correo != correo2) {
            etRepetirCorreo.error = "Los correos no coinciden"
            etRepetirCorreo.requestFocus()
            return
        }

        if (contrasenia1.length < 6) {
            etContrasenia.error = "Por favor lector, mínimo 6 caracteres"
            etContrasenia.requestFocus()
            return
        }

        if (contrasenia1 != contrasenia2) {
            etRepetirContrasenia.error = "Las contraseñas no coinciden"
            etRepetirContrasenia.requestFocus()
            return
        }

        btnRegistrar.isEnabled = false

        auth.createUserWithEmailAndPassword(correo, contrasenia1)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid

                    if (uid == null) {
                        Toast.makeText(
                            this,
                            "Error: no se pudo obtener el UID del usuario",
                            Toast.LENGTH_LONG
                        ).show()
                        btnRegistrar.isEnabled = true
                        return@addOnCompleteListener
                    }

                    val guardarUsuarioEnFirestore: (String?) -> Unit = { certificadoUrl ->

                        //We add this for the validation part
                        val estadoVerificacion = if (
                            rol == "translator" || rol == "editor"
                        ) {
                            "pending_review"
                        } else {
                            "not_required"
                        }
                        val datosUsuario = hashMapOf(
                            "nombre" to nombre,
                            "apellidos" to apellidos,
                            "fechaNacimiento" to fechaNac,
                            "localidad" to localidad,
                            "municipio" to municipio,
                            "pais" to pais,
                            "email" to correo,
                            "rol" to rol,

                            "roleCertificatePath" to certificatePath,
                            "certificateUrl" to (certificadoUrl ?: ""),

                            "roleVerificationStatus" to estadoVerificacion,
                            "certificateValidation" to null,

                            "nativeLanguage" to idiomaNativo,
                            "createdAt" to FieldValue.serverTimestamp()
                        )

                        db.collection("users")
                            .document(uid)
                            .set(datosUsuario)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    this,
                                    "Registro correcto. ¡Bienvenido/a!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    this,
                                    "Usuario creado, pero error al guardar datos: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                                btnRegistrar.isEnabled = true
                            }
                    }

                    val uriCertificado = certificateUri

                    if (uriCertificado != null) {
                        certificatePath = "users/$uid/certificates/certificado.pdf"

                        storage.reference.child(certificatePath)
                            .putFile(uriCertificado)
                            .addOnSuccessListener {
                                storage.reference.child(certificatePath)
                                    .downloadUrl
                                    .addOnSuccessListener { downloadUri ->
                                        guardarUsuarioEnFirestore(downloadUri.toString())
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(
                                            this,
                                            "Error al obtener URL del certificado: ${e.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        btnRegistrar.isEnabled = true
                                    }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    this,
                                    "Error al subir certificado: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                                btnRegistrar.isEnabled = true
                            }
                    } else {
                        guardarUsuarioEnFirestore(null)
                    }

                } else {
                    Toast.makeText(
                        this,
                        "Error al registrarse: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    btnRegistrar.isEnabled = true
                }
            }
    }
}