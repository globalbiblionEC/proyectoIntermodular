package com.example.globalbiblion

import android.content.Intent
import android.os.Bundle //Para recibir información del ciclo de vuda de la pantalla
import android.widget.* //Componentes visuales
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

//Activty para mostrar todos los libros guardados en Firebase
class Biblioteca : Bars (){//Heredamos de Bars
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private val libros= mutableListOf<Libro>() //Aquí se guardan los libros
    private val portadasStorage= mutableMapOf<String, String>()
    private lateinit var gridBiblioteca: GridLayout
    private var navegando = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_biblioteca)

        configurarTopBar() //Funciones de las bars
        configurarBottomBar()

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        gridBiblioteca=findViewById(R.id.gridBiblioteca)

        cargarLibrosDesdeFirebase()
    }

    private fun cargarLibrosDesdeFirebase() {
        db.collection("books")
            .get()
            .addOnSuccessListener { snapshot -> //si todo es correcto se ejecuta
                libros.clear()
                portadasStorage.clear()
                gridBiblioteca.removeAllViews()

                for (doc in snapshot.documents) {//Recorremos cada elemento de la colección books
                    val idLibro = doc.id
                    val titulo = doc.getString("title") ?: "Sin título"

                    val authors = doc.get("authors") as? List<*>
                    val autor = authors
                        ?.mapNotNull { it as? String }
                        ?.joinToString(", ")
                        ?: "Autor desconocido" //Valor por defecto

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

                    libros.add(libro) //Añadimos el libro encontrado a la lista

                    if (coverPath.isNotBlank()) {
                        portadasStorage[idLibro] = coverPath
                    }

                    gridBiblioteca.addView(crearVistaLibro(libro))
                }
            }
            .addOnFailureListener { e -> //Si no se ejecutta correctamente, mostramos un Toast
                Toast.makeText(
                    this,
                    "Error cargando libros: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun crearVistaLibro(libro: Libro): LinearLayout {
        val contenedor = LinearLayout(this)
        contenedor.orientation = LinearLayout.VERTICAL //Coloca los elementos en vertical
        contenedor.gravity = android.view.Gravity.CENTER_HORIZONTAL ///Luego horizontalmente
        contenedor.setPadding(dp(8), dp(8), dp(8), dp(8))

        val params = GridLayout.LayoutParams()
        params.width = dp(150) //Ancho de cada tarjeta
        params.height = GridLayout.LayoutParams.WRAP_CONTENT ///Alto de cada tarjeta
        params.setMargins(dp(8), dp(8), dp(8), dp(16))
        contenedor.layoutParams = params

        val imagen = ImageView(this) //Acá mostraremos la portada
        imagen.layoutParams = LinearLayout.LayoutParams(dp(120), dp(170))
        imagen.scaleType = ImageView.ScaleType.CENTER_CROP
        imagen.setImageResource(R.drawable.logogbsinfondo)

        val rutaPortada = portadasStorage[libro.idLibro]

        if (!rutaPortada.isNullOrBlank()) {
            storage.reference.child(rutaPortada).downloadUrl
                .addOnSuccessListener { uri ->
                    Glide.with(this)//Cargamos la portada con Glide
                        .load(uri.toString())
                        .placeholder(R.drawable.logogbsinfondo)//Mientras carga se ve el logo
                        .error(R.drawable.logogbsinfondo) //Si da error también se ve el logo
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
        titulo.setTypeface(null, android.graphics.Typeface.BOLD)//Ponemos el titulo en negrita

        contenedor.addView(imagen)
        contenedor.addView(titulo)

        contenedor.setOnClickListener {
            irALibroSeleccionado(libro)
        }

        return contenedor
    }

    private fun irALibroSeleccionado(libro: Libro) {
        if (navegando) return //Si ya hya una acción en progreso, no se hace nada
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
        navegando = false//cuando volvamos a esta pantalla ponemos a false el navegando
    }
    //Para que los pixeles se vean bien distintas pantallas
    private fun dp(valor: Int): Int {
        return (valor * resources.displayMetrics.density).toInt()
    }
}