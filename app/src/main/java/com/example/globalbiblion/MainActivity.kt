package com.example.globalbiblion
//--------------VERSION 29-------------
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var btnIniciarSesion: Button
    private lateinit var btnRegistrarse: Button
    private lateinit var btnInvitado: Button
    //Colocamos linearLayout porque es un botón con imagen
    private lateinit var btnIdiomas: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnIniciarSesion = findViewById(R.id.btnIniciarSesion)
        btnRegistrarse = findViewById(R.id.btnRegistrarse)
        btnInvitado=findViewById(R.id.btnInvitado)
        btnIdiomas=findViewById(R.id.btnIdiomas)

        //Set on click listeners
        btnIdiomas.setOnClickListener {
            try {
                //Intent implicito que ira a la configuración del telefono del usuario
                val intent = Intent(android.provider.Settings.ACTION_LOCALE_SETTINGS)
                startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(android.provider.Settings.ACTION_SETTINGS)
                startActivity(intent)
            }
        }
       btnIniciarSesion.setOnClickListener {
            // Ir a pantalla de Inicio de sesión (Firebase Auth)
            val intent = Intent(this, iniciarSesion::class.java)
            startActivity(intent)
        }

        btnRegistrarse.setOnClickListener {
            // Ir a pantalla de registro (Firebase Firestore)
            val intent = Intent(this, Registro::class.java)
            startActivity(intent)
        }
        //Esta opcion es solo para los invitados
        btnInvitado.setOnClickListener {
            //Cerramos la sesión de cualquier persona que hya iniciado ya
            FirebaseAuth.getInstance().signOut()
            // Ir directamente a la biblioteca, el invitado no puede dar puntuaciones
            val intent = Intent(this, Menuprincipal::class.java)
            intent.putExtra("isGuest", true)
            startActivity(intent)}

    }
}
