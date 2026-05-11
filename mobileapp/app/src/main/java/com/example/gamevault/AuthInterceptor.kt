package com.example.gamevault

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import okhttp3.Interceptor
import okhttp3.Response

// Pasamos el Context en el constructor para poder abrir la libreta y lanzar pantallas
class AuthInterceptor(private val context: Context) : Interceptor {

    private val sharedPreferences = context.getSharedPreferences("GameVaultPrefs", Context.MODE_PRIVATE)

    override fun intercept(chain: Interceptor.Chain): Response {
        // 1. Atrapamos la petición original antes de que salga hacia internet
        val originalRequest = chain.request()

        // 2. Buscamos el token en nuestra libreta
        val token = sharedPreferences.getString("JWT_TOKEN", null)

        // 3. Preparamos una copia de la petición para poder modificarla
        val requestBuilder = originalRequest.newBuilder()

        // 4. Si tenemos el token, se lo pegamos en la cabecera (Header)
        if (token != null) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        // 5. Enviamos la petición hacia Node.js y guardamos su RESPUESTA
        val response = chain.proceed(requestBuilder.build())

        // 6. BARRERA DE CADUCIDAD: Si Node.js nos da un portazo (401 o 403)
        // Solo lo hacemos si NO estamos en las rutas de login, registro o verify-otp.
        val path = originalRequest.url().encodedPath()
        val isAuthEndpoint = path.contains("/login") || path.contains("/verify-otp") || path.contains("/register")
        
        if (!isAuthEndpoint && (response.code() == 401 || response.code() == 403)) {

            // Borramos los datos de sesión (ID y Token) de SharedPreferences
            with(sharedPreferences.edit()) {
                clear() // Borra todo el contenido
                apply()
            }

            // El Interceptor trabaja en un hilo de fondo (Background Thread).
            // Para cambiar de pantalla y mostrar un Toast, debemos "saltar" al hilo principal (UI Thread).
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "Session expired. Please log in again.", Toast.LENGTH_LONG).show()

                // Preparamos el viaje hacia la pantalla de Login
                val intent = Intent(context, LoginActivity::class.java)

                // Estas banderas son vitales: Borran todo el historial de pantallas (el Dashboard)
                // para que el usuario no pueda pulsar el botón de "Atrás" en su móvil y saltarse el login.
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                context.startActivity(intent)
            }
        }

        // 7. Devolvemos la respuesta para que Retrofit continúe su flujo normal
        return response
    }
}