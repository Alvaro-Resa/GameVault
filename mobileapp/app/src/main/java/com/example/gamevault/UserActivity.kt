package com.example.gamevault

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Pantalla de perfil del usuario.
 * Muestra información personal, estadísticas, las 3 últimas reseñas realizadas y accesos de configuración.
 * Incluye un footer con información legal y de contacto.
 */
class UserActivity : AppCompatActivity() {

    private var currentUserId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user)

        val sharedPref = getSharedPreferences("GameVaultPrefs", Context.MODE_PRIVATE)
        currentUserId = sharedPref.getInt("USER_ID", -1)

        setupNavigation()
        setupFooter()

        val layoutGuest = findViewById<LinearLayout>(R.id.layoutGuest)
        val layoutAuth = findViewById<LinearLayout>(R.id.layoutAuth)

        if (currentUserId == -1) {
            layoutGuest.visibility = View.VISIBLE
            layoutAuth.visibility = View.GONE
            findViewById<Button>(R.id.btnLogin).setOnClickListener {
                startActivity(Intent(this, LoginActivity::class.java))
            }
        } else {
            layoutGuest.visibility = View.GONE
            layoutAuth.visibility = View.VISIBLE

            findViewById<View>(R.id.btnLogout).setOnClickListener {
                with(sharedPref.edit()) {
                    remove("USER_ID")
                    remove("JWT_TOKEN")
                    apply()
                }
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }

            findViewById<View>(R.id.btnAdmin).setOnClickListener {
                startActivity(Intent(this, AdminActivity::class.java))
            }

            findViewById<View>(R.id.btnEditProfile).setOnClickListener {
                startActivity(Intent(this, EditProfileActivity::class.java))
            }

            findViewById<TextView>(R.id.btnViewAllReviews).setOnClickListener {
                startActivity(Intent(this, AllReviewsActivity::class.java))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (currentUserId != -1) {
            // Pasamos explícitamente la variable asegurando que es Int
            val safeId: Int = currentUserId
            loadUserProfile(safeId)
            loadUserStats(safeId)
            loadUserReviews(safeId)
        }
    }

    private fun setupFooter() {
        findViewById<View>(R.id.btnContact)?.setOnClickListener {
            startActivity(Intent(this, ContactActivity::class.java))
        }
        findViewById<View>(R.id.btnPrivacy)?.setOnClickListener {
            startActivity(Intent(this, PrivacyActivity::class.java))
        }
    }

    private fun loadUserReviews(userId: Int) {
        val apiService = RetrofitClient.getApiService(this)
        apiService.getUserReviews(userId).enqueue(object : Callback<List<Review>> {
            override fun onResponse(call: Call<List<Review>>, response: Response<List<Review>>) {
                if (response.isSuccessful) {
                    val reviews = response.body() ?: emptyList()
                    val sortedReviews = reviews.sortedByDescending { it.id }

                    val recentReviews = sortedReviews.take(3)

                    val rvReviews = findViewById<RecyclerView>(R.id.rvUserReviews)
                    rvReviews.layoutManager = LinearLayoutManager(this@UserActivity)
                    rvReviews.adapter = ReviewAdapter(recentReviews)

                    val btnViewAll = findViewById<TextView>(R.id.btnViewAllReviews)
                    if (sortedReviews.size > 3) {
                        btnViewAll.visibility = View.VISIBLE
                    } else {
                        btnViewAll.visibility = View.GONE
                    }
                }
            }
            override fun onFailure(call: Call<List<Review>>, t: Throwable) {}
        })
    }

    private fun loadUserProfile(userId: Int) {
        val apiService = RetrofitClient.getApiService(this)
        apiService.getUserProfile(userId).enqueue(object : Callback<User> {
            override fun onResponse(call: Call<User>, response: Response<User>) {
                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!

                    // SOLUCIÓN COMPILACIÓN: Tipado estricto a String para evitar errores de nullabilidad
                    val mainName: String = if (!user.nickname.isNullOrBlank()) user.nickname!! else (user.username ?: "Usuario")
                    val safeUsername: String = user.username ?: "usuario"

                    findViewById<TextView>(R.id.txtUsername).text = mainName
                    findViewById<TextView>(R.id.txtUserHandle).text = "@${safeUsername.replace(" ", "").lowercase()}"
                    findViewById<TextView>(R.id.txtUserBio).text = user.bio ?: "No biography set."

                    val imgProfile = findViewById<ImageView>(R.id.imgProfile)
                    val txtInitials = findViewById<TextView>(R.id.txtInitials)

                    if (!user.avatar_img.isNullOrEmpty()) {
                        txtInitials.visibility = View.GONE
                        imgProfile.visibility = View.VISIBLE
                        Glide.with(this@UserActivity)
                            .load(user.avatar_img)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true)
                            .circleCrop()
                            .into(imgProfile)
                    } else {
                        imgProfile.visibility = View.GONE
                        txtInitials.visibility = View.VISIBLE
                        val initials = mainName.split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("")
                        txtInitials.text = if (initials.isNotEmpty()) initials else "U"
                    }

                    if (user.role == "admin") {
                        findViewById<View>(R.id.btnAdmin).visibility = View.VISIBLE
                    }
                }
            }
            override fun onFailure(call: Call<User>, t: Throwable) {}
        })
    }

    private fun loadUserStats(userId: Int) {
        val apiService = RetrofitClient.getApiService(this)
        apiService.getUserStats(userId).enqueue(object : Callback<UserStats> {
            override fun onResponse(call: Call<UserStats>, response: Response<UserStats>) {
                if (response.isSuccessful && response.body() != null) {
                    val st = response.body()!!
                    findViewById<TextView>(R.id.txtStatGames).text = (st.total_games ?: 0).toString()
                    findViewById<TextView>(R.id.txtStatHours).text = (st.total_hours ?: 0).toString()
                    findViewById<TextView>(R.id.txtStatCompleted).text = (st.completed_games ?: 0).toString()
                    findViewById<TextView>(R.id.txtStatReviews).text = (st.total_reviews ?: 0).toString()
                } else {
                    setEmptyStats()
                }
            }
            override fun onFailure(call: Call<UserStats>, t: Throwable) {
                setEmptyStats()
            }
        })
    }

    private fun setEmptyStats() {
        findViewById<TextView>(R.id.txtStatGames).text = "0"
        findViewById<TextView>(R.id.txtStatHours).text = "0"
        findViewById<TextView>(R.id.txtStatCompleted).text = "0"
        findViewById<TextView>(R.id.txtStatReviews).text = "0"
    }

    private fun setupNavigation() {
        findViewById<View>(R.id.btnNavHome).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            overridePendingTransition(0, 0)
            finish()
        }
        findViewById<View>(R.id.btnNavLibrary).setOnClickListener {
            val intent = Intent(this, LibraryActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            overridePendingTransition(0, 0)
            finish()
        }
    }
}