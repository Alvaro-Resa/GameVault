package com.example.gamevault

import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Actividad que muestra la lista completa de reseñas realizadas por el usuario.
 */
class AllReviewsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_reviews)

        val currentUserId = getSharedPreferences("GameVaultPrefs", MODE_PRIVATE).getInt("USER_ID", -1)

        // Botón volver
        findViewById<ImageView>(R.id.btnBackAllReviews).setOnClickListener {
            finish()
        }

        if (currentUserId != -1) {
            loadAllReviews(currentUserId)
        } else {
            Toast.makeText(this, "Error de sesión", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadAllReviews(userId: Int) {
        val apiService = RetrofitClient.getApiService(this)
        val rv = findViewById<RecyclerView>(R.id.rvAllUserReviews)

        // Configuramos el layoutManager aquí una sola vez
        rv.layoutManager = LinearLayoutManager(this)

        apiService.getUserReviews(userId).enqueue(object : Callback<List<Review>> {
            override fun onResponse(call: Call<List<Review>>, response: Response<List<Review>>) {
                if (response.isSuccessful) {
                    val reviews = response.body() ?: emptyList()
                    // Solo actualizamos el adapter cuando llegan los datos
                    rv.adapter = ReviewAdapter(reviews.sortedByDescending { it.id })
                }
            }
            override fun onFailure(call: Call<List<Review>>, t: Throwable) {
                Toast.makeText(this@AllReviewsActivity, "Error de red", Toast.LENGTH_SHORT).show()
            }
        })
    }
}