package com.example.globalbiblion
import android.os.Bundle
import android.content.Intent
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FieldValue

//Esta activity es el centro principal de la aplicación
class Menuprincipal : Bars () {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private val libros=mutableListOf<Libro>()
    private val portadasStorage=mutableMapOf<String,String>()
    private lateinit var gridBiblioteca: LinearLayout
    private lateinit var tvNombreUsuarioTopBar: TextView
    private lateinit var etBuscarLibroTopBar: EditText
    private lateinit var ivLupaTopBar: ImageView
    private lateinit var tvTituloContinuar: TextView
    private lateinit var tvAutorContinuar: TextView
    private lateinit var llContiLeyendo: LinearLayout
    private lateinit var btnContiLeyend: ImageButton
    private lateinit var ivPortadaLibroActual: ImageView
    private lateinit var llTop1Ranking: LinearLayout
    private lateinit var llTop5Ranking: LinearLayout
    private lateinit var llAudiolibros: LinearLayout
    private var libroActual: Libro? =null
    private var navegando = false
    private var portadaActualPath = ""
    private var pdfActualUrl = ""
    private var idiomaActualLectura = ""
    private var audioActualUrl = ""
    private var contentTypeActual = ""
    private var audioPositionActual = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menuprincipal)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        tvTituloContinuar = findViewById(R.id.tvTituloContinuar)
        tvAutorContinuar = findViewById(R.id.tvAutorContinuar)
        ivPortadaLibroActual = findViewById(R.id.ivPortadaLibroactual)
        llContiLeyendo = findViewById(R.id.llContiLeyendo)
        btnContiLeyend = findViewById(R.id.btnContiLeyend)

        etBuscarLibroTopBar = findViewById(R.id.etBuscarLibroTopBar)
        ivLupaTopBar = findViewById(R.id.ivLupaTopBar)

        tvNombreUsuarioTopBar = findViewById(R.id.tvNombreUsuarioTopBar)

        llTop1Ranking = findViewById(R.id.llTop1Ranking)
        llTop5Ranking = findViewById(R.id.llTop5Ranking)
        llAudiolibros = findViewById(R.id.llAudiolibros)
        gridBiblioteca = findViewById(R.id.gridBiblioteca)

        cargarLibrosDesdeFirebase()
        cargarNombreUsuario()
        configurarBottomBar()
        configurarTopBar()

        findViewById<LinearLayout>(R.id.llContileyendo).setOnClickListener {
            irAContinuarLeyendo()
        }

        val listenerContinuar = {
            irAContinuarLeyendo()
        }

        llContiLeyendo.setOnClickListener { listenerContinuar() }
        btnContiLeyend.setOnClickListener { listenerContinuar() }

        ivLupaTopBar.setOnClickListener {
            val texto = etBuscarLibroTopBar.text.toString().trim()
            if (texto.length < 4) {
                Toast.makeText(this, "Escribe al menos 4 caracteres", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            buscarPor4Letras(texto)
        }

        etBuscarLibroTopBar.setOnEditorActionListener { _, _, _ ->
            val texto = etBuscarLibroTopBar.text.toString().trim()
            if (texto.length < 4) {
                Toast.makeText(this, "Escribe al menos 4 caracteres", Toast.LENGTH_SHORT).show()
                return@setOnEditorActionListener true
            }

            buscarPor4Letras(texto)
            true
        }
        cargarDatosDesdeFirebase()
        cargarRankingLibros()
        cargarAudiolibros()
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

                    gridBiblioteca.addView(crearVistaLibroBiblioteca(libro))
                }

                if (libros.isNotEmpty() && libroActual == null) {
                    libroActual = libros[0]
                    portadaActualPath = portadasStorage[libros[0].idLibro] ?: ""
                    actualizarContinuarLeyendo()
                }

                cargarDatosDesdeFirebase()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Error cargando libros: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun crearVistaLibroBiblioteca(libro: Libro): LinearLayout {
        val contenedor = LinearLayout(this)
        contenedor.orientation = LinearLayout.VERTICAL
        contenedor.gravity = android.view.Gravity.CENTER_HORIZONTAL
        contenedor.setPadding(dp(8), dp(8), dp(8), dp(8))

        val paramsContenedor = LinearLayout.LayoutParams(
            dp(120),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        paramsContenedor.setMargins(0, 0, dp(12), 0)
        contenedor.layoutParams = paramsContenedor

        val imagen = ImageView(this)
        imagen.layoutParams = LinearLayout.LayoutParams(dp(85), dp(89))
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
        }

        val titulo = TextView(this)
        titulo.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        titulo.text = libro.titulo
        titulo.gravity = android.view.Gravity.CENTER
        titulo.maxLines = 2
        titulo.textSize = 12f

        contenedor.addView(imagen)
        contenedor.addView(titulo)

        contenedor.setOnClickListener {
            irALibroSeleccionado(libro)
        }

        return contenedor
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
        val sinAcentos = java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        return sinAcentos.lowercase().replace(" ", "")
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

                    val textoNombre = when {
                        nombre.isNotEmpty() && apellidos.isNotEmpty() -> "$nombre $apellidos"
                        nombre.isNotEmpty() -> nombre
                        else -> "Lector"
                    }
                    tvNombreUsuarioTopBar.text = textoNombre
                } else {
                    tvNombreUsuarioTopBar.text = "Lector"
                }
            }
            .addOnFailureListener {
                tvNombreUsuarioTopBar.text = "Lector"
            }
    }

    private fun actualizarContinuarLeyendo() {
        val libro = libroActual ?: return

        tvTituloContinuar.text = libro.titulo
        tvAutorContinuar.text = libro.autor

        cargarPortadaDesdeStorage(libro, ivPortadaLibroActual)
    }

    private fun cargarPortadaDesdeStorage(libro: Libro, imageView: ImageView) {
        val rutaPortada = portadasStorage[libro.idLibro]

        if (rutaPortada == null) {
            imageView.setImageResource(libro.portadaResId)
            return
        }

        val coverRef = FirebaseStorage.getInstance()
            .reference
            .child(rutaPortada)

        coverRef.downloadUrl
            .addOnSuccessListener { uri ->
                val imageUrl = uri.toString()

                Glide.with(this)
                    .load(imageUrl)
                    .placeholder(libro.portadaResId)
                    .error(libro.portadaResId)
                    .into(imageView)
            }
            .addOnFailureListener {
                imageView.setImageResource(libro.portadaResId)

                Toast.makeText(
                    this,
                    "Error: ${it.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun cargarDatosDesdeFirebase() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("readings")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) return@addOnSuccessListener

                val id = doc.getString("idLibro") ?: return@addOnSuccessListener
                val titulo = doc.getString("title") ?: "Libro actual"

                val authors = doc.get("authors") as? List<String>
                val autor = if (!authors.isNullOrEmpty()) {
                    authors.joinToString(", ")
                } else {
                    ""
                }

                val pdfPath = doc.getString("pdfPath") ?: ""
                val pdfUrl = doc.getString("pdfUrl") ?: ""
                val coverPath = doc.getString("coverPath") ?: ""
                val idioma = doc.getString("readingLanguage") ?: ""

                val audioUrl = doc.getString("audioUrl") ?: ""
                val contentType = doc.getString("contentType") ?: ""
                val audioPosition = doc.getLong("audioPosition")?.toInt() ?: 0

                portadaActualPath = coverPath
                pdfActualUrl = pdfUrl
                idiomaActualLectura = idioma

                audioActualUrl = audioUrl
                contentTypeActual = contentType
                audioPositionActual = audioPosition

                libroActual = Libro(
                    id,
                    titulo,
                    autor,
                    pdfPath,
                    0,
                    R.drawable.logogbsinfondo
                )

                tvTituloContinuar.text = titulo
                tvAutorContinuar.text = autor

                if (coverPath.isNotBlank()) {
                    storage.reference.child(coverPath).downloadUrl
                        .addOnSuccessListener { uri ->
                            Glide.with(this)
                                .load(uri.toString())
                                .placeholder(R.drawable.logogbsinfondo)
                                .error(R.drawable.logogbsinfondo)
                                .into(ivPortadaLibroActual)
                        }
                } else {
                    ivPortadaLibroActual.setImageResource(R.drawable.logogbsinfondo)
                }
            }
    }

    private fun irAContinuarLeyendo() {
        val libro = libroActual ?: run {
            Toast.makeText(this, "No hay libro actual", Toast.LENGTH_SHORT).show()
            return
        }

        if (contentTypeActual == "audio" && audioActualUrl.isNotBlank()) {
            val intent = Intent(this, ContinuarLeyendo::class.java).apply {
                putExtra("idLibro", libro.idLibro)
                putExtra("tituloLibro", libro.titulo)
                putExtra("autorLibro", libro.autor)
                putExtra("portadaStoragePath", portadaActualPath)
                putExtra("portadaResId", libro.portadaResId)
                putExtra("audioUrl", audioActualUrl)
                putExtra("contentType", "audio")
                putExtra("audioPosition", audioPositionActual)
            }

            startActivity(intent)
            return
        }

        if (navegando) return
        navegando = true

        if (pdfActualUrl.isNotBlank()) {
            val intent = Intent(this, ContinuarLeyendo::class.java).apply {
                putExtra("idLibro", libro.idLibro)
                putExtra("pdfUrl", pdfActualUrl)
                putExtra("pdfStoragePath", libro.nombrePDF)
                putExtra("tituloLibro", libro.titulo)
                putExtra("autorLibro", libro.autor)
                putExtra("portadaResId", libro.portadaResId)
                putExtra("portadaStoragePath", portadaActualPath)
                putExtra("readingLanguage", idiomaActualLectura)
            }

            startActivity(intent)
            return
        }

        val rutaPdfStorage = libro.nombrePDF

        storage.reference.child(rutaPdfStorage).downloadUrl
            .addOnSuccessListener { pdfUri ->
                val intent = Intent(this, ContinuarLeyendo::class.java).apply {
                    putExtra("idLibro", libro.idLibro)
                    putExtra("pdfUrl", pdfUri.toString())
                    putExtra("pdfStoragePath", rutaPdfStorage)
                    putExtra("tituloLibro", libro.titulo)
                    putExtra("autorLibro", libro.autor)
                    putExtra("portadaResId", libro.portadaResId)
                    putExtra("portadaStoragePath", portadaActualPath)
                    putExtra("readingLanguage", idiomaActualLectura)
                }

                startActivity(intent)
            }
            .addOnFailureListener { e ->
                navegando = false
                Toast.makeText(
                    this,
                    "Error al obtener PDF desde Firebase Storage: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
    override fun onResume() {
        super.onResume()
        navegando = false
        cargarDatosDesdeFirebase()
    }

    private fun irALibroSeleccionado(libro: Libro) {
        if (navegando) return
        navegando = true

        val rutaPdfStorage = libro.nombrePDF
        val rutaPortadaStorage = portadasStorage[libro.idLibro] ?: portadaActualPath

        db.collection("books")
            .document(libro.idLibro)
            .get()
            .addOnSuccessListener { doc ->

                val rutaPortadaStorage = doc.getString("coverPath")
                    ?: portadasStorage[libro.idLibro]
                    ?: ""

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
            .addOnFailureListener {
                navegando = false
                Toast.makeText(this, "No se pudo cargar la portada del libro", Toast.LENGTH_LONG).show()
            }
    }
    private fun cargarRankingLibros() {
        cargarTop1DesdeFirebase()
        cargarTop5DesdeFirebase()
    }

    private fun cargarTop1DesdeFirebase() {
        db.collection("rankings_books")
            .document("top1")
            .get()
            .addOnSuccessListener { doc ->
                val librosMap = doc.get("books") as? List<Map<String, Any>> ?: emptyList()
                val lista = convertirRanking(librosMap)
                pintarTop1(lista)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error leyendo Top 1: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun cargarTop5DesdeFirebase() {
        db.collection("rankings_books")
            .document("top5")
            .get()
            .addOnSuccessListener { doc ->
                val librosMap = doc.get("books") as? List<Map<String, Any>> ?: emptyList()
                val lista = convertirRanking(librosMap)
                pintarTop5(lista)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error leyendo Top 5: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun convertirRanking(librosMap: List<Map<String, Any>>): List<LibroRanking> {
        return librosMap.map { libro ->
            val id = libro["id"]?.toString() ?: ""

            val titulo = libro["title"]?.toString()
                ?: libro["titulo"]?.toString()
                ?: "Sin título"

            val authors = libro["authors"] as? List<*>
            val autor = authors
                ?.mapNotNull { it as? String }
                ?.joinToString(", ")
                ?: libro["autor"]?.toString()
                ?: "Autor desconocido"

            val pdfPath = libro["pdfPath"]?.toString() ?: ""
            val coverPath = libro["coverPath"]?.toString() ?: ""

            val averageRating = when (val valor = libro["averageRating"]) {
                is Number -> valor.toDouble()
                else -> 0.0
            }

            val reviewsCount = when (val valor = libro["reviewsCount"]) {
                is Number -> valor.toInt()
                else -> 0
            }

            LibroRanking(
                id = id,
                titulo = titulo,
                autor = autor,
                pdfPath = pdfPath,
                coverPath = coverPath,
                positivos = reviewsCount,
                negativos = 0,
                valor = averageRating * 100
            )
        }
    }

    private fun pintarTop1(top1: List<LibroRanking>) {
        llTop1Ranking.removeAllViews()

        if (top1.isEmpty()) {
            val tv = TextView(this)
            tv.text = "No hay libros valorados todavía."
            llTop1Ranking.addView(tv)
            return
        }

        llTop1Ranking.addView(crearVistaLibroRanking(top1[0], 1))
    }

    private fun pintarTop5(top5: List<LibroRanking>) {
        llTop5Ranking.removeAllViews()

        if (top5.isEmpty()) {
            val tv = TextView(this)
            tv.text = "No hay libros valorados todavía."
            llTop5Ranking.addView(tv)
            return
        }

        for ((index, libro) in top5.withIndex()) {
            llTop5Ranking.addView(crearVistaLibroRanking(libro, index + 1))
        }
    }

    private fun crearVistaLibroRanking(libroRanking: LibroRanking, posicion: Int): LinearLayout {
        val contenedor = LinearLayout(this)
        contenedor.orientation = LinearLayout.VERTICAL
        contenedor.gravity = android.view.Gravity.CENTER_HORIZONTAL
        contenedor.setPadding(dp(8), dp(8), dp(8), dp(8))
        contenedor.setBackgroundColor(android.graphics.Color.parseColor("#E9ECF5"))

        val paramsContenedor = LinearLayout.LayoutParams(
            dp(130),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        paramsContenedor.setMargins(0, dp(8), dp(12), dp(8))
        contenedor.layoutParams = paramsContenedor

        val imagen = ImageView(this)
        imagen.layoutParams = LinearLayout.LayoutParams(dp(85), dp(120))
        imagen.scaleType = ImageView.ScaleType.CENTER_CROP
        imagen.setImageResource(R.drawable.logogbsinfondo)

        if (libroRanking.coverPath.isNotBlank()) {
            storage.reference.child(libroRanking.coverPath).downloadUrl
                .addOnSuccessListener { uri ->
                    Glide.with(this)
                        .load(uri.toString())
                        .placeholder(R.drawable.logogbsinfondo)
                        .error(R.drawable.logogbsinfondo)
                        .into(imagen)
                }
        }

        val titulo = TextView(this)
        titulo.text = "$posicion. ${libroRanking.titulo}"
        titulo.textSize = 12f
        titulo.gravity = android.view.Gravity.CENTER
        titulo.maxLines = 2
        titulo.setTypeface(null, android.graphics.Typeface.BOLD)

        val autor = TextView(this)
        autor.text = libroRanking.autor
        autor.textSize = 11f
        autor.gravity = android.view.Gravity.CENTER
        autor.maxLines = 1

        val puntuacion = TextView(this)
        puntuacion.text = "Valoración: ${libroRanking.valor / 100.0}/5"
        puntuacion.textSize = 11f
        puntuacion.gravity = android.view.Gravity.CENTER

        contenedor.addView(imagen)
        contenedor.addView(titulo)
        contenedor.addView(autor)
        contenedor.addView(puntuacion)

        contenedor.setOnClickListener {
            val libro = Libro(
                libroRanking.id,
                libroRanking.titulo,
                libroRanking.autor,
                libroRanking.pdfPath,
                0,
                R.drawable.logogbsinfondo
            )

            irALibroSeleccionado(libro)
        }

        return contenedor
    }


    private fun cargarAudiolibros() {

        db.collection("books")
            .get()
            .addOnSuccessListener { snapshot ->

                llAudiolibros.removeAllViews()

                for (doc in snapshot.documents) {

                    val audioUrlDirecto = doc.getString("audioUrl") ?: ""
                    val audioPathDirecto = doc.getString("audioPath") ?: ""

                    val translations = doc.get("translations") as? Map<*, *>

                    var audioEncontrado = audioUrlDirecto
                    var audioPathEncontrado = audioPathDirecto
                    var tieneAudio = audioUrlDirecto.isNotBlank() || audioPathDirecto.isNotBlank()

                    if (!tieneAudio && translations != null) {

                        for ((_, value) in translations) {

                            val idiomaData = value as? Map<*, *> ?: continue

                            val audioUrlTranslation =
                                idiomaData["audioUrl"]?.toString() ?: ""

                            val audioPathTranslation =
                                idiomaData["audioPath"]?.toString() ?: ""

                            if (audioUrlTranslation.isNotBlank() || audioPathTranslation.isNotBlank()) {
                                audioEncontrado = audioUrlTranslation
                                audioPathEncontrado = audioPathTranslation
                                tieneAudio = true
                                break
                            }
                        }
                    }

                    if (!tieneAudio) continue

                    val authors = doc.get("authors") as? List<*>
                    val autor = authors
                        ?.mapNotNull { it as? String }
                        ?.joinToString(", ")
                        ?: "Autor desconocido"

                    val libro = Libro(
                        doc.id,
                        doc.getString("title") ?: "Sin título",
                        autor,
                        doc.getString("pdfPath") ?: "",
                        0,
                        R.drawable.logogbsinfondo
                    )

                    val coverPath = doc.getString("coverPath") ?: ""

                    val vista = crearVistaLibroBiblioteca(libro)

                    vista.setOnClickListener {

                        if (audioEncontrado.isNotBlank()) {

                            irAContinuarLeyendoAudioDesdeMenu(
                                libro = libro,
                                audioUrl = audioEncontrado,
                                coverPath = coverPath
                            )

                        } else if (audioPathEncontrado.isNotBlank()) {

                            storage.reference.child(audioPathEncontrado)
                                .downloadUrl
                                .addOnSuccessListener { uri ->

                                    irAContinuarLeyendoAudioDesdeMenu(
                                        libro = libro,
                                        audioUrl = uri.toString(),
                                        coverPath = coverPath
                                    )
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(
                                        this,
                                        "No se pudo cargar el audiolibro: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        }
                    }

                    llAudiolibros.addView(vista)
                }
            }
    }

    private fun irAContinuarLeyendoAudioDesdeMenu(
        libro: Libro,
        audioUrl: String,
        coverPath: String
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        val datos = hashMapOf(
            "idLibro" to libro.idLibro,
            "title" to libro.titulo,
            "authors" to libro.autor.split(",").map { it.trim() },
            "coverPath" to coverPath,
            "audioUrl" to audioUrl,
            "contentType" to "audio",
            "audioPosition" to 0,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        if (uid != null) {
            db.collection("readings")
                .document(uid)
                .set(datos)
        }

        val intent = Intent(this, ContinuarLeyendo::class.java).apply {
            putExtra("idLibro", libro.idLibro)
            putExtra("tituloLibro", libro.titulo)
            putExtra("autorLibro", libro.autor)
            putExtra("portadaStoragePath", coverPath)
            putExtra("portadaResId", libro.portadaResId)
            putExtra("audioUrl", audioUrl)
            putExtra("contentType", "audio")
            putExtra("audioPosition", 0)
        }

        startActivity(intent)
    }
    private fun dp(valor: Int): Int {
        return (valor * resources.displayMetrics.density).toInt()
    }
}