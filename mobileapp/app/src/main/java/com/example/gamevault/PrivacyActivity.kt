package com.example.gamevault

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

/**
 * Actividad que muestra la política de privacidad de GameVault.
 */
class PrivacyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_privacy)

        // Botón para volver atrás
        findViewById<ImageView>(R.id.btnBackPrivacy).setOnClickListener {
            finish()
        }
    }
}
