package com.example.globalbiblion

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent //Para activar el reconocimiento de voz
import android.widget.*
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.Locale

//Vista para buscar libros por reconocimiento de voz
class BuscarPorVoz : Bars() {//Heredamos de bars
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private lateinit var btnVolver: ImageButton
    private lateinit var btnHablar: Button
    private lateinit var tvResultadoVoz: TextView
    private lateinit var gridBiblioteca: GridLayout
    private val libros =mutableListOf<Libro>()
    private val portadasStorage = mutableMapOf<String, String>()

    private val codigoVoz = 100 //Código no número de caracteres

    private var navegando = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_buscar_por_voz)
        configurarBottomBar()

        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        btnVolver = findViewById(R.id.btnVolver)
        btnHablar = findViewById(R.id.btnHablar)
        tvResultadoVoz = findViewById(R.id.tvResultadoVoz)
        gridBiblioteca = findViewById(R.id.gridBibliotecaVoz)

        cargarLibrosDesdeFirebase()

        btnVolver.setOnClickListener {
            finish() //Volver hacia atrás
        }


        btnHablar.setOnClickListener {
            iniciarReconocimientoVoz()
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


    private fun iniciarReconocimientoVoz() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {//Activamos el reconocimiento de voz
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,//El usuario habla libremente y no frases predefinidas
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())//Se usa el idioma que tenga el usuario por defecto
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Di el nombre del libro")
        }

        try {
            startActivityForResult(intent, codigoVoz)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Tu dispositivo no permite búsqueda por voz", Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) { //Para cuando el usuario termine de hablar
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == codigoVoz && resultCode == RESULT_OK && data != null) {
            val resultados = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)//Obtenemos los resultados
            val textoVoz = resultados?.get(0) ?: "" //Obtenemos la primera frase reconocida

            tvResultadoVoz.text = "Has dicho: $textoVoz"
            buscarLibroPorVoz(textoVoz)
        }
    }

    private fun buscarLibroPorVoz(texto: String) {
        val consulta = normalizar(texto)

        val libroEncontrado = libros.firstOrNull { libro ->
            normalizar(libro.titulo).contains(consulta) ||
                    consulta.contains(normalizar(libro.titulo).take(4))//cogemos 4 carateres
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