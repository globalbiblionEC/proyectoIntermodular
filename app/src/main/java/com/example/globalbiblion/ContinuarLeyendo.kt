package com.example.globalbiblion
import android.os.Bundle
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri //Para abrir el PDF mediante una URL
import android.graphics.BitmapFactory //Convierte bytes en imágenes para mostrar portadas
import com.google.firebase.storage.FirebaseStorage
import kotlin.jvm.java
import android.media.MediaPlayer //Permite reproducir audio
import android.view.View
import android.widget.SeekBar //Barra de progreso del audio
class ContinuarLeyendo : Bars() {//Heredamos
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var ivPortadaLibro: ImageView
    private lateinit var tvTituloLibroActual: TextView
    private lateinit var btnEscribirResenia: Button
    private lateinit var seekBarAudio: SeekBar //Barra de progreso
    private lateinit var btnPlayPauseAudio: Button
    private lateinit var tvModoLectura: TextView
    private lateinit var tvTiempoActual: TextView
    private lateinit var tvTiempoTotal: TextView
    private var mediaPlayer: MediaPlayer? = null
    private val handler = android.os.Handler(android.os.Looper.getMainLooper()) //Para actualizar continuamente la barra del audio
    private var audioUrl: String = ""
    private var contentType: String = ""
    private var idLibro: String = ""
    private var pdfUrl: String = ""
    private var portadaStoragePath: String = ""
    private var tituloLibro: String = ""
    private var portadaResId: Int = 0
    private var audioPosition: Int = 0

    //Esta activity es para mantener el libro o aduiolibro que esté usando el usuario
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

        seekBarAudio = findViewById(R.id.seekBarAudio)
        btnPlayPauseAudio = findViewById(R.id.btnPlayPauseAudio)
        tvModoLectura = findViewById(R.id.tvModoLectura)

        tvTiempoActual = findViewById(R.id.tvTiempoActual)
        tvTiempoTotal = findViewById(R.id.tvTiempoTotal)

        //Ocultamos controles de audio
        tvModoLectura.visibility = View.GONE
        seekBarAudio.visibility = View.GONE
        btnPlayPauseAudio.visibility = View.GONE
        tvTiempoActual.visibility = View.GONE
        tvTiempoTotal.visibility = View.GONE

        //Variables para los Intents
        audioUrl = intent.getStringExtra("audioUrl") ?: ""
        contentType = intent.getStringExtra("contentType") ?: ""
        tituloLibro = intent.getStringExtra("tituloLibro") ?: "Libro actual"
        idLibro = intent.getStringExtra("idLibro") ?: ""
        pdfUrl = intent.getStringExtra("pdfUrl") ?: ""
        portadaStoragePath = intent.getStringExtra("portadaStoragePath") ?: ""
        portadaResId = intent.getIntExtra("portadaResId", 0)
        audioPosition = intent.getIntExtra("audioPosition", 0)

        if (idLibro.isBlank()) {
            Toast.makeText(
                this,
                getString(R.string.toast_id_libro_vacio),
                Toast.LENGTH_LONG
            ).show()
        }

        tvTituloLibroActual.text = tituloLibro

        cargarPortadaDesdeStorage()

        if (contentType == "audio") {
            configurarModoAudiolibro()
        }

        ivPortadaLibro.setOnClickListener {
            if (pdfUrl.isNotBlank()) {
                abrirPdfDesdeUrl()
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.toast_pdf_url_no_encontrada),
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
            .getBytes(5 * 1024 * 1024)//Descargamos máximo 5MB
            .addOnSuccessListener { bytes ->
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)//Convierto bytes en imagen
                ivPortadaLibro.setImageBitmap(bitmap)//Mostramos la portada
            }
            .addOnFailureListener {
                if (portadaResId != 0) {
                    ivPortadaLibro.setImageResource(portadaResId)
                }

                Toast.makeText(
                    this,
                    getString(R.string.toast_error_portada_storage),
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun abrirPdfDesdeUrl() {
        val uri = Uri.parse(pdfUrl)

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            startActivity(Intent.createChooser(intent, getString(R.string.chooser_abrir_libro)))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                this,
                getString(R.string.toast_no_app_pdf),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun configurarModoAudiolibro() {
        tvModoLectura.visibility = View.VISIBLE
        seekBarAudio.visibility = View.VISIBLE
        btnPlayPauseAudio.visibility = View.VISIBLE
        tvTiempoActual.visibility = View.VISIBLE
        tvTiempoTotal.visibility = View.VISIBLE

        btnEscribirResenia.visibility = View.GONE

        btnPlayPauseAudio.setOnClickListener {
            reproducirOPausarAudio()
        }

        seekBarAudio.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser) {
                        tvTiempoActual.text = formatearTiempo(progress)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    val nuevaPosicion = seekBar?.progress ?: 0
                    mediaPlayer?.seekTo(nuevaPosicion)//Movemos al audio a la nueva posición
                    tvTiempoActual.text = formatearTiempo(nuevaPosicion)
                }
            }
        )
    }

    private fun reproducirOPausarAudio() {
        if (audioUrl.isBlank()) {
            Toast.makeText(this, getString(R.string.toast_no_audiolibro), Toast.LENGTH_LONG).show()
            return
        }

        if (mediaPlayer != null && mediaPlayer!!.isPlaying) {
            mediaPlayer?.pause()
            btnPlayPauseAudio.text = "Reproducir"
            return
        }

        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer().apply {//Creamos el reproductor de audio
                setDataSource(audioUrl)

                setOnPreparedListener {
                    seekBarAudio.max = it.duration //definimos la duración máxima de la barra
                    tvTiempoTotal.text = formatearTiempo(it.duration)

                    if (audioPosition > 0) {
                        it.seekTo(audioPosition)
                        seekBarAudio.progress = audioPosition
                        tvTiempoActual.text = formatearTiempo(audioPosition)
                    }

                    it.start()
                    btnPlayPauseAudio.text = getString(R.string.btn_pausar)
                    actualizarBarraAudio()
                }

                setOnCompletionListener {
                    btnPlayPauseAudio.text = getString(R.string.btn_reproducir)
                    seekBarAudio.progress = 0
                }

                prepareAsync()
            }

            Toast.makeText(this, getString(R.string.toast_cargando_audiolibro), Toast.LENGTH_SHORT).show()
        } else {
            mediaPlayer?.start()
            btnPlayPauseAudio.text = getString(R.string.btn_pausar)
            actualizarBarraAudio()
        }
    }

    private fun actualizarBarraAudio() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                val player = mediaPlayer ?: return

                if (player.isPlaying) {
                    seekBarAudio.progress = player.currentPosition
                    tvTiempoActual.text =
                        formatearTiempo(player.currentPosition)
                    handler.postDelayed(this, 1000)
                }
            }
        }, 1000)
    }

    private fun formatearTiempo(milliseconds: Int): String {

        val totalSegundos = milliseconds / 1000

        val minutos = totalSegundos / 60
        val segundos = totalSegundos % 60

        return String.format("%02d:%02d", minutos, segundos)
    }

    private fun guardarProgresoAudio() {
        val uid = auth.currentUser?.uid ?: return
        val posicion = mediaPlayer?.currentPosition ?: audioPosition

        if (contentType == "audio" && idLibro.isNotBlank()) {
            db.collection("readings")
                .document(uid)
                .update(
                    mapOf(
                        "audioPosition" to posicion,
                        "contentType" to "audio",
                        "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                )
        }
    }

    override fun onPause() {
        super.onPause()
        guardarProgresoAudio()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.release()
        mediaPlayer = null
    }
}