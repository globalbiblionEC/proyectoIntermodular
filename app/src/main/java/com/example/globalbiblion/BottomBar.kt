package com.example.globalbiblion
import android.content.Intent
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

//Activity base for reuse
open class BottomBar : AppCompatActivity() {

    //This function must be in every child
    protected fun configurarBottomBar() {

        val llMenuPrincipal = findViewById<LinearLayout?>(R.id.llBotonMenuPrincipal)
        val llCatalogo = findViewById<LinearLayout?>(R.id.llCatalogo)
        val llContinuarLeyendo = findViewById<LinearLayout?>(R.id.llContileyendo)
        val llMicrofono = findViewById<LinearLayout?>(R.id.llMicrofono)

        llMenuPrincipal?.setOnClickListener {
            if (this !is Menuprincipal) {
                startActivity(Intent(this, Menuprincipal::class.java))
            }else{
                Toast.makeText(this, "Querido lector, ya estás en la biblioteca", Toast.LENGTH_SHORT).show()
            }
        }

        llCatalogo?.setOnClickListener {
            if (this !is Biblioteca) {
                startActivity(Intent(this, Biblioteca::class.java))
            }
        }

        llContinuarLeyendo?.setOnClickListener {
            abrirUltimaLectura()
        }

        llMicrofono?.setOnClickListener {
            // AÑADIIIIR
        }
    }

    private fun abrirUltimaLectura() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid == null) {
            Toast.makeText(this, "Debes iniciar sesión", Toast.LENGTH_SHORT).show()
            return
        }

        val db = FirebaseFirestore.getInstance()
        val storage = FirebaseStorage.getInstance()

        db.collection("readings")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    Toast.makeText(this, "No tienes ninguna lectura guardada", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val idLibro = doc.getString("idLibro") ?: ""
                val titulo = doc.getString("title") ?: "Libro actual"
                val authors = doc.get("authors") as? List<*>
                val autor = authors
                    ?.mapNotNull { it as? String }
                    ?.joinToString(", ")
                    ?: ""

                val pdfPath = doc.getString("pdfPath") ?: ""
                val coverPath = doc.getString("coverPath") ?: ""

                if (pdfPath.isBlank()) {
                    Toast.makeText(this, "No se encontró el PDF guardado", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                storage.reference.child(pdfPath).downloadUrl
                    .addOnSuccessListener { pdfUri ->
                        val intent = Intent(this, ContinuarLeyendo::class.java).apply {
                            putExtra("idLibro", idLibro)
                            putExtra("tituloLibro", titulo)
                            putExtra("autorLibro", autor)
                            putExtra("pdfUrl", pdfUri.toString())
                            putExtra("pdfStoragePath", pdfPath)
                            putExtra("portadaStoragePath", coverPath)
                            putExtra("portadaResId", R.drawable.logogbsinfondo)
                        }

                        startActivity(intent)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error PDF: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error Firestore: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}