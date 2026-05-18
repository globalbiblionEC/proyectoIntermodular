package com.example.globalbiblion
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue

//Esta Activity es para ver los detalles del libro que selecciione el usuario
class LibroSeleccionado : Bars() {
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var ivPortada: ImageView
    private lateinit var tvTitulo: TextView
    private lateinit var tvAutor: TextView
    private lateinit var tvSinopsis: TextView
    private lateinit var tvGenero: TextView
    private lateinit var tvPublicado: TextView
    private lateinit var tvIdiomasLibro: TextView
    private lateinit var tvExtension: TextView
    private lateinit var tvFechaActualizacion: TextView
    private lateinit var btnEscribirResena: Button
    private lateinit var btnLeerLibro: Button
    private lateinit var btnEscucharAudiolibro: Button
    private lateinit var btnTraducir: Button
    private lateinit var llReviewsContainer: LinearLayout //Para mostrar las reseñas de manera dinámica
    private lateinit var btnMostrarIdiomas: Button
    private lateinit var llIdiomasDisponibles: LinearLayout
    private var audioUrl = ""
    private var idLibro = ""
    private var tituloLibro = ""
    private var autorLibro = ""
    private var pdfUrl = ""
    private var pdfStoragePath = ""
    private var portadaStoragePath = ""
    private var portadaResId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_libro_seleccionado)

        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        ivPortada = findViewById(R.id.ivPortadaLibroSeleccionado)
        tvTitulo = findViewById(R.id.tvTituloLibroSeleccionado)
        tvAutor = findViewById(R.id.tvAutorLibroSeleccionado)
        tvSinopsis = findViewById(R.id.tvSinopsis)
        tvGenero = findViewById(R.id.tvGenero)
        tvPublicado = findViewById(R.id.tvPublicado)
        tvIdiomasLibro = findViewById(R.id.tvIdiomasLibro)
        tvExtension = findViewById(R.id.tvExtension)
        tvFechaActualizacion = findViewById(R.id.tvFechaActualizacion)
        btnLeerLibro = findViewById(R.id.btnLeerLibro)
        btnEscucharAudiolibro = findViewById(R.id.btnEscuchar)
        btnEscribirResena = findViewById(R.id.btnEscribirResena)
        btnTraducir = findViewById(R.id.btnTraducir)
        llReviewsContainer = findViewById(R.id.llReviewsContainer)
        btnMostrarIdiomas = findViewById(R.id.btnMostrarIdiomas)
        llIdiomasDisponibles = findViewById(R.id.llIdiomasDisponibles)

        idLibro = intent.getStringExtra("idLibro") ?: ""
        tituloLibro = intent.getStringExtra("tituloLibro") ?: "Título"
        autorLibro = intent.getStringExtra("autorLibro") ?: "Autor"
        pdfUrl = intent.getStringExtra("pdfUrl") ?: ""
        pdfStoragePath = intent.getStringExtra("pdfStoragePath") ?: ""
        portadaStoragePath = intent.getStringExtra("portadaStoragePath") ?: ""
        portadaResId = intent.getIntExtra("portadaResId", R.drawable.logogbsinfondo)

        tvTitulo.text = tituloLibro
        tvAutor.text = autorLibro

        cargarPortada()
        cargarInfoDesdeFirebase()
        configurarTopBar()
        configurarBottomBar()

        btnMostrarIdiomas.setOnClickListener {
            if (llIdiomasDisponibles.visibility == android.view.View.VISIBLE) {
                llIdiomasDisponibles.visibility = android.view.View.GONE
                btnMostrarIdiomas.text =getString(R.string.btn_ver_idiomas)
            } else {
                llIdiomasDisponibles.visibility = android.view.View.VISIBLE
                btnMostrarIdiomas.text =getString(R.string.btn_ocultar_idiomas)
            }
        }

        ivPortada.setOnClickListener {
            abrirPdf()
        }

        btnLeerLibro.setOnClickListener {
            guardarComoContinuarLeyendoYAbrirPdf()
        }
        btnEscucharAudiolibro.setOnClickListener {
            irAContinuarLeyendoAudio()
        }

        btnEscribirResena.setOnClickListener {
            val intent = Intent(this, EscribirResenia::class.java).apply {
                putExtra("idLibro", idLibro)
                putExtra("tituloLibro", tituloLibro)
                putExtra("autorLibro", autorLibro)
                putExtra("pdfUrl", pdfUrl)
                putExtra("pdfStoragePath", pdfStoragePath)
                putExtra("portadaStoragePath", portadaStoragePath)
                putExtra("portadaResId", portadaResId)
            }
            startActivity(intent)
        }

        btnTraducir.setOnClickListener {
            comprobarSiPuedeTraducir()
        }
    }

    private fun cargarPortada() {
        if (portadaStoragePath.isBlank()) {
            ivPortada.setImageResource(portadaResId)
            return
        }

        storage.reference.child(portadaStoragePath).downloadUrl
            .addOnSuccessListener { uri ->
                Glide.with(this)
                    .load(uri.toString())
                    .placeholder(portadaResId)
                    .error(portadaResId)
                    .into(ivPortada)
            }
            .addOnFailureListener {
                ivPortada.setImageResource(portadaResId)
            }
    }

    private fun cargarInfoDesdeFirebase() {
        if (idLibro.isBlank()) return

        db.collection("books")
            .document(idLibro)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    Toast.makeText(this, "No existe el libro con id: $idLibro", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                tituloLibro = doc.getString("title") ?: tituloLibro

                val authors = doc.get("authors") as? List<*>
                autorLibro = authors
                    ?.mapNotNull { it as? String }
                    ?.joinToString(", ")
                    ?.takeIf { it.isNotBlank() }
                    ?: autorLibro

                val descripcion = doc.getString("description") ?: getString(R.string.sinopsis_no_disponible)

                portadaStoragePath = doc.getString("coverPath") ?: portadaStoragePath
                pdfStoragePath = doc.getString("pdfPath") ?: pdfStoragePath

                if (pdfStoragePath.isBlank()) {

                    btnLeerLibro.visibility = android.view.View.GONE

                } else {

                    btnLeerLibro.visibility = android.view.View.VISIBLE
                }

                val averageRating = doc.getDouble("averageRating") ?: 0.0
                val reviewsCount = doc.getLong("reviewsCount") ?: 0L

                val categories = doc.get("categories") as? List<*>
                val categoria = categories
                    ?.mapNotNull { it as? String }
                    ?.joinToString(", ")
                    ?.takeIf { it.isNotBlank() }
                    ?: "-"

                val language = doc.getString("language") ?: "-"

                cargarAudioLibro(idLibro, language)
                cargarIdiomasDisponibles(doc)

                tvTitulo.text = tituloLibro
                tvAutor.text = autorLibro
                tvSinopsis.text = descripcion

                val ratingEntero = averageRating.toInt().coerceIn(0, 5)
                findViewById<TextView>(R.id.tvEstrellas).text =
                    "★".repeat(ratingEntero) + "☆".repeat(5 - ratingEntero)

                tvGenero.text = "GÉNERO\n$categoria"
                tvPublicado.text = "VALORACIÓN\n$averageRating/5"
                tvIdiomasLibro.text = "IDIOMA\n$language"
                tvExtension.text = "RESEÑAS\n$reviewsCount"

                val createdAt = doc.getTimestamp("createdAt")
                tvFechaActualizacion.text = if (createdAt != null) {
                    "Creado\n${createdAt.toDate()}"
                } else {
                    "Valoración media\n$averageRating/5"
                }

                cargarPortada()
                cargarPdfUrlDesdeStorage()
                cargarReviews()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "No se pudo cargar la información del libro: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun abrirPdf() {
        if (pdfUrl.isBlank()) {
            Toast.makeText(this, getString(R.string.toast_pdf_url_no_encontrada), Toast.LENGTH_LONG).show()
            return
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(pdfUrl), "application/pdf")
            addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(intent, "Abrir libro con"))
    }

    private fun guardarComoContinuarLeyendoYAbrirPdf() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid == null || idLibro.isBlank()) {
            abrirPdf()
            return
        }

        val datos = hashMapOf(
            "idLibro" to idLibro,
            "title" to tituloLibro,
            "authors" to autorLibro.split(",").map { it.trim() },
            "pdfUrl" to pdfUrl,
            "pdfPath" to pdfStoragePath,
            "coverPath" to portadaStoragePath,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        //db.collection("lecturas")
        db.collection("readings")
            .document(uid)
            .set(datos)
            .addOnSuccessListener {
                abrirPdf()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "No se pudo guardar en continuar leyendo: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                abrirPdf()
            }
    }

    private fun cargarPdfUrlDesdeStorage() {
        if (pdfStoragePath.isBlank()) return

        storage.reference.child(pdfStoragePath).downloadUrl
            .addOnSuccessListener { uri ->
                pdfUrl = uri.toString()
            }
            .addOnFailureListener {
                Toast.makeText(this, "No se pudo cargar el PDF", Toast.LENGTH_LONG).show()
            }
    }


    private fun cargarReviews() {
        if (idLibro.isBlank()) return

        db.collection("books")
            .document(idLibro)
            .collection("reviews")
            .orderBy("updatedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->

                llReviewsContainer.removeAllViews()

                if (result.isEmpty) {
                    val tvSinResenas = TextView(this)
                    tvSinResenas.text = getString(R.string.sin_resenias)
                    tvSinResenas.textSize = 14f
                    tvSinResenas.setPadding(16, 12, 16, 12)
                    llReviewsContainer.addView(tvSinResenas)
                    return@addOnSuccessListener
                }

                for (doc in result.documents) {
                    val userName = doc.getString("userName") ?: "Usuario"
                    val comment = doc.getString("comment") ?: "Sin comentario"
                    val rating = doc.getLong("rating")?.toInt() ?: 0

                    val cardReview = crearCardReview(userName, comment, rating)
                    llReviewsContainer.addView(cardReview)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Error al cargar reseñas: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
    private fun crearCardReview(
        userName: String,
        comment: String,
        rating: Int
    ): LinearLayout {

        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.setPadding(18, 18, 18, 18)
        card.setBackgroundColor(android.graphics.Color.WHITE)

        val params = LinearLayout.LayoutParams(
            dp(220),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 0, dp(12), 0)
        card.layoutParams = params

        val tvEstrellas = TextView(this)
        tvEstrellas.text = "${"★".repeat(rating)}${"☆".repeat(5 - rating)}   $rating/5"
        tvEstrellas.textSize = 15f
        tvEstrellas.setTextColor(android.graphics.Color.parseColor("#1E3A5F"))
        tvEstrellas.setTypeface(null, android.graphics.Typeface.BOLD)

        val tvUsuario = TextView(this)
        tvUsuario.text = userName
        tvUsuario.textSize = 14f
        tvUsuario.setTypeface(null, android.graphics.Typeface.BOLD)
        tvUsuario.setPadding(0, 8, 0, 4)

        val tvComentario = TextView(this)
        tvComentario.text = comment
        tvComentario.textSize = 13f
        tvComentario.maxLines = 4
        tvComentario.setTextColor(android.graphics.Color.DKGRAY)

        card.addView(tvEstrellas)
        card.addView(tvUsuario)
        card.addView(tvComentario)

        return card
    }

    private fun comprobarSiPuedeTraducir() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid == null) {
            Toast.makeText(this, getString(R.string.toast_login_traducir), Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { userDoc ->

                val rol = userDoc.getString("rol") ?: ""
                val estado = userDoc.getString("roleVerificationStatus") ?: ""
                val idiomaTraductor = userDoc.getString("nativeLanguage") ?: ""

                if (rol != "translator") {
                    Toast.makeText(this, getString(R.string.toast_solo_traductores), Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                if (estado != "verified") {
                    Toast.makeText(this, getString(R.string.toast_traductor_no_verificado), Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                db.collection("books")
                    .document(idLibro)
                    .get()
                    .addOnSuccessListener { bookDoc ->

                        val idiomaOriginal = bookDoc.getString("language") ?: ""
                        val idiomasDisponibles = bookDoc.get("availableLanguages") as? List<*>
                        val listaIdiomas = idiomasDisponibles
                            ?.mapNotNull { it as? String }
                            ?: listOf(idiomaOriginal)

                        if (idiomaTraductor.isBlank()) {
                            Toast.makeText(this, getString(R.string.toast_sin_idioma_nativo), Toast.LENGTH_LONG).show()
                            return@addOnSuccessListener
                        }

                        if (listaIdiomas.contains(idiomaTraductor)) {
                            Toast.makeText(
                                this,
                                getString(R.string.toast_libro_ya_idioma),
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            val intent = Intent(this, SolicitudTraduccion::class.java).apply {
                                putExtra("bookId", idLibro)
                                putExtra("bookTitle", tituloLibro)
                                putExtra("sourceLanguage", idiomaOriginal)
                                putExtra("targetLanguage", idiomaTraductor)
                                putExtra("modo", "subir_traduccion")
                            }
                            startActivity(intent)
                        }
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, getString(R.string.toast_error_permisos), Toast.LENGTH_LONG).show()
            }
    }

    private fun cargarIdiomasDisponibles(doc: com.google.firebase.firestore.DocumentSnapshot) {
        llIdiomasDisponibles.removeAllViews()

        val idiomaOriginal = doc.getString("language") ?: ""
        val pdfOriginalPath = doc.getString("pdfPath") ?: ""

        val availableLanguages = doc.get("availableLanguages") as? List<*>
        val idiomas = mutableListOf<String>()

        if (idiomaOriginal.isNotBlank()) {
            idiomas.add(idiomaOriginal)
        }

        availableLanguages
            ?.mapNotNull { it as? String }
            ?.forEach { idioma ->
                if (!idiomas.contains(idioma)) {
                    idiomas.add(idioma)
                }
            }

        if (idiomas.isEmpty()) {
            val tv = TextView(this)
            tv.text = getString(R.string.sin_idiomas_disponibles)
            tv.textSize = 14f
            tv.setTextColor(android.graphics.Color.DKGRAY)
            tv.setPadding(12, 12, 12, 12)
            llIdiomasDisponibles.addView(tv)
            return
        }

        for (idioma in idiomas) {
            val botonIdioma = Button(this)
            botonIdioma.text = "Leer en $idioma"
            botonIdioma.isAllCaps = false
            botonIdioma.textSize = 15f
            botonIdioma.setTextColor(android.graphics.Color.WHITE)
            botonIdioma.setBackgroundColor(android.graphics.Color.parseColor("#1E3A5F"))

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(44)
            )
            params.setMargins(0, 0, 0, dp(8))
            botonIdioma.layoutParams = params

            botonIdioma.setOnClickListener {
                abrirPdfPorIdioma(doc, idioma, idiomaOriginal, pdfOriginalPath)
            }

            llIdiomasDisponibles.addView(botonIdioma)
        }
    }

    private fun abrirPdfPorIdioma(
        doc: com.google.firebase.firestore.DocumentSnapshot,
        idiomaSeleccionado: String,
        idiomaOriginal: String,
        pdfOriginalPath: String
    ) {
        if (idiomaSeleccionado == idiomaOriginal) {
            guardarContinuarLeyendoPorIdioma(
                idiomaSeleccionado,
                pdfOriginalPath,
                ""
            )
            return
        }

        val translations = doc.get("translations") as? Map<*, *>
        val traduccion = translations?.get(idiomaSeleccionado) as? Map<*, *>

        val translationUrl = traduccion?.get("translationUrl")?.toString() ?: ""
        val translationPath = traduccion?.get("pdfPath")?.toString()
            ?: traduccion?.get("translationPath")?.toString()
            ?: ""

        when {
            translationUrl.isNotBlank() -> {
                guardarContinuarLeyendoPorIdioma(
                    idiomaSeleccionado,
                    translationPath,
                    translationUrl
                )
            }

            translationPath.isNotBlank() -> {
                guardarContinuarLeyendoPorIdioma(
                    idiomaSeleccionado,
                    translationPath,
                    ""
                )
            }

            else -> Toast.makeText(
                this,
                "No se encontró el PDF en $idiomaSeleccionado",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun abrirPdfDesdeStorage(rutaPdf: String) {
        if (rutaPdf.isBlank()) {
            Toast.makeText(this, "No se encontró la ruta del PDF", Toast.LENGTH_LONG).show()
            return
        }

        storage.reference.child(rutaPdf).downloadUrl
            .addOnSuccessListener { uri ->
                abrirPdfDesdeUrl(uri.toString())
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Error al abrir PDF: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun abrirPdfDesdeUrl(urlPdf: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(urlPdf), "application/pdf")
            addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(intent, getString(R.string.chooser_abrir_libro)))
    }

    private fun guardarContinuarLeyendoPorIdioma(
        idiomaSeleccionado: String,
        rutaPdf: String,
        urlPdfDirecta: String
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid == null || idLibro.isBlank()) {
            if (urlPdfDirecta.isNotBlank()) {
                abrirPdfDesdeUrl(urlPdfDirecta)
            } else {
                abrirPdfDesdeStorage(rutaPdf)
            }
            return
        }

        val tituloConIdioma = "$tituloLibro ($idiomaSeleccionado)"

        if (urlPdfDirecta.isNotBlank()) {
            guardarReadingYabrir(uid, tituloConIdioma, idiomaSeleccionado, rutaPdf, urlPdfDirecta)
        } else {
            storage.reference.child(rutaPdf).downloadUrl
                .addOnSuccessListener { uri ->
                    guardarReadingYabrir(
                        uid,
                        tituloConIdioma,
                        idiomaSeleccionado,
                        rutaPdf,
                        uri.toString()
                    )
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error al abrir PDF: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun guardarReadingYabrir(
        uid: String,
        tituloConIdioma: String,
        idiomaSeleccionado: String,
        rutaPdf: String,
        urlPdf: String
    ) {
        val datos = hashMapOf(
            "idLibro" to idLibro,
            "title" to tituloConIdioma,
            "baseTitle" to tituloLibro,
            "readingLanguage" to idiomaSeleccionado,
            "authors" to autorLibro.split(",").map { it.trim() },
            "pdfUrl" to urlPdf,
            "pdfPath" to rutaPdf,
            "coverPath" to portadaStoragePath,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        db.collection("readings")
            .document(uid)
            .set(datos)
            .addOnSuccessListener {
                abrirPdfDesdeUrl(urlPdf)
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "No se pudo guardar en continuar leyendo: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                abrirPdfDesdeUrl(urlPdf)
            }
    }

    private fun cargarAudioLibro(bookId: String, language: String) {

        btnEscucharAudiolibro.visibility = android.view.View.GONE
        audioUrl = ""

        db.collection("books")
            .document(bookId)
            .get()
            .addOnSuccessListener { doc ->

                val translations = doc.get("translations") as? Map<*, *>

                val idiomaActual = translations?.get(language) as? Map<*, *>
                val englishTranslation = translations?.get("English") as? Map<*, *>

                val audioUrlDirecto = doc.getString("audioUrl") ?: ""
                val audioPathDirecto = doc.getString("audioPath") ?: ""

                val audioUrlIdiomaActual =
                    idiomaActual?.get("audioUrl")?.toString() ?: ""

                val audioPathIdiomaActual =
                    idiomaActual?.get("audioPath")?.toString() ?: ""

                val audioUrlEnglish =
                    englishTranslation?.get("audioUrl")?.toString() ?: ""

                val audioPathEnglish =
                    englishTranslation?.get("audioPath")?.toString() ?: ""

                when {
                    audioUrlDirecto.isNotBlank() -> {
                        audioUrl = audioUrlDirecto
                        btnEscucharAudiolibro.visibility = android.view.View.VISIBLE
                    }

                    audioUrlIdiomaActual.isNotBlank() -> {
                        audioUrl = audioUrlIdiomaActual
                        btnEscucharAudiolibro.visibility = android.view.View.VISIBLE
                    }

                    audioUrlEnglish.isNotBlank() -> {
                        audioUrl = audioUrlEnglish
                        btnEscucharAudiolibro.visibility = android.view.View.VISIBLE
                    }

                    audioPathDirecto.isNotBlank() -> {
                        obtenerUrlAudioDesdeStorage(audioPathDirecto)
                    }

                    audioPathIdiomaActual.isNotBlank() -> {
                        obtenerUrlAudioDesdeStorage(audioPathIdiomaActual)
                    }

                    audioPathEnglish.isNotBlank() -> {
                        obtenerUrlAudioDesdeStorage(audioPathEnglish)
                    }

                    else -> {
                        btnEscucharAudiolibro.visibility = android.view.View.GONE
                    }
                }
            }
            .addOnFailureListener {
                btnEscucharAudiolibro.visibility = android.view.View.GONE
            }
    }

    private fun obtenerUrlAudioDesdeStorage(audioPath: String) {

        storage.reference.child(audioPath)
            .downloadUrl
            .addOnSuccessListener { uri ->
                audioUrl = uri.toString()
                btnEscucharAudiolibro.visibility = android.view.View.VISIBLE
            }
            .addOnFailureListener {
                audioUrl = ""
                btnEscucharAudiolibro.visibility = android.view.View.GONE
            }
    }

    private fun irAContinuarLeyendoAudio() {
        if (audioUrl.isBlank()) {
            Toast.makeText(this, getString(R.string.toast_no_audiolibro), Toast.LENGTH_LONG).show()
            return
        }

        val uid = FirebaseAuth.getInstance().currentUser?.uid

        val datos = hashMapOf(
            "idLibro" to idLibro,
            "title" to tituloLibro,
            "authors" to autorLibro.split(",").map { it.trim() },
            "coverPath" to portadaStoragePath,
            "audioUrl" to audioUrl,
            "contentType" to "audio",
            "updatedAt" to FieldValue.serverTimestamp()
        )

        if (uid != null) {
            db.collection("readings")
                .document(uid)
                .set(datos)
        }

        val intent = Intent(this, ContinuarLeyendo::class.java).apply {
            putExtra("idLibro", idLibro)
            putExtra("tituloLibro", tituloLibro)
            putExtra("autorLibro", autorLibro)
            putExtra("portadaStoragePath", portadaStoragePath)
            putExtra("portadaResId", portadaResId)
            putExtra("audioUrl", audioUrl)
            putExtra("contentType", "audio")
        }

        startActivity(intent)
    }

    private fun dp(valor: Int): Int {
        return (valor * resources.displayMetrics.density).toInt()
    }

}