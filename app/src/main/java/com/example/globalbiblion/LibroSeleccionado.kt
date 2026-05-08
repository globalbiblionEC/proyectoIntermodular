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


class LibroSeleccionado : BottomBar() {

    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private lateinit var ivPerfil: ImageView
    private lateinit var btnVolver: ImageButton
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
    private lateinit var btnTraducir: Button
    private lateinit var llReviewsContainer: LinearLayout
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

        ivPerfil = findViewById(R.id.ivPerfil)
        btnVolver = findViewById(R.id.btnVolver)
        ivPortada = findViewById(R.id.ivPortadaLibroSeleccionado)
        tvTitulo = findViewById(R.id.tvTituloLibroSeleccionado)
        tvAutor = findViewById(R.id.tvAutorLibroSeleccionado)
        tvSinopsis = findViewById(R.id.tvSinopsis)
        tvGenero = findViewById(R.id.tvGenero)
        tvPublicado = findViewById(R.id.tvPublicado)
        tvIdiomasLibro = findViewById(R.id.tvIdiomasLibro)
        tvExtension = findViewById(R.id.tvExtension)
        tvFechaActualizacion = findViewById(R.id.tvFechaActualizacion)
        btnEscribirResena = findViewById(R.id.btnEscribirResena)
        btnTraducir = findViewById(R.id.btnTraducir)
        llReviewsContainer = findViewById(R.id.llReviewsContainer)

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
        configurarBottomBar()

        ivPerfil.setOnClickListener {
            startActivity(Intent(this, PerfilUsuario::class.java))
        }

        btnVolver.setOnClickListener {
            finish()
        }

        ivPortada.setOnClickListener {
            abrirPdf()
        }

        btnLeerLibro = findViewById(R.id.btnLeerLibro)

        btnLeerLibro.setOnClickListener {
            guardarComoContinuarLeyendoYAbrirPdf()
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
            val intent = Intent(this, SolicitudTraduccion::class.java).apply {
                putExtra("bookId", idLibro)
                putExtra("bookTitle", tituloLibro)
                putExtra("sourceLanguage", "Spanish")
            }
            startActivity(intent)
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

                val descripcion = doc.getString("description") ?: "Sinopsis no disponible"

                portadaStoragePath = doc.getString("coverPath") ?: portadaStoragePath
                pdfStoragePath = doc.getString("pdfPath") ?: pdfStoragePath

                val averageRating = doc.getDouble("averageRating") ?: 0.0
                val reviewsCount = doc.getLong("reviewsCount") ?: 0L

                val categories = doc.get("categories") as? List<*>
                val categoria = categories
                    ?.mapNotNull { it as? String }
                    ?.joinToString(", ")
                    ?.takeIf { it.isNotBlank() }
                    ?: "-"

                val language = doc.getString("language") ?: "-"

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
            Toast.makeText(this, "No se ha encontrado la URL del PDF", Toast.LENGTH_LONG).show()
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
                    tvSinResenas.text = "Sin reseñas todavía"
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

    private fun dp(valor: Int): Int {
        return (valor * resources.displayMetrics.density).toInt()
    }
}