package com.example.gamevault

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class EditMangaActivity : AppCompatActivity() {

    private var mangaId: Int = -1
    private var isEditing = false

    private lateinit var etTitle: EditText
    private lateinit var etSynopsis: EditText
    private lateinit var etReleaseDate: EditText
    private lateinit var etGenres: EditText
    private lateinit var etImage: EditText
    private lateinit var etAuthor: EditText
    private lateinit var btnSave: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_game)

        mangaId = intent.getIntExtra("EXTRA_MANGA_ID", -1)
        isEditing = mangaId != -1

        initViews()

        if (isEditing) loadMangaDetails()

        // Corrección: Usamos View? por seguridad
        findViewById<View>(R.id.btnBack)?.setOnClickListener { finish() }
    }

    private fun initViews() {
        etTitle = findViewById(R.id.etEditTitle)
        etSynopsis = findViewById(R.id.etEditDesc)
        etReleaseDate = findViewById(R.id.etEditDate)
        etGenres = findViewById(R.id.etEditGenres)
        etImage = findViewById(R.id.etEditImage)
        etAuthor = findViewById(R.id.etEditCreator)
        btnSave = findViewById(R.id.btnSave)

        findViewById<TextView>(R.id.txtEditHeader).text = if (isEditing) "Edit Manga" else "New Manga"
        btnSave.text = if (isEditing) "Save Changes" else "Create Manga"

        btnSave.setOnClickListener { saveManga() }
    }

    private fun loadMangaDetails() {
        RetrofitClient.getApiService(this).getMangaById(mangaId).enqueue(object : Callback<Manga> {
            override fun onResponse(call: Call<Manga>, response: Response<Manga>) {
                if (response.isSuccessful && response.body() != null) {
                    val manga = response.body()!!
                    etTitle.setText(manga.title)
                    etSynopsis.setText(manga.synopsis)
                    etReleaseDate.setText(manga.release_date)
                    etGenres.setText(manga.genres)
                    etImage.setText(manga.image)
                    etAuthor.setText(manga.author_names)
                }
            }
            override fun onFailure(call: Call<Manga>, t: Throwable) {}
        })
    }

    private fun saveManga() {
        val title = etTitle.text.toString().trim()
        if (title.isEmpty()) {
            Toast.makeText(this, "Title is required", Toast.LENGTH_SHORT).show()
            return
        }

        val manga = Manga(
            id = if (isEditing) mangaId else null,
            title = title,
            release_date = etReleaseDate.text.toString(),
            image = etImage.text.toString(),
            synopsis = etSynopsis.text.toString(),
            status = null,
            publisher_name = null,
            author_names = etAuthor.text.toString(),
            genres = etGenres.text.toString(),
            demographic = "Shonen"
        )

        val api = RetrofitClient.getApiService(this)
        val call = if (isEditing) api.updateManga(mangaId, manga) else api.createManga(manga)

        call.enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@EditMangaActivity, "Success!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                Toast.makeText(this@EditMangaActivity, "Network Error", Toast.LENGTH_SHORT).show()
            }
        })
    }
}