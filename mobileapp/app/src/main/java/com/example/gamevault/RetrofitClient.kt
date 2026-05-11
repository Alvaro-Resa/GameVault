package com.example.gamevault

import android.content.Context
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Cliente centralizado para las peticiones API.
 * Implementa el patrón Singleton para evitar crear múltiples instancias de Retrofit.
 */
object RetrofitClient {

    private const val BASE_URL = "http://10.0.2.2:3000/"
    private var retrofit: Retrofit? = null

    /**
     * Devuelve la instancia del servicio API, configurándola si no existe.
     */
    fun getApiService(context: Context): ApiService {
        if (retrofit == null) {
            // Configuración de tiempos de espera para evitar esperas infinitas
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(AuthInterceptor(context))
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofit!!.create(ApiService::class.java)
    }
}
