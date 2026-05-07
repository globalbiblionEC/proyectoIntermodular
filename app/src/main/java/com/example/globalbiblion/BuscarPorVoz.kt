package com.example.globalbiblion

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.*
import com.bumptech.glide.Glide
import com.google.firebase.storage.FirebaseStorage
import java.util.Locale

class BuscarPorVoz : BottomBar() {
    private lateinit var storage: FirebaseStorage

    private lateinit var btnVolver: ImageButton
    private lateinit var ivPerfil: ImageView
    private lateinit var btnHablar: Button
    private lateinit var tvResultadoVoz: TextView

    private lateinit var llLibro1: LinearLayout
    private lateinit var llLibro2: LinearLayout
    private lateinit var llLibro3: LinearLayout

    private lateinit var ivPortadaL1: ImageView
    private lateinit var ivPortadaL2: ImageView
    private lateinit var ivPortadaL3: ImageView

    private lateinit var tvLibro1Titulo: TextView
    private lateinit var tvLibro2Titulo: TextView
    private lateinit var tvLibro3Titulo: TextView

    private val codigoVoz = 100

    private val libros = listOf(
        Libro(
            "book_03",
            "Una Habitación propia",
            "Virginia Woolf",
            "books/pdf/Habitacion_Propia.pdf",
            86,
            R.drawable.logogbsinfondo
        ),
        Libro(
            "book_01",
            "El Principito",
            "Antoine de Saint-Exupéry",
            "books/pdf/El principito.pdf",
            88,
            R.drawable.logogbsinfondo
        ),
        Libro(
            "book_02",
            "Rebelión en la granja",
            "George Orwell",
            "books/pdf/Rebelión_en_la_Granja.pdf",
            64,
            R.drawable.logogbsinfondo
        )
    )

    private val portadasStorage = mapOf(
        "book_03" to "books/covers/Habitacion_propia.jpg",
        "book_01" to "books/covers/El_principito.jpg",
        "book_02" to "books/covers/Rebelion_en_la_granja.jpg"
    )

    private var navegando = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_buscar_por_voz)
        configurarBottomBar()

        storage = FirebaseStorage.getInstance()

        btnVolver = findViewById(R.id.btnVolver)
        ivPerfil = findViewById(R.id.ivPerfil)
        btnHablar = findViewById(R.id.btnHablar)
        tvResultadoVoz = findViewById(R.id.tvResultadoVoz)

        llLibro1 = findViewById(R.id.llLibro1)
        llLibro2 = findViewById(R.id.llLibro2)
        llLibro3 = findViewById(R.id.llLibro3)

        ivPortadaL1 = findViewById(R.id.ivPortadaL1)
        ivPortadaL2 = findViewById(R.id.ivPortadaL2)
        ivPortadaL3 = findViewById(R.id.ivPortadaL3)

        tvLibro1Titulo = findViewById(R.id.tvLibro1Titulo)
        tvLibro2Titulo = findViewById(R.id.tvLibro2Titulo)
        tvLibro3Titulo = findViewById(R.id.tvLibro3Titulo)

        cargarDatosBiblioteca()
        cargarPortadasDesdeStorage()

        btnVolver.setOnClickListener {
            finish()
        }

        ivPerfil.setOnClickListener {
            startActivity(Intent(this, PerfilUsuario::class.java))
        }

        btnHablar.setOnClickListener {
            iniciarReconocimientoVoz()
        }

        llLibro1.setOnClickListener { irALibroSeleccionado(libros[0]) }
        llLibro2.setOnClickListener { irALibroSeleccionado(libros[1]) }
        llLibro3.setOnClickListener { irALibroSeleccionado(libros[2]) }
    }

    private fun iniciarReconocimientoVoz() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Di el nombre del libro")
        }

        try {
            startActivityForResult(intent, codigoVoz)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Tu dispositivo no permite búsqueda por voz", Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == codigoVoz && resultCode == RESULT_OK && data != null) {
            val resultados = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val textoVoz = resultados?.get(0) ?: ""

            tvResultadoVoz.text = "Has dicho: $textoVoz"
            buscarLibroPorVoz(textoVoz)
        }
    }

    private fun buscarLibroPorVoz(texto: String) {
        val consulta = normalizar(texto)

        val libroEncontrado = libros.firstOrNull { libro ->
            normalizar(libro.titulo).contains(consulta) ||
                    consulta.contains(normalizar(libro.titulo).take(4))
        }

        if (libroEncontrado != null) {
            irALibroSeleccionado(libroEncontrado)
        } else {
            Toast.makeText(this, "No se encontró ningún libro con: $texto", Toast.LENGTH_LONG).show()
        }
    }

    private fun normalizar(texto: String): String {
        val sinAcentos = java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")

        return sinAcentos.lowercase().replace(" ", "")
    }

    private fun cargarDatosBiblioteca() {
        tvLibro1Titulo.text = libros[0].titulo
        tvLibro2Titulo.text = libros[1].titulo
        tvLibro3Titulo.text = libros[2].titulo
    }

    private fun cargarPortadasDesdeStorage() {
        cargarPortadaDesdeStorage(libros[0], ivPortadaL1)
        cargarPortadaDesdeStorage(libros[1], ivPortadaL2)
        cargarPortadaDesdeStorage(libros[2], ivPortadaL3)
    }

    private fun cargarPortadaDesdeStorage(libro: Libro, imageView: ImageView) {
        val rutaPortada = portadasStorage[libro.idLibro]

        if (rutaPortada == null) {
            imageView.setImageResource(libro.portadaResId)
            return
        }

        storage.reference.child(rutaPortada).downloadUrl
            .addOnSuccessListener { uri ->
                Glide.with(this)
                    .load(uri.toString())
                    .placeholder(libro.portadaResId)
                    .error(libro.portadaResId)
                    .into(imageView)
            }
            .addOnFailureListener {
                imageView.setImageResource(libro.portadaResId)
            }
    }

    private fun irALibroSeleccionado(libro: Libro) {
        if (navegando) return
        navegando = true

        val rutaPdfStorage = libro.nombrePDF
        val rutaPortadaStorage = portadasStorage[libro.idLibro] ?: ""

        storage.reference.child(rutaPdfStorage).downloadUrl
            .addOnSuccessListener { pdfUri ->
                val intent = Intent(this, LibroSeleccionado::class.java).apply {
                    putExtra("idLibro", libro.idLibro)
                    putExtra("tituloLibro", libro.titulo)
                    putExtra("autorLibro", libro.autor)
                    putExtra("pdfUrl", pdfUri.toString())
                    putExtra("pdfStoragePath", rutaPdfStorage)
                    putExtra("portadaStoragePath", rutaPortadaStorage)
                    putExtra("portadaResId", libro.portadaResId)
                }

                startActivity(intent)
            }
            .addOnFailureListener { e ->
                navegando = false
                Toast.makeText(this, "Error al obtener PDF: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    override fun onResume() {
        super.onResume()
        navegando = false
    }
}