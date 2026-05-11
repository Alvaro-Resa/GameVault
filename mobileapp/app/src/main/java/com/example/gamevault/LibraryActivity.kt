package com.example.gamevault

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Actividad que gestiona la visualización de la colección personal de juegos y mangas.
 * Incluye pestañas para alternar entre la biblioteca de juegos y la de mangas.
 */
class LibraryActivity : AppCompatActivity() {

    private var currentUserId: Int = -1

    // Datos de juegos
    private var fullGameLibrary: List<Game> = emptyList()
    private var currentGameStatusFilter: String = "All"
    private var currentGameGenreFilter: String = "All"

    // Datos de mangas
    private var fullMangaLibrary: List<Manga> = emptyList()
    private var currentMangaStatusFilter: String = "All"

    // Pestaña activa
    private var currentTab: String = "GAMES"

    private lateinit var pbLibLoading: ProgressBar
    private lateinit var rvUserGames: RecyclerView
    private lateinit var rvStatusFilter: RecyclerView
    private lateinit var rvGenreFilter: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_library)

        val sharedPref = getSharedPreferences("GameVaultPrefs", Context.MODE_PRIVATE)
        currentUserId = sharedPref.getInt("USER_ID", -1)

        pbLibLoading = findViewById(R.id.pbLibLoading)
        rvUserGames = findViewById(R.id.rvUserGames)
        rvStatusFilter = findViewById(R.id.rvStatusFilter)
        rvGenreFilter = findViewById(R.id.rvGenreFilter)
        rvUserGames.layoutManager = LinearLayoutManager(this)

        setupNavigation()
        setupLibraryTabs()

        if (currentUserId == -1) {
            findViewById<View>(R.id.layoutGuestLibrary).visibility = View.VISIBLE
            findViewById<View>(R.id.layoutAuthLibrary).visibility = View.GONE
            findViewById<Button>(R.id.btnLoginLibrary).setOnClickListener {
                startActivity(Intent(this, LoginActivity::class.java))
            }
        } else {
            findViewById<View>(R.id.layoutGuestLibrary).visibility = View.GONE
            findViewById<View>(R.id.layoutAuthLibrary).visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        if (currentUserId != -1) {
            loadUserStats()
            if (currentTab == "GAMES") loadGameLibrary() else loadMangaLibrary()
        }
    }

    private fun setupLibraryTabs() {
        // Buscamos los botones de pestaña en el XML. Si no existen en el layout actual,
        // se añade lógica defensiva con ?. para no crashear.
        val btnTabGames = findViewById<TextView?>(R.id.btnLibTabGames)
        val btnTabMangas = findViewById<TextView?>(R.id.btnLibTabMangas)

        btnTabGames?.setOnClickListener {
            if (currentTab == "GAMES") return@setOnClickListener
            currentTab = "GAMES"
            currentGameStatusFilter = "All"
            currentGameGenreFilter = "All"
            updateLibraryTabUI(btnTabGames, btnTabMangas)
            loadGameLibrary()
        }

        btnTabMangas?.setOnClickListener {
            if (currentTab == "MANGAS") return@setOnClickListener
            currentTab = "MANGAS"
            currentMangaStatusFilter = "All"
            updateLibraryTabUI(btnTabMangas, btnTabGames)
            loadMangaLibrary()
        }
    }

    private fun updateLibraryTabUI(active: TextView?, inactive: TextView?) {
        active?.setTextColor(resources.getColor(R.color.gv_text_primary, theme))
        active?.setBackgroundResource(R.drawable.bg_red_line)
        inactive?.setTextColor(resources.getColor(R.color.gv_text_secondary, theme))
        inactive?.setBackgroundColor(android.graphics.Color.TRANSPARENT)
    }

    private fun loadUserStats() {
        RetrofitClient.getApiService(this).getUserStats(currentUserId).enqueue(object : Callback<UserStats> {
            override fun onResponse(call: Call<UserStats>, response: Response<UserStats>) {
                if (response.isSuccessful && response.body() != null) {
                    val st = response.body()!!
                    findViewById<TextView>(R.id.txtLibGames)?.text = (st.total_games ?: 0).toString()
                    findViewById<TextView>(R.id.txtLibHours)?.text = (st.total_hours ?: 0).toString()
                    findViewById<TextView>(R.id.txtLibCompleted)?.text = (st.completed_games ?: 0).toString()
                    findViewById<TextView>(R.id.txtLibReviews)?.text = (st.total_reviews ?: 0).toString()
                }
            }
            override fun onFailure(call: Call<UserStats>, t: Throwable) {
                Log.e("LIBRARY", "Error al cargar estadísticas", t)
            }
        })
    }

    private fun loadGameLibrary() {
        pbLibLoading.visibility = View.VISIBLE
        RetrofitClient.getApiService(this).getUserLibrary(currentUserId).enqueue(object : Callback<List<Game>> {
            override fun onResponse(call: Call<List<Game>>, response: Response<List<Game>>) {
                pbLibLoading.visibility = View.GONE
                if (response.isSuccessful) {
                    fullGameLibrary = response.body() ?: emptyList()
                    setupGameFilters()
                    filterGames()
                } else {
                    Log.e("RETROFIT_ERROR", "Error biblioteca juegos: ${response.code()} - ${response.errorBody()?.string()}")
                    Toast.makeText(this@LibraryActivity, "Error al obtener juegos", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<List<Game>>, t: Throwable) {
                pbLibLoading.visibility = View.GONE
                Log.e("RETROFIT_ERROR", "Fallo crítico (Juegos): ${t.message}", t)
                Toast.makeText(this@LibraryActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadMangaLibrary() {
        pbLibLoading.visibility = View.VISIBLE
        RetrofitClient.getApiService(this).getUserMangaLibrary(currentUserId).enqueue(object : Callback<List<Manga>> {
            override fun onResponse(call: Call<List<Manga>>, response: Response<List<Manga>>) {
                pbLibLoading.visibility = View.GONE
                if (response.isSuccessful) {
                    fullMangaLibrary = response.body() ?: emptyList()
                    setupMangaFilters()
                    filterMangas()
                } else {
                    Log.e("RETROFIT_ERROR", "Error biblioteca mangas: ${response.code()} - ${response.errorBody()?.string()}")
                    Toast.makeText(this@LibraryActivity, "Error al obtener mangas", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<List<Manga>>, t: Throwable) {
                pbLibLoading.visibility = View.GONE
                Log.e("RETROFIT_ERROR", "Fallo crítico (Mangas): ${t.message}", t)
                Toast.makeText(this@LibraryActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupGameFilters() {
        val statuses = listOf("All", "Playing", "Completed", "Dropped", "Plan to Play")
        rvStatusFilter.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvStatusFilter.adapter = GenreAdapter(statuses) { selected ->
            currentGameStatusFilter = selected
            filterGames()
        }

        val genres = mutableListOf("All")
        genres.addAll(
            fullGameLibrary
                .flatMap { it.genre_name?.split(",")?.map { g -> g.trim() } ?: emptyList() }
                .distinct().sorted()
        )
        rvGenreFilter.visibility = View.VISIBLE
        rvGenreFilter.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvGenreFilter.adapter = GenreAdapter(genres) { selected ->
            currentGameGenreFilter = selected
            filterGames()
        }
    }

    private fun setupMangaFilters() {
        val statuses = listOf("All", "Reading", "Completed", "Dropped", "Plan to Read")
        rvStatusFilter.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvStatusFilter.adapter = GenreAdapter(statuses) { selected ->
            currentMangaStatusFilter = selected
            filterMangas()
        }
        // Ocultar el filtro de género para mangas (usan demographic en vez de genre_name)
        rvGenreFilter.visibility = View.GONE
    }

    private fun filterGames() {
        val filtered = fullGameLibrary.filter { game ->
            val matchesGenre = currentGameGenreFilter == "All" ||
                    (game.genre_name?.contains(currentGameGenreFilter, ignoreCase = true) == true)
            val matchesStatus = currentGameStatusFilter == "All" ||
                    game.status?.lowercase() == currentGameStatusFilter.lowercase()
            matchesGenre && matchesStatus
        }
        rvUserGames.adapter = GameAdapter(filtered)
    }

    private fun filterMangas() {
        val filtered = fullMangaLibrary.filter { manga ->
            currentMangaStatusFilter == "All" ||
                    manga.user_status?.lowercase() == currentMangaStatusFilter.lowercase()
        }
        // Reutilizamos GameAdapter ya que acepta VaultItem (interfaz común)
        rvUserGames.adapter = GameAdapter(filtered)
    }

    private fun setupNavigation() {
        findViewById<View>(R.id.btnNavHomeLib).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }
        findViewById<View>(R.id.btnNavUserLib).setOnClickListener {
            startActivity(Intent(this, UserActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }
    }
}