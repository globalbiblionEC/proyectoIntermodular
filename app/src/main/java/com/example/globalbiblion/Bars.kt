package com.example.globalbiblion
import android.content.Intent
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import android.widget.TextView
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide

//Activity base for reuse
open class Bars : AppCompatActivity() {

    //This function must be in every child
    protected fun configurarBottomBar() {

        val llMenuPrincipal = findViewById<LinearLayout?>(R.id.llBotonMenuPrincipal)
        val llCatalogo = findViewById<LinearLayout?>(R.id.llCatalogo)
        val llContinuarLeyendo = findViewById<LinearLayout?>(R.id.llContileyendo)
        val llMicrofono = findViewById<LinearLayout?>(R.id.llMicrofono)
        val llNotificaciones = findViewById<LinearLayout?>(R.id.llNotificaciones)
        val tvBadgeNotificaciones = findViewById<TextView?>(R.id.tvBadgeNotificaciones)
        val flNotificaciones = findViewById<FrameLayout?>(R.id.flNotificaciones)

        llMenuPrincipal?.setOnClickListener {
            if (this !is Menuprincipal) {
                startActivity(Intent(this, Menuprincipal::class.java))
            }else{
                Toast.makeText(this, "Querido lector, ya estás en la biblioteca", Toast.LENGTH_SHORT).show()
            }
        }

        llCatalogo?.setOnClickListener {
            if (this !is Biblioteca) {
                startActivity(Intent(this, Biblioteca::class.java))
            }
        }

        llContinuarLeyendo?.setOnClickListener {
            abrirUltimaLectura()
        }

        llMicrofono?.setOnClickListener {
            if (this !is BuscarPorVoz) {
                startActivity(Intent(this, BuscarPorVoz::class.java))
            } else {
                Toast.makeText(this, "Ya estás en búsqueda por voz", Toast.LENGTH_SHORT).show()
            }        }

        llNotificaciones?.setOnClickListener {
            if (this !is Notificaciones) {
                startActivity(Intent(this, Notificaciones::class.java))
            } else {
                Toast.makeText(this, "Ya estás en notificaciones", Toast.LENGTH_SHORT).show()
            }
        }

        controlarBotonNotificaciones(flNotificaciones,llNotificaciones,tvBadgeNotificaciones)

    }

