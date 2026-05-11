package com.example.gamevault

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    // VARIABLES DE DATOS UNIFICADAS
    // Usamos VaultItem para que quepan tanto Juegos como Mangas
    private var fullList: List<VaultItem> = emptyList()
    private var selectedGenres: MutableList<String> = mutableListOf()
    private var allAvailableGenres: List<String> = emptyList()
    private var currentSearchText: String = ""
    private var currentSortOption: String = "Por defecto"

    private var currentCategory: String = "GAMES"

    // Componentes UI
    private lateinit var rvJuegos: RecyclerView
    private lateinit var rvPopularGames: RecyclerView
    private lateinit var searchBar: EditText
    private lateinit var btnGenreFilter: LinearLayout
    private lateinit var btnSortFilter: LinearLayout
    private lateinit var txtSelectedGenres: TextView
    private lateinit var txtCurrentSort: TextView
    private lateinit var pbMainLoading: ProgressBar
    private lateinit var mainScrollView: NestedScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicialización
        rvJuegos = findViewById(R.id.rvJuegos)
        rvPopularGames = findViewById(R.id.rvPopularGames)
        searchBar = findViewById(R.id.searchBar)
        btnGenreFilter = findViewById(R.id.btnGenreFilter)
        btnSortFilter = findViewById(R.id.btnSortFilter)
        txtSelectedGenres = findViewById(R.id.txtSelectedGenres)
        txtCurrentSort = findViewById(R.id.txtCurrentSort)
        pbMainLoading = findViewById(R.id.pbMainLoading)
        mainScrollView = findViewById(R.id.mainScrollView)

        rvJuegos.layoutManager = LinearLayoutManager(this)
        rvPopularGames.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        setupSearchListener()
        setupNavigation()
        setupGenrePicker()
        setupSortPicker()
        setupCategoryTabs()

        fetchDataFromServer()
    }

    private fun setupCategoryTabs() {
        val btnGames = findViewById<TextView>(R.id.btnTabGames)
        val btnMangas = findViewById<TextView>(R.id.btnTabMangas)

        btnGames.setOnClickListener {
            if (currentCategory == "GAMES") return@setOnClickListener
            currentCategory = "GAMES"
            updateTabUI(btnGames, btnMangas)
            fetchDataFromServer()
        }

        btnMangas.setOnClickListener {
            if (currentCategory == "MANGAS") return@setOnClickListener
            currentCategory = "MANGAS"
            updateTabUI(btnMangas, btnGames)
            fetchDataFromServer()
        }
    }

    private fun updateTabUI(active: TextView, inactive: TextView) {
        active.setTextColor(resources.getColor(R.color.gv_text_primary))
        active.setBackgroundResource(R.drawable.bg_red_line)
        inactive.setTextColor(resources.getColor(R.color.gv_text_secondary))
        inactive.setBackgroundColor(android.graphics.Color.TRANSPARENT)
    }

    /**
     * MÉTODO DE CARGA CORREGIDO:
     * Eliminamos la solución temporal y usamos polimorfismo con VaultItem.
     */
    private fun fetchDataFromServer() {
        pbMainLoading.visibility = View.VISIBLE
        val apiService = RetrofitClient.getApiService(this)

        if (currentCategory == "GAMES") {
            apiService.getAllGames().enqueue(object : Callback<List<Game>> {
                override fun onResponse(call: Call<List<Game>>, response: Response<List<Game>>) {
                    processResponse(response.body())
                }
                override fun onFailure(call: Call<List<Game>>, t: Throwable) {
                    handleFailure(t)
                }
            })
        } else {
            apiService.getAllMangas().enqueue(object : Callback<List<Manga>> {
                override fun onResponse(call: Call<List<Manga>>, response: Response<List<Manga>>) {
                    processResponse(response.body())
                }
                override fun onFailure(call: Call<List<Manga>>, t: Throwable) {
                    handleFailure(t)
                }
            })
        }
    }

    // Procesa la lista sea del tipo que sea (Juego o Manga)
    private fun processResponse(newList: List<VaultItem>?) {
        pbMainLoading.visibility = View.GONE
        mainScrollView.visibility = View.VISIBLE

        if (newList != null) {
            fullList = newList

            // Extraer géneros usando 'display_genres' de la interfaz
            allAvailableGenres = fullList.flatMap {
                it.display_genres?.split(",")?.map { g -> g.trim() } ?: emptyList()
            }.distinct().filter { it.isNotEmpty() }.sorted()

            setupPopularGames()
            filterGames()
        } else {
            Toast.makeText(this, "No se recibieron datos", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleFailure(t: Throwable) {
        pbMainLoading.visibility = View.GONE
        mainScrollView.visibility = View.VISIBLE
        Log.e("RETROFIT_ERROR", "Error: ${t.message}")
        Toast.makeText(this, "Error de conexión", Toast.LENGTH_SHORT).show()
    }

    private fun setupPopularGames() {
        val popularItems = fullList.take(10)
        rvPopularGames.adapter = GameAdapter(popularItems, R.layout.item_game_horizontal)
    }

    private fun setupGenrePicker() {
        btnGenreFilter.setOnClickListener {
            if (allAvailableGenres.isEmpty()) return@setOnClickListener

            val items = allAvailableGenres.toTypedArray()
            val checkedItems = BooleanArray(items.size) { index ->
                selectedGenres.contains(items[index])
            }

            AlertDialog.Builder(this)
                .setTitle("Seleccionar Géneros")
                .setMultiChoiceItems(items, checkedItems) { _, which, isChecked ->
                    if (isChecked) selectedGenres.add(items[which]) else selectedGenres.remove(items[which])
                }
                .setPositiveButton("Aplicar") { _, _ ->
                    txtSelectedGenres.text = if (selectedGenres.isEmpty()) "Todos los géneros" else selectedGenres.joinToString(", ")
                    filterGames()
                }
                .setNeutralButton("Limpiar") { _, _ ->
                    selectedGenres.clear()
                    txtSelectedGenres.text = "Todos los géneros"
                    filterGames()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun setupSortPicker() {
        val sortOptions = arrayOf(
            "Título (A-Z)", "Título (Z-A)",
            "Rating (Mejor valorados)", "Rating (Menor valoración)",
            "Lanzamiento (Nuevos)", "Lanzamiento (Antiguos)",
            "Por defecto"
        )

        btnSortFilter.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Ordenar por")
                .setItems(sortOptions) { _, which ->
                    currentSortOption = sortOptions[which]
                    txtCurrentSort.text = "Orden: $currentSortOption"
                    filterGames()
                }
                .show()
        }
    }

    private fun setupSearchListener() {
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                currentSearchText = s?.toString() ?: ""
                filterGames()
            }
        })
    }

    private fun filterGames() {
        var filteredList = fullList.filter { item ->
            // Usamos display_genres para que funcione con ambos tipos
            val genres = item.display_genres?.split(",")?.map { it.trim() } ?: emptyList()
            val matchesGenre = selectedGenres.isEmpty() || selectedGenres.any { it in genres }
            val matchesSearch = currentSearchText.isEmpty() || (item.title?.contains(currentSearchText, ignoreCase = true) == true)
            matchesGenre && matchesSearch
        }

        filteredList = when (currentSortOption) {
            "Título (A-Z)" -> filteredList.sortedBy { it.title ?: "" }
            "Título (Z-A)" -> filteredList.sortedByDescending { it.title ?: "" }
            "Rating (Mejor valorados)" -> filteredList.sortedByDescending { it.average_rating?.toDoubleOrNull() ?: 0.0 }
            "Rating (Menor valoración)" -> filteredList.sortedBy { it.average_rating?.toDoubleOrNull() ?: 0.0 }
            "Lanzamiento (Nuevos)" -> filteredList.sortedByDescending { it.release_date ?: "" }
            "Lanzamiento (Antiguos)" -> filteredList.sortedBy { it.release_date ?: "9999" }
            else -> filteredList
        }

        rvJuegos.adapter = GameAdapter(filteredList)
    }

    private fun setupNavigation() {
        findViewById<View>(R.id.btnNavUser)?.setOnClickListener {
            startActivity(Intent(this, UserActivity::class.java))
        }
        findViewById<View>(R.id.btnNavLibrary)?.setOnClickListener {
            startActivity(Intent(this, LibraryActivity::class.java))
        }
    }
}