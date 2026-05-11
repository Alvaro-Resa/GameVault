package com.example.gamevault

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class OtpActivity : AppCompatActivity() {

    // Variable global para guardar el ID que recibimos del Login.
    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otp)

        // REFERENCIAS A LA UI
        val etOtpCode = findViewById<EditText>(R.id.etOtpCode)
        val btnVerifyOtp = findViewById<Button>(R.id.btnVerifyOtp)
        val pbOtpLoading = findViewById<ProgressBar>(R.id.pbOtpLoading)

        // RECUPERAR EL ID
        userId = intent.getIntExtra("EXTRA_USER_ID", -1)

        if (userId == -1) {
            // Mensaje amigable si perdemos el ID en el viaje
            Toast.makeText(this, "Oops! Something went wrong. Please log in again.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // CONFIGURACIÓN DE RETROFIT (Usando el nuevo mayordomo centralizado)
        val apiService = RetrofitClient.getApiService(this)

        // ACCIÓN DEL BOTÓN DE VERIFICACIÓN
        btnVerifyOtp.setOnClickListener {
            val otpInput = etOtpCode.text.toString().trim()

            // BARRERA DE VALIDACIÓN
            if (otpInput.length != 6) {
                // Asumo que tu string resource ya es algo como "Please enter a valid 6-digit code"
                Toast.makeText(this, getString(R.string.error_invalid_otp), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ESTADO DE CARGA VISUAL
            btnVerifyOtp.visibility = View.INVISIBLE
            pbOtpLoading.visibility = View.VISIBLE

            // PETICIÓN AL SERVIDOR NODE.JS
            val request = OtpRequest(userId = userId, otpCode = otpInput)

            apiService.verifyOtp(request).enqueue(object : Callback<OtpResponse> {
                override fun onResponse(call: Call<OtpResponse>, response: Response<OtpResponse>) {

                    // Restauramos la interfaz
                    btnVerifyOtp.visibility = View.VISIBLE
                    pbOtpLoading.visibility = View.GONE

                    if (response.isSuccessful && response.body() != null) {
                        val otpResponse = response.body()!!

                        // Verificamos si Node.js nos dijo "success: true"
                        if (otpResponse.success) {

                            // Mensaje de bienvenida cálido
                            Toast.makeText(this@OtpActivity, "Welcome back to GameVault!", Toast.LENGTH_SHORT).show()

                            // GUARDAR SESIÓN Y TOKEN EN LA LIBRETA
                            val sharedPref = getSharedPreferences("GameVaultPrefs", Context.MODE_PRIVATE)
                            with(sharedPref.edit()) {
                                putInt("USER_ID", userId)
                                putLong("LAST_OTP_TIME_${userId}", System.currentTimeMillis())

                                // Guardamos la "pulsera VIP" (el JWT Token)
                                otpResponse.token?.let {
                                    putString("JWT_TOKEN", it)
                                    putString("LAST_TOKEN_${userId}", it)
                                }

                                apply()
                            }

                            // Hacemos el viaje final hacia el Dashboard
                            val intent = Intent(this@OtpActivity, MainActivity::class.java)
                            startActivity(intent)
                            finish()

                        } else {
                            // Mensaje claro si el código caducó o es falso
                            Toast.makeText(this@OtpActivity, "The code is incorrect or has expired. Please try again.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // LA CAJA NEGRA: Guardamos el error real en los logs para el desarrollador
                        val errorJson = response.errorBody()?.string()
                        Log.e("OTP_ERROR", "Server error: $errorJson")

                        // UX: Mostramos un mensaje genérico y educado al usuario
                        Toast.makeText(this@OtpActivity, "Verification failed. Please try again later.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<OtpResponse>, t: Throwable) {
                    // LA CAJA NEGRA: Error de conexión real al log
                    Log.e("OTP_ERROR", "Network failure: ${t.message}")

                    btnVerifyOtp.visibility = View.VISIBLE
                    pbOtpLoading.visibility = View.GONE

                    // UX: Mensaje de error de internet amigable
                    Toast.makeText(this@OtpActivity, "Connection error. Please check your internet and try again.", Toast.LENGTH_LONG).show()
                }
            })
        }
    }
}