    protected fun configurarTopBar() {
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()
        val storage = FirebaseStorage.getInstance()

        val ivPerfil = findViewById<ImageView?>(R.id.ivPerfilTopBar)
        val tvNombreUsuario = findViewById<TextView?>(R.id.tvNombreUsuarioTopBar)
        val btnCerrarSesion = findViewById<ImageButton?>(R.id.btnCerrarSesionTopBar)
        val etBuscarLibro = findViewById<EditText?>(R.id.etBuscarLibroTopBar)
        val ivLupa = findViewById<ImageView?>(R.id.ivLupaTopBar)

        val uid = auth.currentUser?.uid

        if (uid != null) {
            db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener { doc ->
                    val nombre = doc.getString("nombre") ?: ""
                    val apellidos = doc.getString("apellidos") ?: ""

                    tvNombreUsuario?.text = when {
                        nombre.isNotBlank() && apellidos.isNotBlank() -> "$nombre $apellidos"
                        nombre.isNotBlank() -> nombre
                        else -> "Usuario"
                    }

                    val profileImageUrl = doc.getString("profileImageUrl") ?: ""
                    val profileImagePath = doc.getString("profileImagePath") ?: ""

                    if (ivPerfil != null) {
                        when {
                            profileImageUrl.isNotBlank() -> {
                                Glide.with(this)
                                    .load(profileImageUrl)
                                    .circleCrop()
                                    .placeholder(R.drawable.usuarioleyendocf)
                                    .error(R.drawable.usuarioleyendocf)
                                    .into(ivPerfil)
                            }

                            profileImagePath.isNotBlank() -> {
                                storage.reference.child(profileImagePath).downloadUrl
                                    .addOnSuccessListener { uri ->
                                        Glide.with(this)
                                            .load(uri)
                                            .circleCrop()
                                            .placeholder(R.drawable.usuarioleyendocf)
                                            .error(R.drawable.usuarioleyendocf)
                                            .into(ivPerfil)
                                    }
                                    .addOnFailureListener {
                                        ivPerfil.setImageResource(R.drawable.usuarioleyendocf)
                                    }
                            }

                            else -> {
                                ivPerfil.setImageResource(R.drawable.usuarioleyendocf)
                            }
                        }
                    }
                }
                .addOnFailureListener {
                    tvNombreUsuario?.text = "Usuario"
                }
        } else {
            tvNombreUsuario?.text = "Invitado"
            ivPerfil?.setImageResource(R.drawable.usuarioleyendocf)
        }

        ivPerfil?.setOnClickListener {
            if (this !is PerfilUsuario) {
                startActivity(Intent(this, PerfilUsuario::class.java))
            } else {
                Toast.makeText(this, "Ya estás en tu perfil", Toast.LENGTH_SHORT).show()
            }
        }

        btnCerrarSesion?.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Cerrar sesión")
                .setMessage("¿Seguro que deseas cerrar sesión?")
                .setPositiveButton("Sí") { _, _ ->
                    auth.signOut()

                    val intent = Intent(this, MainActivity::class.java)
                    intent.addFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TASK
                    )

                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        ivLupa?.setOnClickListener {
            val texto = etBuscarLibro?.text.toString().trim()

            if (texto.length < 4) {
                Toast.makeText(this, "Escribe al menos 4 caracteres", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            buscarLibroDesdeTopBar(texto)
        }

        etBuscarLibro?.setOnEditorActionListener { _, _, _ ->
            val texto = etBuscarLibro.text.toString().trim()

            if (texto.length < 4) {
                Toast.makeText(this, "Escribe al menos 4 caracteres", Toast.LENGTH_SHORT).show()
                return@setOnEditorActionListener true
            }

            buscarLibroDesdeTopBar(texto)
            true
        }
    }
    /*protected fun configurarTopBar() {
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()

        val ivPerfil = findViewById<ImageView?>(R.id.ivPerfilTopBar)
        val tvNombreUsuario = findViewById<TextView?>(R.id.tvNombreUsuarioTopBar)
        val btnCerrarSesion = findViewById<ImageButton?>(R.id.btnCerrarSesionTopBar)
        val etBuscarLibro = findViewById<EditText?>(R.id.etBuscarLibroTopBar)
        val ivLupa = findViewById<ImageView?>(R.id.ivLupaTopBar)

        val uid = auth.currentUser?.uid

        if (uid != null) {
            db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener { doc ->
                    val nombre = doc.getString("nombre") ?: ""
                    val apellidos = doc.getString("apellidos") ?: ""

                    tvNombreUsuario?.text = when {
                        nombre.isNotBlank() && apellidos.isNotBlank() -> "$nombre $apellidos"
                        nombre.isNotBlank() -> nombre
                        else -> "Usuario"
                    }
                }
                .addOnFailureListener {
                    tvNombreUsuario?.text = "Usuario"
                }
        } else {
            tvNombreUsuario?.text = "Invitado"
        }

        ivPerfil?.setOnClickListener {
            if (this !is PerfilUsuario) {
                startActivity(Intent(this, PerfilUsuario::class.java))
            } else {
                Toast.makeText(this, "Ya estás en tu perfil", Toast.LENGTH_SHORT).show()
            }
        }

        btnCerrarSesion?.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Cerrar sesión")
                .setMessage("¿Seguro que deseas cerrar sesión?")
                .setPositiveButton("Sí") { _, _ ->
                    auth.signOut()

                    val intent = Intent(this, MainActivity::class.java)
                    intent.addFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TASK
                    )

                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        ivLupa?.setOnClickListener {
            val texto = etBuscarLibro?.text.toString().trim()

            if (texto.length < 4) {
                Toast.makeText(this, "Escribe al menos 4 caracteres", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            buscarLibroDesdeTopBar(texto)
        }

        etBuscarLibro?.setOnEditorActionListener { _, _, _ ->
            val texto = etBuscarLibro.text.toString().trim()

            if (texto.length < 4) {
                Toast.makeText(this, "Escribe al menos 4 caracteres", Toast.LENGTH_SHORT).show()
                return@setOnEditorActionListener true
            }

            buscarLibroDesdeTopBar(texto)
            true
        }
    }*/

    private fun buscarLibroDesdeTopBar(texto: String) {
        val db = FirebaseFirestore.getInstance()
        val storage = FirebaseStorage.getInstance()
        val consulta = normalizar(texto).take(4)

        db.collection("books")
            .get()
            .addOnSuccessListener { snapshot ->

                val libroEncontrado = snapshot.documents.firstOrNull { doc ->
                    val titulo = doc.getString("title") ?: ""
                    normalizar(titulo).contains(consulta)
                }

                if (libroEncontrado == null) {
                    Toast.makeText(this, "No se ha encontrado el libro con: $consulta", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val idLibro = libroEncontrado.id
                val titulo = libroEncontrado.getString("title") ?: "Sin título"

                val authors = libroEncontrado.get("authors") as? List<*>
                val autor = authors
                    ?.mapNotNull { it as? String }
                    ?.joinToString(", ")
                    ?: "Autor desconocido"

                val pdfPath = libroEncontrado.getString("pdfPath") ?: ""
                val coverPath = libroEncontrado.getString("coverPath") ?: ""

                if (pdfPath.isBlank()) {
                    Toast.makeText(this, "Este libro no tiene PDF", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                storage.reference.child(pdfPath).downloadUrl
                    .addOnSuccessListener { pdfUri ->
                        val intent = Intent(this, LibroSeleccionado::class.java).apply {
                            putExtra("idLibro", idLibro)
                            putExtra("tituloLibro", titulo)
                            putExtra("autorLibro", autor)
                            putExtra("pdfUrl", pdfUri.toString())
                            putExtra("pdfStoragePath", pdfPath)
                            putExtra("portadaStoragePath", coverPath)
                            putExtra("portadaResId", R.drawable.logogbsinfondo)
                        }

                        startActivity(intent)
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error buscando libro: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun normalizar(texto: String): String {
        val sinAcentos = java.text.Normalizer.normalize(
            texto,
            java.text.Normalizer.Form.NFD
        ).replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")

        return sinAcentos.lowercase().replace(" ", "")
    }

    private fun abrirUltimaLectura() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid == null) {
            Toast.makeText(this, "Debes iniciar sesión", Toast.LENGTH_SHORT).show()
            return
        }

        val db = FirebaseFirestore.getInstance()
        val storage = FirebaseStorage.getInstance()

        db.collection("readings")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    Toast.makeText(this, "No tienes ninguna lectura guardada", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val idLibro = doc.getString("idLibro") ?: ""
                val titulo = doc.getString("title") ?: "Libro actual"

                val authors = doc.get("authors") as? List<*>
                val autor = authors
                    ?.mapNotNull { it as? String }
                    ?.joinToString(", ")
                    ?: ""

                /*val pdfUrl = doc.getString("pdfUrl") ?: ""
                val pdfPath = doc.getString("pdfPath") ?: ""
                val coverPath = doc.getString("coverPath") ?: ""
                val readingLanguage = doc.getString("readingLanguage") ?: ""*/

                val pdfUrl = doc.getString("pdfUrl") ?: ""
                val pdfPath = doc.getString("pdfPath") ?: ""

                val audioUrl = doc.getString("audioUrl") ?: ""
                val contentType = doc.getString("contentType") ?: ""
                val audioPosition = doc.getLong("audioPosition")?.toInt() ?: 0

                val coverPath = doc.getString("coverPath") ?: ""
                val readingLanguage = doc.getString("readingLanguage") ?: ""

                /*if (pdfUrl.isNotBlank()) {
                    abrirContinuarLeyendo(
                        idLibro,
                        titulo,
                        autor,
                        pdfUrl,
                        pdfPath,
                        coverPath,
                        readingLanguage
                    )
                    return@addOnSuccessListener
                }*/

                if (contentType == "audio" && audioUrl.isNotBlank()) {

                    abrirContinuarLeyendo(
                        idLibro = idLibro,
                        titulo = titulo,
                        autor = autor,
                        pdfUrl = "",
                        pdfPath = "",
                        audioUrl = audioUrl,
                        contentType = "audio",
                        audioPosition = audioPosition,
                        coverPath = coverPath,
                        readingLanguage = readingLanguage
                    )

                    return@addOnSuccessListener
                }

                if (pdfUrl.isNotBlank()) {

                    abrirContinuarLeyendo(
                        idLibro = idLibro,
                        titulo = titulo,
                        autor = autor,
                        pdfUrl = pdfUrl,
                        pdfPath = pdfPath,
                        audioUrl = "",
                        contentType = "pdf",
                        audioPosition = 0,
                        coverPath = coverPath,
                        readingLanguage = readingLanguage
                    )

                    return@addOnSuccessListener
                }

                if (pdfPath.isBlank()) {
                    Toast.makeText(this, "No se encontró el PDF guardado", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                storage.reference.child(pdfPath).downloadUrl
                    .addOnSuccessListener { pdfUri ->
                        abrirContinuarLeyendo(
                            idLibro = idLibro,
                            titulo = titulo,
                            autor = autor,
                            pdfUrl = pdfUri.toString(),
                            pdfPath = pdfPath,
                            audioUrl = "",
                            contentType = "pdf",
                            audioPosition = 0,
                            coverPath = coverPath,
                            readingLanguage = readingLanguage
                        )
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error PDF: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error Firestore: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun controlarBotonNotificaciones(
        flNotificaciones: FrameLayout?,
        llNotificaciones: LinearLayout?,
        tvBadge: TextView?
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid == null) {
            flNotificaciones?.visibility = android.view.View.GONE
            return
        }

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->

                val rol = doc.getString("rol") ?: ""
                val hayNotificacion = doc.getBoolean("notificationPending") ?: false

                if (rol == "reader") {
                    flNotificaciones?.visibility = android.view.View.GONE
                    tvBadge?.visibility = android.view.View.GONE
                    return@addOnSuccessListener
                }

                flNotificaciones?.visibility = android.view.View.VISIBLE
                llNotificaciones?.visibility = android.view.View.VISIBLE

                tvBadge?.visibility =
                    if (hayNotificacion) android.view.View.VISIBLE
                    else android.view.View.GONE
            }
            .addOnFailureListener {
                flNotificaciones?.visibility = android.view.View.GONE
                tvBadge?.visibility = android.view.View.GONE
            }
    }

    private fun abrirContinuarLeyendo(
        idLibro: String,
        titulo: String,
        autor: String,
        pdfUrl: String,
        pdfPath: String,
        audioUrl: String,
        contentType: String,
        audioPosition: Int,
        coverPath: String,
        readingLanguage: String
    ) {

        val intent = Intent(this, ContinuarLeyendo::class.java).apply {

            putExtra("idLibro", idLibro)
            putExtra("tituloLibro", titulo)
            putExtra("autorLibro", autor)

            putExtra("pdfUrl", pdfUrl)
            putExtra("pdfStoragePath", pdfPath)

            putExtra("audioUrl", audioUrl)
            putExtra("contentType", contentType)
            putExtra("audioPosition", audioPosition)

            putExtra("portadaStoragePath", coverPath)
            putExtra("portadaResId", R.drawable.logogbsinfondo)

            putExtra("readingLanguage", readingLanguage)
        }

        startActivity(intent)
    }
    /*private fun abrirContinuarLeyendo(
        idLibro: String,
        titulo: String,
        autor: String,
        pdfUrl: String,
        pdfPath: String,
        coverPath: String,
        readingLanguage: String
    ) {
        val intent = Intent(this, ContinuarLeyendo::class.java).apply {
            putExtra("idLibro", idLibro)
            putExtra("tituloLibro", titulo)
            putExtra("autorLibro", autor)
            putExtra("pdfUrl", pdfUrl)
            putExtra("pdfStoragePath", pdfPath)
            putExtra("portadaStoragePath", coverPath)
            putExtra("portadaResId", R.drawable.logogbsinfondo)
            putExtra("readingLanguage", readingLanguage)
        }

        startActivity(intent)
    }*/
}