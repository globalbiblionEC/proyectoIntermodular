package com.example.globalbiblion

import android.content.Intent
import android.os.Bundle
import android.widget.*
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class Biblioteca : BottomBar (){

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

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

    private lateinit var tvNombreUsuario: TextView
    private lateinit var ivPerfil: ImageView

    private lateinit var llLibro1: LinearLayout
    private lateinit var llLibro2: LinearLayout
    private lateinit var llLibro3: LinearLayout

    private lateinit var tvLibro1Titulo: TextView
    private lateinit var tvLibro2Titulo: TextView
    private lateinit var tvLibro3Titulo: TextView

    private lateinit var ivPortadaL1: ImageView
    private lateinit var ivPortadaL2: ImageView
    private lateinit var ivPortadaL3: ImageView

    private lateinit var etBuscarLibro: EditText
    private lateinit var ivLupa: ImageView
    private lateinit var btnVolver: ImageButton


    private var navegando = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_biblioteca)
        configurarBottomBar()

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        tvNombreUsuario = findViewById(R.id.tvNombreUsuario)
        ivPerfil = findViewById(R.id.ivPerfil)

        etBuscarLibro = findViewById(R.id.etBuscarLibro)
        ivLupa = findViewById(R.id.ivLupa)
        btnVolver = findViewById(R.id.btnVolver)

        llLibro1 = findViewById(R.id.llLibro1)
        llLibro2 = findViewById(R.id.llLibro2)
        llLibro3 = findViewById(R.id.llLibro3)

        tvLibro1Titulo = findViewById(R.id.tvLibro1Titulo)
        tvLibro2Titulo = findViewById(R.id.tvLibro2Titulo)
        tvLibro3Titulo = findViewById(R.id.tvLibro4Titulo)

        ivPortadaL1 = findViewById(R.id.ivPortadaL1)
        ivPortadaL2 = findViewById(R.id.ivPortadaL2)
        ivPortadaL3 = findViewById(R.id.ivPortadaL3)

        cargarNombreUsuario()
        cargarDatosBiblioteca()
        cargarPortadasDesdeStorage()

        ivPerfil.setOnClickListener {
            startActivity(Intent(this, PerfilUsuario::class.java))
        }

        btnVolver.setOnClickListener {
            finish()
        }

        llLibro1.setOnClickListener {
            irALibroSeleccionado(libros[0])
        }

        llLibro2.setOnClickListener {
            irALibroSeleccionado(libros[1])
        }

        llLibro3.setOnClickListener {
            irALibroSeleccionado(libros[2])
        }

        ivLupa.setOnClickListener {
            val texto = etBuscarLibro.text.toString().trim()

            if (texto.length < 4) {
                Toast.makeText(this, "Escribe al menos 4 caracteres", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            buscarPor4Letras(texto)
        }

        etBuscarLibro.setOnEditorActionListener { _, _, _ ->
            val texto = etBuscarLibro.text.toString().trim()

            if (texto.length < 4) {
                Toast.makeText(this, "Escribe al menos 4 caracteres", Toast.LENGTH_SHORT).show()
                return@setOnEditorActionListener true
            }

            buscarPor4Letras(texto)
            true
        }

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

    private fun cargarNombreUsuario() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    val nombre = doc.getString("nombre") ?: ""
                    val apellidos = doc.getString("apellidos") ?: ""

                    tvNombreUsuario.text = when {
                        nombre.isNotEmpty() && apellidos.isNotEmpty() -> "$nombre $apellidos"
                        nombre.isNotEmpty() -> nombre
                        else -> "Lector"
                    }
                } else {
                    tvNombreUsuario.text = "Lector"
                }
            }
            .addOnFailureListener {
                tvNombreUsuario.text = "Lector"
            }
    }

    private fun buscarPor4Letras(query: String) {
        val consulta = normalizar(query).take(4)

        val libroEncontrado = libros.firstOrNull { libro ->
            normalizar(libro.titulo).contains(consulta)
        }

        if (libroEncontrado != null) {
            irALibroSeleccionado(libroEncontrado)
        } else {
            Toast.makeText(
                this,
                "No se ha encontrado el libro con: $consulta",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun normalizar(texto: String): String {
        val sinAcentos = java.text.Normalizer.normalize(
            texto,
            java.text.Normalizer.Form.NFD
        ).replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")

        return sinAcentos.lowercase().replace(" ", "")
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
                Toast.makeText(
                    this,
                    "Error al obtener PDF: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    override fun onResume() {
        super.onResume()
        navegando = false
    }
}