package com.example.gamevault

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.regex.Pattern

/**
 * Actividad encargada de gestionar el inicio de sesión de los usuarios.
 * Incluye lógica para saltar el login si el token JWT sigue siendo válido.
 */
class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. COMPROBACIÓN DE SESIÓN EXISTENTE (Auto-Login)
        val sharedPref = getSharedPreferences("GameVaultPrefs", Context.MODE_PRIVATE)
        val savedToken = sharedPref.getString("JWT_TOKEN", null)
        val savedUserId = sharedPref.getInt("USER_ID", -1)

        // Si tenemos un token guardado, saltamos directamente a la pantalla principal
        if (savedToken != null && savedUserId != -1) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Cerramos LoginActivity para que no pueda volver atrás
            return
        }

        setContentView(R.layout.activity_login)

        // Referencias a la interfaz
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val pbLoading = findViewById<ProgressBar>(R.id.pbLoading)
        val tvGoToRegister = findViewById<TextView>(R.id.tvGoToRegister)

        // Navegación al registro
        tvGoToRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        val apiService = RetrofitClient.getApiService(this)

        // Lógica del botón de Login
        btnLogin.setOnClickListener {
            val rawUsername = etEmail.text.toString().trim()
            val passwordInput = etPassword.text.toString().trim()

            // Validaciones básicas de campos vacíos
            if (rawUsername.isEmpty() || passwordInput.isEmpty()) {
                Toast.makeText(this, getString(R.string.error_empty_fields), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val sanitizedUsername = rawUsername.lowercase()

            // Validación de username: solo permite caracteres seguros
            val validLoginPattern = Regex("^[a-zA-Z0-9_@.-]+$")
            if (!validLoginPattern.matches(sanitizedUsername)) {
                Toast.makeText(this, "El usuario o email contiene caracteres no permitidos.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // La contraseña puede contener cualquier carácter — no restringir

            // Petición de login al servidor
            btnLogin.visibility = View.INVISIBLE
            pbLoading.visibility = View.VISIBLE

            val request = LoginRequest(username = sanitizedUsername, password = passwordInput)

            apiService.loginUser(request).enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    btnLogin.visibility = View.VISIBLE
                    pbLoading.visibility = View.GONE

                    if (response.isSuccessful && response.body() != null) {
                        val loginResponse = response.body()!!

                        // Comprobamos si el usuario ya verificó OTP recientemente (menos de 30 días)
                        val lastOtpTime = sharedPref.getLong("LAST_OTP_TIME_${loginResponse.userId}", 0L)
                        val currentTime = System.currentTimeMillis()
                        val thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000
                        val oldToken = sharedPref.getString("LAST_TOKEN_${loginResponse.userId}", null)
                        
                        if (currentTime - lastOtpTime < thirtyDaysInMillis && oldToken != null) {
                            // Restauramos sesión automáticamente
                            with(sharedPref.edit()) {
                                putInt("USER_ID", loginResponse.userId)
                                putString("JWT_TOKEN", oldToken)
                                apply()
                            }
                            Toast.makeText(this@LoginActivity, "¡Bienvenido de nuevo!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                            finish()
                            return
                        }

                        // Si no hay sesión previa, vamos a la verificación OTP
                        val intent = Intent(this@LoginActivity, OtpActivity::class.java)
                        intent.putExtra("EXTRA_USER_ID", loginResponse.userId)
                        startActivity(intent)
                        finish()
                    } else {
                        val errorJson = response.errorBody()?.string()
                        Log.e("LOGIN_ERROR", "Error del servidor: $errorJson")
                        Toast.makeText(this@LoginActivity, "Email o contraseña incorrectos.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    btnLogin.visibility = View.VISIBLE
                    pbLoading.visibility = View.GONE
                    Toast.makeText(this@LoginActivity, "Error de conexión.", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}
