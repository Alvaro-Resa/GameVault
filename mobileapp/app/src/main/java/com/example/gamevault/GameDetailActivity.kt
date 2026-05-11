package com.example.gamevault

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class GameDetailActivity : AppCompatActivity() {

    private var itemId: Int = -1
    private var isManga: Boolean = false
    private var currentUserId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_detail)

        // Recuperamos el ID y el tipo. Intentamos ambos extras por si acaso.
        itemId = intent.getIntExtra("EXTRA_GAME_ID", -1)
        if (itemId == -1) itemId = intent.getIntExtra("EXTRA_MANGA_ID", -1)
        isManga = intent.getBooleanExtra("IS_MANGA", false)

        val sharedPref = getSharedPreferences("GameVaultPrefs", MODE_PRIVATE)
        currentUserId = sharedPref.getInt("USER_ID", -1)

        if (itemId == -1) { finish(); return }

        setupUI()
        fetchData()
    }

    private fun setupUI() {
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        if (isManga) {
            findViewById<TextView>(R.id.lblDeveloper).text = "Author/s:"
            findViewById<TextView>(R.id.lblGenre).text = "Demographic & Genres:"
            findViewById<EditText>(R.id.etHoursPlayed).hint = "Vol. read"
        }

        // SeekBar — actualiza el label de puntuación en tiempo real
        val seekBar = findViewById<SeekBar>(R.id.seekBarRating)
        val txtRatingValue = findViewById<TextView>(R.id.txtRatingValue)
        seekBar.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                txtRatingValue.text = progress.toString()
            }
            override fun onStartTrackingTouch(sb: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(sb: android.widget.SeekBar?) {}
        })

        // Lógica de reseñas (Auth vs Guest)
        val layoutAuth = findViewById<LinearLayout>(R.id.layoutAuthReview)
        val layoutGuest = findViewById<LinearLayout>(R.id.layoutGuestReview)

        if (currentUserId == -1) {
            layoutAuth.visibility = View.GONE
            layoutGuest.visibility = View.VISIBLE
            findViewById<Button>(R.id.btnLoginPrompt).setOnClickListener {
                startActivity(Intent(this, LoginActivity::class.java))
            }
        } else {
            layoutAuth.visibility = View.VISIBLE
            layoutGuest.visibility = View.GONE
            findViewById<Button>(R.id.btnSubmitReview).setOnClickListener { submitReview() }
        }

        findViewById<Button>(R.id.btnAddToLibrary).setOnClickListener { handleStatusClick() }
        findViewById<Button>(R.id.btnSaveHours).setOnClickListener { saveLibraryData() }
    }

    private fun fetchData() {
        val api = RetrofitClient.getApiService(this)

        if (isManga) {
            api.getMangaById(itemId).enqueue(object : Callback<Manga> {
                override fun onResponse(call: Call<Manga>, response: Response<Manga>) {
                    if (response.isSuccessful && response.body() != null) {
                        // Como Manga es un VaultItem, updateUI lo acepta sin problemas
                        updateUI(response.body()!!)
                    }
                }
                override fun onFailure(call: Call<Manga>, t: Throwable) {
                    Toast.makeText(this@GameDetailActivity, "Error al cargar manga", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            api.getGameById(itemId).enqueue(object : Callback<Game> {
                override fun onResponse(call: Call<Game>, response: Response<Game>) {
                    if (response.isSuccessful && response.body() != null) {
                        // Como Game es un VaultItem, updateUI lo acepta sin problemas
                        updateUI(response.body()!!)
                    }
                }
                override fun onFailure(call: Call<Game>, t: Throwable) {
                    Toast.makeText(this@GameDetailActivity, "Error al cargar juego", Toast.LENGTH_SHORT).show()
                }
            })
        }
        fetchReviews()
    }

    private fun updateUI(item: VaultItem) {
        findViewById<TextView>(R.id.txtGameTitle).text = item.title
        findViewById<TextView>(R.id.txtGenresPill).text = item.display_genres

        val txtCreator = findViewById<TextView>(R.id.txtDevName)

        if (item is Game) {
            txtCreator.text = item.developer_name
            findViewById<TextView>(R.id.txtSynopsis).text = item.description

            // Clic para Desarrollador de Juegos
            txtCreator.setOnClickListener {
                val intent = Intent(this, DeveloperDetailActivity::class.java)
                intent.putExtra("DEVELOPER_ID", item.developer_id)
                intent.putExtra("DEVELOPER_NAME", item.developer_name)
                startActivity(intent)
            }
        } else if (item is Manga) {
            txtCreator.text = item.author_names
            findViewById<TextView>(R.id.txtSynopsis).text = item.synopsis

            // Clic para Autor de Mangas
            txtCreator.setOnClickListener {
                val intent = Intent(this, AuthorDetailActivity::class.java)
                intent.putExtra("AUTHOR_ID", item.primary_author_id)
                intent.putExtra("AUTHOR_NAME", item.author_names)
                startActivity(intent)
            }
        }

        val folder = if (isManga) "Mangas" else "Games"
        Glide.with(this)
            .load("http://10.0.2.2:3000/imgBBDD/$folder/${item.image}")
            .into(findViewById(R.id.imgGameCover))
    }

    // Funciones para separar los destinos
    private fun openAuthorDetail(authorId: Int?, authorName: String?) {
        if (authorId == null || authorId == -1) return
        val intent = Intent(this, AuthorDetailActivity::class.java).apply {
            putExtra("AUTHOR_ID", authorId)
            putExtra("AUTHOR_NAME", authorName)
        }
        startActivity(intent)
    }

    private fun openDeveloperDetail(devId: Int?, devName: String?) {
        if (devId == null || devId == -1) return
        val intent = Intent(this, DeveloperDetailActivity::class.java).apply {
            putExtra("DEVELOPER_ID", devId)
            putExtra("DEVELOPER_NAME", devName)
        }
        startActivity(intent)
    }

    private fun fetchLibraryState() {
        val api = RetrofitClient.getApiService(this)
        if (isManga) {
            api.getUserMangaLibrary(currentUserId).enqueue(object : Callback<List<Manga>> {
                override fun onResponse(call: Call<List<Manga>>, response: Response<List<Manga>>) {
                    val entry = response.body()?.find { it.id == itemId }
                    if (entry != null) {
                        // user_status es el estado de lectura del usuario (Reading, Completed…)
                        val displayStatus = entry.user_status ?: "Status"
                        findViewById<Button>(R.id.btnAddToLibrary).text = displayStatus
                        findViewById<EditText>(R.id.etHoursPlayed).setText(entry.volumes_read?.toString() ?: "0")
                    }
                }
                override fun onFailure(call: Call<List<Manga>>, t: Throwable) {}
            })
        } else {
            api.getUserLibrary(currentUserId).enqueue(object : Callback<List<Game>> {
                override fun onResponse(call: Call<List<Game>>, response: Response<List<Game>>) {
                    val entry = response.body()?.find { it.id == itemId }
                    if (entry != null) {
                        findViewById<Button>(R.id.btnAddToLibrary).text = entry.status
                        findViewById<EditText>(R.id.etHoursPlayed).setText(entry.hours_played.toString())
                    }
                }
                override fun onFailure(call: Call<List<Game>>, t: Throwable) {}
            })
        }
    }

    private fun saveLibraryData() {
        val statusText = findViewById<Button>(R.id.btnAddToLibrary).text.toString().lowercase()
        val value = findViewById<EditText>(R.id.etHoursPlayed).text.toString().toIntOrNull() ?: 0

        val payload = LibraryPayload(
            user_id = currentUserId,
            status = statusText,
            game_id = if (isManga) null else itemId,
            manga_id = if (isManga) itemId else null,
            hours_played = if (isManga) null else value,
            volumes_read = if (isManga) value else null
        )

        val api = RetrofitClient.getApiService(this)
        val call = if (isManga) api.addMangaToLibrary(payload) else api.addToLibrary(payload)

        call.enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@GameDetailActivity, "Saved to Library!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@GameDetailActivity, "Server Error", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                Toast.makeText(this@GameDetailActivity, "Network Error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun submitReview() {
        val rating = findViewById<SeekBar>(R.id.seekBarRating).progress
        val title = findViewById<EditText>(R.id.etReviewTitle).text.toString()
        val content = findViewById<EditText>(R.id.etReviewBody).text.toString()

        if (content.isBlank()) {
            Toast.makeText(this, "Please write a review before posting", Toast.LENGTH_SHORT).show()
            return
        }

        val payload = ReviewPayload(
            user_id = currentUserId,
            game_id = if (isManga) null else itemId,
            manga_id = if (isManga) itemId else null,
            rating = rating,
            title = title,
            content = content
        )

        RetrofitClient.getApiService(this).postReview(payload).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@GameDetailActivity, "Review posted!", Toast.LENGTH_SHORT).show()
                    // Limpiar campos
                    findViewById<EditText>(R.id.etReviewTitle).text.clear()
                    findViewById<EditText>(R.id.etReviewBody).text.clear()
                    fetchReviews()
                } else {
                    Toast.makeText(this@GameDetailActivity, "Error posting review", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                Toast.makeText(this@GameDetailActivity, "Network error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchReviews() {
        val api = RetrofitClient.getApiService(this)
        val call = if (isManga) api.getReviewsByManga(itemId) else api.getReviewsByGame(itemId)

        call.enqueue(object : Callback<List<Review>> {
            override fun onResponse(call: Call<List<Review>>, response: Response<List<Review>>) {
                if (response.isSuccessful) {
                    val reviews = response.body() ?: emptyList()
                    val rvReviews = findViewById<RecyclerView>(R.id.rvReviews)
                    rvReviews.layoutManager = LinearLayoutManager(this@GameDetailActivity)
                    rvReviews.adapter = ReviewAdapter(reviews)
                    // Actualizar el contador de reseñas
                    findViewById<TextView>(R.id.txtReviewCount)?.text = "${reviews.size} reviews"
                }
            }
            override fun onFailure(call: Call<List<Review>>, t: Throwable) {}
        })
    }

    private fun handleStatusClick() {
        val options = if (isManga) arrayOf("Reading", "Completed", "Dropped", "Plan to Read")
        else arrayOf("Playing", "Completed", "Dropped", "Plan to Play")
        AlertDialog.Builder(this).setTitle("Select Status").setItems(options) { _, which ->
            findViewById<Button>(R.id.btnAddToLibrary).text = options[which]
        }.show()
    }
}