package com.example.gamevault

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Actividad que muestra la información de contacto de GameVault.
 */
class ContactActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact)

        // Botón para volver atrás
        findViewById<ImageView>(R.id.btnBackContact).setOnClickListener {
            finish()
        }

        val etMessage = findViewById<EditText>(R.id.etContactMessage)
        val btnSend = findViewById<Button>(R.id.btnSendMessage)

        // Simulación de envío de mensaje
        btnSend.setOnClickListener {
            val message = etMessage.text.toString().trim()
            if (message.isNotEmpty()) {
                Toast.makeText(this, "Gracias por tu mensaje. Nos pondremos en contacto pronto.", Toast.LENGTH_SHORT).show()
                etMessage.text.clear()
            } else {
                Toast.makeText(this, "Por favor, escribe un mensaje antes de enviar.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
