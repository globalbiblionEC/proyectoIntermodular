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
import android.text.Editable
import android.text.TextWatcher
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.storage.FirebaseStorage
import android.view.View
import com.bumptech.glide.Glide


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
    private lateinit var btnUploadCertificate: MaterialButton
    private lateinit var btnRegistrar: MaterialButton
    private lateinit var storage: FirebaseStorage
    private var certificateUri: Uri? = null
    private var certificatePath: String = ""
    private var btnVolver: ImageButton? = null
    private lateinit var ivUsuario: ImageView
    private lateinit var btnUploadProfileImage: MaterialButton
    private var profileImageUri: Uri? = null
    private var profileImagePath: String = ""
    private var profileImageUrl: String = ""

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

    private val seleccionarImagenLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                profileImageUri = uri

                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                //ivUsuario.setImageURI(uri)
                Glide.with(this)
                    .load(uri)
                    .circleCrop()
                    .into(ivUsuario)

                Toast.makeText(this, "Imagen seleccionada", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No seleccionaste ninguna imagen", Toast.LENGTH_SHORT).show()
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
        btnUploadProfileImage = findViewById(R.id.btnUploadProfileImage)
        ivUsuario = findViewById(R.id.ivUsuario)

        configurarSpinners()
        configurarFormatoFecha()

        btnUploadCertificate.setOnClickListener {
            seleccionarPdfLauncher.launch(arrayOf("application/pdf"))
        }
        btnUploadProfileImage.setOnClickListener {
            seleccionarImagenLauncher.launch(arrayOf("image/*"))
        }
        btnRegistrar = findViewById(R.id.btnRegistrar)
        btnVolver = findViewById(R.id.btnVolver)

        // Flecha volver
        btnVolver?.setOnClickListener {
            finish()
        }

        btnUploadProfileImage.setOnClickListener {
            seleccionarImagenLauncher.launch(arrayOf("image/*"))
        }

        // Botón REGISTRARSE (Firebase Auth + Firestore)
        btnRegistrar.setOnClickListener {
            registrarUsuario()
        }

    }

    private fun configurarSpinners() {
        val roles = arrayOf("reader", "translator", "proofreader")

        val adapterRoles = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            roles
        )

        adapterRoles.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRole.adapter = adapterRoles

        val idiomas = arrayOf("Spanish", "English", "French", "Portuguese")

        val adapterIdiomas = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            idiomas
        )

        adapterIdiomas.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerNativeLanguage.adapter = adapterIdiomas

        btnUploadCertificate.visibility = View.GONE

        spinnerRole.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                val rolSeleccionado = parent.getItemAtPosition(position).toString()

                if (rolSeleccionado == "translator" || rolSeleccionado == "proofreader") {
                    btnUploadCertificate.visibility = View.VISIBLE
                } else {
                    btnUploadCertificate.visibility = View.GONE
                    certificateUri = null
                    certificatePath = ""
                    btnUploadCertificate.text = "Certificado"
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun configurarFormatoFecha() {
        etFechaNacimiento.addTextChangedListener(object : TextWatcher {

            private var cambiandoTexto = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (cambiandoTexto) return

                val numeros = s.toString()
                    .replace("/", "")
                    .take(8)

                val fechaFormateada = StringBuilder()

                for (i in numeros.indices) {
                    fechaFormateada.append(numeros[i])

                    if ((i == 1 || i == 3) && i != numeros.lastIndex) {
                        fechaFormateada.append("/")
                    }
                }

                cambiandoTexto = true
                etFechaNacimiento.setText(fechaFormateada.toString())
                etFechaNacimiento.setSelection(fechaFormateada.length)
                cambiandoTexto = false
            }
        })
    }

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

        if ((rol == "translator" || rol == "proofreader") && certificateUri == null) {
            Toast.makeText(this, "Debes subir un certificado PDF", Toast.LENGTH_LONG).show()
            return
        }

        btnRegistrar.isEnabled = false

        auth.createUserWithEmailAndPassword(correo, contrasenia1)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Toast.makeText(
                        this,
                        "Error al registrarse: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    btnRegistrar.isEnabled = true
                    return@addOnCompleteListener
                }

                val uid = auth.currentUser?.uid

                if (uid == null) {
                    Toast.makeText(this, "Error: no se pudo obtener el UID", Toast.LENGTH_LONG).show()
                    btnRegistrar.isEnabled = true
                    return@addOnCompleteListener
                }

                fun guardarUsuarioEnFirestore() {
                    val estadoVerificacion = if (rol == "translator" || rol == "proofreader") {
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
                        "nativeLanguage" to idiomaNativo,

                        "roleCertificatePath" to certificatePath,
                        "certificateUrl" to "",

                        "profileImagePath" to profileImagePath,
                        "profileImageUrl" to profileImageUrl,

                        "roleVerificationStatus" to estadoVerificacion,
                        "certificateValidation" to null,
                        "createdAt" to FieldValue.serverTimestamp()
                    )

                    db.collection("users")
                        .document(uid)
                        .set(datosUsuario)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Registro correcto. ¡Bienvenido/a!", Toast.LENGTH_SHORT).show()
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

               fun subirImagenPerfil() {
                    val uriImagen = profileImageUri

                    if (uriImagen == null) {
                        guardarUsuarioEnFirestore()
                        return
                    }

                    profileImagePath = "users/$uid/profile/profile.jpg"
                    val refImagen = storage.reference.child(profileImagePath)

                    refImagen.putFile(uriImagen)
                        .addOnSuccessListener {
                            refImagen.downloadUrl
                                .addOnSuccessListener { downloadUri ->
                                    profileImageUrl = downloadUri.toString()
                                    guardarUsuarioEnFirestore()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(
                                        this,
                                        "Error obteniendo URL de imagen: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    btnRegistrar.isEnabled = true
                                }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                this,
                                "Error al subir imagen: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                            btnRegistrar.isEnabled = true
                        }
                }

                val uriImagen = profileImageUri
                val uriCertificado = certificateUri

// ---------- SUBIR IMAGEN PERFIL ----------
                if (uriImagen != null) {

                    profileImagePath = "users/$uid/profile/profile.jpg"

                    val storageRef = storage.reference.child(profileImagePath)

                    storageRef.putFile(uriImagen)

                        .addOnSuccessListener {

                            storageRef.downloadUrl
                                .addOnSuccessListener { imageUrl ->

                                    profileImageUrl = imageUrl.toString()

                                    // ---------- SUBIR CERTIFICADO SI EXISTE ----------

                                    if (uriCertificado != null) {

                                        certificatePath =
                                            "users/$uid/certificates/certificado.pdf"

                                        storage.reference.child(certificatePath)
                                            .putFile(uriCertificado)

                                            .addOnSuccessListener {
                                                guardarUsuarioEnFirestore()
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

                                        guardarUsuarioEnFirestore()
                                    }
                                }
                        }

                        .addOnFailureListener { e ->

                            Toast.makeText(
                                this,
                                "Error al subir imagen: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()

                            btnRegistrar.isEnabled = true
                        }

                } else {

                    if (uriCertificado != null) {

                        certificatePath =
                            "users/$uid/certificates/certificado.pdf"

                        storage.reference.child(certificatePath)
                            .putFile(uriCertificado)

                            .addOnSuccessListener {

                                //guardarUsuarioEnFirestore("")
                                guardarUsuarioEnFirestore()
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
                        guardarUsuarioEnFirestore()
                    }
                }

            }
    }
}