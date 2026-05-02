package com.example.globalbiblion

import android.os.Bundle
import android.content.Intent
import android.graphics.Color
import android.view.Gravity
import android.widget.*
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class EscribirResenia : BottomBar() {//BottomBar child

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private lateinit var ivPerfil: ImageView
    private lateinit var btnVolver: ImageButton
    private lateinit var ivPortadaLibro: ImageView
    private lateinit var tvTituloLibro: TextView
    private lateinit var tvAutorLibro: TextView
    private lateinit var tvRatingLibro: TextView
    private lateinit var ratingBarResena: RatingBar
    private lateinit var etComentario: EditText
    private lateinit var btnPublicarResena: Button
    private lateinit var llMisResenas: LinearLayout
    private lateinit var llBiblioteca: LinearLayout
    private lateinit var llContileyendo: LinearLayout
    private lateinit var tvNombreUsuarioPerfil: TextView

    private var idLibro = ""
    private var tituloLibro = ""
    private var autorLibro = ""
    private var portadaStoragePath = ""
    private var pdfUrl = ""
    private var pdfStoragePath = ""
    private var portadaResId = 0

    private var reviewIdEditando: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_escribir_resenia)
        configurarBottomBar()

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        ivPerfil = findViewById(R.id.ivPerfil)
        btnVolver = findViewById(R.id.btnVolver)
        ivPortadaLibro = findViewById(R.id.ivPortadaLibro)
        tvTituloLibro = findViewById(R.id.tvTituloLibro)
        tvAutorLibro = findViewById(R.id.tvAutorLibro)
        tvRatingLibro = findViewById(R.id.tvRatingLibro)
        ratingBarResena = findViewById(R.id.ratingBarResena)
        etComentario = findViewById(R.id.etComentario)
        btnPublicarResena = findViewById(R.id.btnPublicarResena)
        llMisResenas = findViewById(R.id.llMisResenas)
        llBiblioteca = findViewById(R.id.llCatalogo)
        llContileyendo = findViewById(R.id.llContileyendo)
        tvNombreUsuarioPerfil = findViewById(R.id.tvNombreUsuario)

        idLibro = intent.getStringExtra("idLibro") ?: ""
        tituloLibro = intent.getStringExtra("tituloLibro") ?: "Título"
        autorLibro = intent.getStringExtra("autorLibro") ?: "Autor"
        portadaStoragePath = intent.getStringExtra("portadaStoragePath") ?: ""
        pdfUrl = intent.getStringExtra("pdfUrl") ?: ""
        pdfStoragePath = intent.getStringExtra("pdfStoragePath") ?: ""
        portadaResId = intent.getIntExtra("portadaResId", R.drawable.logogbsinfondo)

        tvTituloLibro.text = tituloLibro
        tvAutorLibro.text = autorLibro

        cargarPortada()
        cargarDatosLibro()
        cargarMisResenas()
        cargarNombreUsuario()

        btnVolver.setOnClickListener { finish() }

        ivPerfil.setOnClickListener {
            startActivity(Intent(this, PerfilUsuario::class.java))
        }

        btnPublicarResena.setOnClickListener {
            guardarResena()
        }

        llBiblioteca.setOnClickListener {
            startActivity(Intent(this, Biblioteca::class.java))
        }

        llContileyendo.setOnClickListener {
            val intent = Intent(this, ContinuarLeyendo::class.java).apply {
                putExtra("idLibro", idLibro)
                putExtra("tituloLibro", tituloLibro)
                putExtra("pdfUrl", pdfUrl)
                putExtra("pdfStoragePath", pdfStoragePath)
                putExtra("portadaStoragePath", portadaStoragePath)
                putExtra("portadaResId", portadaResId)
            }
            startActivity(intent)
        }
    }

    private fun cargarPortada() {
        if (portadaStoragePath.isBlank()) {
            ivPortadaLibro.setImageResource(portadaResId)
            return
        }

        storage.reference.child(portadaStoragePath).downloadUrl
            .addOnSuccessListener { uri ->
                Glide.with(this)
                    .load(uri.toString())
                    .placeholder(portadaResId)
                    .error(portadaResId)
                    .into(ivPortadaLibro)
            }
            .addOnFailureListener {
                ivPortadaLibro.setImageResource(portadaResId)
            }
    }

    private fun cargarDatosLibro() {
        if (idLibro.isBlank()) return

        db.collection("books")
            .document(idLibro)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) return@addOnSuccessListener

                tituloLibro = doc.getString("title") ?: tituloLibro

                val authors = doc.get("authors") as? List<*>
                autorLibro = authors
                    ?.mapNotNull { it as? String }
                    ?.joinToString(", ")
                    ?.takeIf { it.isNotBlank() }
                    ?: autorLibro

                portadaStoragePath = doc.getString("coverPath") ?: portadaStoragePath
                pdfStoragePath = doc.getString("pdfPath") ?: pdfStoragePath

                val averageRating = doc.getDouble("averageRating") ?: 0.0
                val ratingEntero = averageRating.toInt().coerceIn(0, 5)

                tvTituloLibro.text = tituloLibro
                tvAutorLibro.text = autorLibro
                tvRatingLibro.text =
                    "$averageRating/5   ${"★".repeat(ratingEntero)}${"☆".repeat(5 - ratingEntero)}"

                cargarPortada()
            }
    }

    private fun guardarResena() {
        val uid = auth.currentUser?.uid

        if (uid == null) {//If is a guest, he cant review
            Toast.makeText(this, "Debes iniciar sesión para escribir una reseña", Toast.LENGTH_LONG).show()
            return
        }

        if (idLibro.isBlank()) {
            Toast.makeText(this, "Error: idLibro vacío", Toast.LENGTH_LONG).show()
            return
        }

        val comentario = etComentario.text.toString().trim()
        val rating = ratingBarResena.rating.toInt()

        if (rating <= 0) {
            Toast.makeText(this, "Selecciona una puntuación", Toast.LENGTH_SHORT).show()
            return
        }

        if (comentario.isEmpty()) {
            etComentario.error = "Escribe tu reseña"
            etComentario.requestFocus()
            return
        }

        btnPublicarResena.isEnabled = false

        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { userDoc ->
                val nombre = userDoc.getString("nombre") ?: "Usuario"
                val apellidos = userDoc.getString("apellidos") ?: ""
                val userName = "$nombre $apellidos".trim()

                val datos = hashMapOf(
                    "comment" to comentario,
                    "rating" to rating,
                    "userId" to uid,
                    "userName" to userName,
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                db.collection("books")
                    .document(idLibro)
                    .collection("reviews")
                    .document(uid)
                    .set(datos)
                    .addOnSuccessListener {
                        limpiarFormulario()
                        cargarMisResenas()
                        Toast.makeText(this, "Reseña guardada", Toast.LENGTH_SHORT).show()
                        btnPublicarResena.isEnabled = true
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
                        btnPublicarResena.isEnabled = true
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al obtener usuario: ${e.message}", Toast.LENGTH_LONG).show()
                btnPublicarResena.isEnabled = true
            }
    }

    private fun cargarMisResenas() {
        val uid = auth.currentUser?.uid ?: return
        if (idLibro.isBlank()) return

        db.collection("books")
            .document(idLibro)
            .collection("reviews")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                llMisResenas.removeAllViews()

                if (!doc.exists()) {
                    val tv = TextView(this)
                    tv.text = "Todavía no has escrito reseñas de este libro."
                    tv.textSize = 14f
                    tv.setTextColor(Color.BLACK)
                    llMisResenas.addView(tv)
                    return@addOnSuccessListener
                }

                val reviewId = doc.id
                val comment = doc.getString("comment") ?: ""
                val rating = doc.getLong("rating")?.toInt() ?: 0
                val userName = doc.getString("userName") ?: "Usuario"

                agregarVistaResena(reviewId, userName, comment, rating)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al cargar reseñas: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun agregarVistaResena(
        reviewId: String,
        userName: String,
        comment: String,
        rating: Int
    ) {
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.setPadding(18, 18, 18, 18)
        card.setBackgroundColor(Color.WHITE)

        val paramsCard = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        paramsCard.setMargins(0, 0, 0, 18)
        card.layoutParams = paramsCard

        val filaSuperior = LinearLayout(this)
        filaSuperior.orientation = LinearLayout.HORIZONTAL
        filaSuperior.gravity = Gravity.CENTER_VERTICAL

        val tvInfo = TextView(this)
        tvInfo.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        tvInfo.text = "$userName\n${"★".repeat(rating)}${"☆".repeat(5 - rating)}"
        tvInfo.textSize = 15f
        tvInfo.setTextColor(Color.BLACK)

        val btnEditar = ImageButton(this)
        btnEditar.setImageResource(android.R.drawable.ic_menu_edit)
        btnEditar.setBackgroundColor(Color.TRANSPARENT)
        btnEditar.contentDescription = "Editar reseña"

        btnEditar.setOnClickListener {
            reviewIdEditando = reviewId
            etComentario.setText(comment)
            ratingBarResena.rating = rating.toFloat()
            btnPublicarResena.text = "Actualizar opinión"
        }

        filaSuperior.addView(tvInfo)
        filaSuperior.addView(btnEditar)

        val tvComentario = TextView(this)
        tvComentario.text = comment
        tvComentario.textSize = 14f
        tvComentario.setTextColor(Color.DKGRAY)
        tvComentario.setPadding(0, 12, 0, 0)

        card.addView(filaSuperior)
        card.addView(tvComentario)

        llMisResenas.addView(card)
    }

    private fun limpiarFormulario() {
        reviewIdEditando = null
        etComentario.setText("")
        ratingBarResena.rating = 0f
        btnPublicarResena.text = "Dejar mi opinión"
    }
    private fun cargarNombreUsuario() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val nombre = doc.getString("nombre") ?: ""
                    val apellidos = doc.getString("apellidos") ?: ""

                    val nombreCompleto = when {
                        nombre.isNotEmpty() && apellidos.isNotEmpty() -> "$nombre $apellidos"
                        nombre.isNotEmpty() -> nombre
                        else -> "Usuario"
                    }

                    tvNombreUsuarioPerfil.text = nombreCompleto
                } else {
                    tvNombreUsuarioPerfil.text = "Usuario"
                }
            }
            .addOnFailureListener {
                tvNombreUsuarioPerfil.text = "Usuario"
            }
    }
}