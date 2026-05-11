package com.example.gamevault

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MangaDetailActivity : AppCompatActivity() {

    private var mangaId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Reutilizamos el layout de detalle de juegos
        setContentView(R.layout.activity_game_detail)

        mangaId = intent.getIntExtra("EXTRA_MANGA_ID", -1)
        if (mangaId == -1) { finish(); return }

        setupLabels()
        fetchMangaDetails()

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
    }

    private fun setupLabels() {
        // Cambiamos los textos estáticos del layout de juegos para que tengan sentido en manga
        findViewById<TextView>(R.id.lblDeveloper).text = "Author(s)"
        findViewById<TextView>(R.id.lblGenre).text = "Demographic & Genres"

        // Ajustamos el hint del progreso
        findViewById<Button>(R.id.btnSaveHours).text = "Save Volumes"
        findViewById<EditText>(R.id.etHoursPlayed).hint = "Volumes read"
    }

    private fun fetchMangaDetails() {
        RetrofitClient.getApiService(this).getMangaById(mangaId).enqueue(object : Callback<Manga> {
            override fun onResponse(call: Call<Manga>, response: Response<Manga>) {
                if (response.isSuccessful && response.body() != null) {
                    val manga = response.body()!!

                    findViewById<TextView>(R.id.txtGameTitle).text = manga.title
                    findViewById<TextView>(R.id.txtDevName).text = manga.author_names ?: "Unknown"
                    findViewById<TextView>(R.id.txtSynopsis).text = manga.synopsis ?: "No synopsis available."
                    findViewById<TextView>(R.id.txtGenresPill).text = manga.display_genres
                    findViewById<TextView>(R.id.txtTopRating).text = "★ ${manga.average_rating ?: "0.0"}"

                    val imageUrl = "http://10.0.2.2:3000/imgBBDD/Mangas/${manga.image}"
                    Glide.with(this@MangaDetailActivity).load(imageUrl).into(findViewById(R.id.imgGameCover))
                }
            }
            override fun onFailure(call: Call<Manga>, t: Throwable) {
                Toast.makeText(this@MangaDetailActivity, "Error loading data", Toast.LENGTH_SHORT).show()
            }
        })
    }
}