package com.example.gamevault

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.regex.Pattern

// Esta clase maneja la creación de nuevos usuarios en la base de datos
class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val etUsername = findViewById<EditText>(R.id.etRegUsername)
        val etNickname = findViewById<EditText>(R.id.etRegNickname)
        val etEmail = findViewById<EditText>(R.id.etRegEmail)
        val etPassword = findViewById<EditText>(R.id.etRegPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)

        // Usamos la nueva arquitectura centralizada
        val apiService = RetrofitClient.getApiService(this)

        btnRegister.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val nickname = etNickname.text.toString().trim()
            val rawEmail = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // 1. VALIDACIONES BÁSICAS
            if (username.isEmpty() || nickname.isEmpty() || rawEmail.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, getString(R.string.error_empty_fields), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Transformar el correo siempre a minúsculas
            val email = rawEmail.lowercase()

            // 2. SEGURIDAD ANTI SQL-INJECTION / CARACTERES NO PERMITIDOS
            val namePattern = Regex("^[a-zA-Z0-9_-]+$")
            val emailPattern = Regex("^[a-zA-Z0-9_.]+@[a-zA-Z0-9]+\\.[a-zA-Z0-9.]+$")

            if (!namePattern.matches(username)) {
                Toast.makeText(this, "Error: Username only allows letters, numbers, and symbols -, _, *", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (!namePattern.matches(nickname)) {
                Toast.makeText(this, "Error: Nickname only allows letters, numbers, and symbols -, _, *", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (!namePattern.matches(password)) {
                Toast.makeText(this, "Error: Password only allows letters, numbers, and symbols -, _, *", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (!emailPattern.matches(email)) {
                Toast.makeText(this, "Error: Invalid email format. Only letters, numbers, @, _, and dots allowed.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Validación extra de contraseña (mínimo 8 caracteres y 1 de los símbolos permitidos)
            if (password.length < 8) {
                Toast.makeText(this, "Password must be at least 8 characters long.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            
            val requiredSymbolPattern = Pattern.compile("[-_]")
            if (!requiredSymbolPattern.matcher(password).find()) {
                Toast.makeText(this, "Password must contain at least one allowed symbol (- or _)", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // 3. PREPARAR DATOS PARA EL BACKEND (Ya sanitizados y comprobados)
            val userData = mapOf(
                "username" to username,
                "nickname" to nickname,
                "email" to email,
                "password" to password
            )

            // 4. LLAMADA A LA API
            apiService.register(userData).enqueue(object : Callback<Map<String, Any>> {
                override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@RegisterActivity, "Account created successfully!", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        // CHIVATO DE ERRORES: Capturamos la queja de Node.js
                        val errorJson = response.errorBody()?.string()
                        Log.e("REGISTER_ERROR", "Server error: $errorJson")
                        Toast.makeText(this@RegisterActivity, "That username or email is already taken.", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                    Toast.makeText(this@RegisterActivity, "Connection error. Please check your internet and try again.", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}