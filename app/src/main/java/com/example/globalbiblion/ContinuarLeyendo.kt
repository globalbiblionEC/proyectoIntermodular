package com.example.globalbiblion

import android.os.Bundle
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.graphics.BitmapFactory
import com.google.firebase.storage.FirebaseStorage
import kotlin.jvm.java

class ContinuarLeyendo : Bars() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var ivPortadaLibro: ImageView
    private lateinit var tvTituloLibroActual: TextView
    private lateinit var btnEscribirResenia: Button

    private var idLibro: String = ""
    private var pdfUrl: String = ""
    private var portadaStoragePath: String = ""

    private var tituloLibro: String = ""
    private var portadaResId: Int = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_continuar_leyendo)

        configurarTopBar()
        configurarBottomBar()

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        ivPortadaLibro = findViewById(R.id.ivPortadaLibroActual)
        tvTituloLibroActual = findViewById(R.id.tvTituloLibroActual)
        btnEscribirResenia = findViewById(R.id.btnEscribirResena)

        tituloLibro = intent.getStringExtra("tituloLibro") ?: "Libro actual"
        idLibro = intent.getStringExtra("idLibro") ?: ""
        pdfUrl = intent.getStringExtra("pdfUrl") ?: ""
        portadaStoragePath = intent.getStringExtra("portadaStoragePath") ?: ""

        // Respaldo por si falla Storage
        portadaResId = intent.getIntExtra("portadaResId", 0)

        if (idLibro.isBlank()) {
            Toast.makeText(
                this,
                "Aviso: idLibro viene vacío, no se podrá guardar la valoración",
                Toast.LENGTH_LONG
            ).show()
        }

        tvTituloLibroActual.text = tituloLibro

        cargarPortadaDesdeStorage()

        ivPortadaLibro.setOnClickListener {
            if (pdfUrl.isNotBlank()) {
                abrirPdfDesdeUrl()
            } else {
                Toast.makeText(
                    this,
                    "No se ha encontrado la URL del PDF",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        btnEscribirResenia.setOnClickListener {
            val intent = Intent(this, EscribirResenia::class.java).apply {
                putExtra("idLibro", idLibro)
                putExtra("tituloLibro", tituloLibro)
                putExtra("pdfUrl", pdfUrl)
                putExtra("portadaStoragePath", portadaStoragePath)
                putExtra("portadaResId", portadaResId)
            }
            startActivity(intent)
        }

    }

    private fun cargarPortadaDesdeStorage() {
        if (portadaStoragePath.isBlank()) {
            if (portadaResId != 0) {
                ivPortadaLibro.setImageResource(portadaResId)
            }
            return
        }

        storage.reference.child(portadaStoragePath)
            .getBytes(5 * 1024 * 1024)
            .addOnSuccessListener { bytes ->
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ivPortadaLibro.setImageBitmap(bitmap)
            }
            .addOnFailureListener {
                if (portadaResId != 0) {
                    ivPortadaLibro.setImageResource(portadaResId)
                }

                Toast.makeText(
                    this,
                    "No se pudo cargar la portada desde Firebase Storage",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // --------- ABRIR PDF DEL LIBRO DESDE FIREBASE STORAGE ---------

    private fun abrirPdfDesdeUrl() {
        val uri = Uri.parse(pdfUrl)

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            startActivity(Intent.createChooser(intent, "Abrir libro con"))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                this,
                "No hay ninguna app para abrir PDFs",
                Toast.LENGTH_LONG
            ).show()
        }
    }


}