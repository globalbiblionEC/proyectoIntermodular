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
    private val libros= mutableListOf<Libro>()
    private val portadasStorage= mutableMapOf<String, String>()

    private lateinit var tvNombreUsuario: TextView
    private lateinit var ivPerfil: ImageView
    private lateinit var etBuscarLibro: EditText
    private lateinit var ivLupa: ImageView
    private lateinit var btnVolver: ImageButton
    private lateinit var gridBiblioteca: GridLayout


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

        gridBiblioteca=findViewById(R.id.gridBiblioteca)

        cargarNombreUsuario()
        cargarLibrosDesdeFirebase()

        ivPerfil.setOnClickListener {
            startActivity(Intent(this, PerfilUsuario::class.java))
        }

        btnVolver.setOnClickListener {
            finish()
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

    private fun cargarLibrosDesdeFirebase() {
        db.collection("books")
            .get()
            .addOnSuccessListener { snapshot ->
                libros.clear()
                portadasStorage.clear()
                gridBiblioteca.removeAllViews()

                for (doc in snapshot.documents) {
                    val idLibro = doc.id
                    val titulo = doc.getString("title") ?: "Sin título"

                    val authors = doc.get("authors") as? List<*>
                    val autor = authors
                        ?.mapNotNull { it as? String }
                        ?.joinToString(", ")
                        ?: "Autor desconocido"

                    val pdfPath = doc.getString("pdfPath") ?: ""
                    val coverPath = doc.getString("coverPath") ?: ""

                    val libro = Libro(
                        idLibro,
                        titulo,
                        autor,
                        pdfPath,
                        0,
                        R.drawable.logogbsinfondo
                    )

                    libros.add(libro)

                    if (coverPath.isNotBlank()) {
                        portadasStorage[idLibro] = coverPath
                    }

                    gridBiblioteca.addView(crearVistaLibro(libro))
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Error cargando libros: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun crearVistaLibro(libro: Libro): LinearLayout {
        val contenedor = LinearLayout(this)
        contenedor.orientation = LinearLayout.VERTICAL
        contenedor.gravity = android.view.Gravity.CENTER_HORIZONTAL
        contenedor.setPadding(dp(8), dp(8), dp(8), dp(8))

        val params = GridLayout.LayoutParams()
        params.width = dp(150)
        params.height = GridLayout.LayoutParams.WRAP_CONTENT
        params.setMargins(dp(8), dp(8), dp(8), dp(16))
        contenedor.layoutParams = params

        val imagen = ImageView(this)
        imagen.layoutParams = LinearLayout.LayoutParams(dp(120), dp(170))
        imagen.scaleType = ImageView.ScaleType.CENTER_CROP
        imagen.setImageResource(R.drawable.logogbsinfondo)

        val rutaPortada = portadasStorage[libro.idLibro]

        if (!rutaPortada.isNullOrBlank()) {
            storage.reference.child(rutaPortada).downloadUrl
                .addOnSuccessListener { uri ->
                    Glide.with(this)
                        .load(uri.toString())
                        .placeholder(R.drawable.logogbsinfondo)
                        .error(R.drawable.logogbsinfondo)
                        .into(imagen)
                }
                .addOnFailureListener {
                    imagen.setImageResource(R.drawable.logogbsinfondo)
                }
        }

        val titulo = TextView(this)
        titulo.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        titulo.text = libro.titulo
        titulo.gravity = android.view.Gravity.CENTER
        titulo.maxLines = 2
        titulo.textSize = 14f
        titulo.setTypeface(null, android.graphics.Typeface.BOLD)

        contenedor.addView(imagen)
        contenedor.addView(titulo)

        contenedor.setOnClickListener {
            irALibroSeleccionado(libro)
        }

        return contenedor
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
    private fun dp(valor: Int): Int {
        return (valor * resources.displayMetrics.density).toInt()
    }
}