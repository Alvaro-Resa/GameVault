package com.example.gamevault

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class EditGameActivity : AppCompatActivity() {

    private var itemId: Int = -1
    private var isManga: Boolean = false

    private lateinit var etTitle: EditText
    private lateinit var etCreator: EditText
    private lateinit var etGenres: EditText
    private lateinit var etDesc: EditText
    private lateinit var etDate: EditText
    private lateinit var etImage: EditText
    private lateinit var imgPreview: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_game)

        // Sincronizamos con los nombres de extras que usas en el resto de la app
        itemId = intent.getIntExtra("EXTRA_ID", -1)
        if (itemId == -1) itemId = intent.getIntExtra("EXTRA_GAME_ID", -1)
        if (itemId == -1) itemId = intent.getIntExtra("EXTRA_MANGA_ID", -1)

        isManga = intent.getBooleanExtra("IS_MANGA", false)

        if (itemId == -1) { finish(); return }

        initViews()
        loadData()

        findViewById<Button>(R.id.btnSave).setOnClickListener { saveChanges() }
        findViewById<ImageView>(R.id.btnBack)?.setOnClickListener { finish() }
    }

    private fun initViews() {
        etTitle = findViewById(R.id.etEditTitle)
        etCreator = findViewById(R.id.etEditCreator)
        etGenres = findViewById(R.id.etEditGenres)
        etDesc = findViewById(R.id.etEditDesc)
        etDate = findViewById(R.id.etEditDate)
        etImage = findViewById(R.id.etEditImage)

        // CORRECCIÓN ID: Debe coincidir con el XML (imgEditPreview)
        imgPreview = findViewById(R.id.imgEditPreview)

        if (isManga) {
            findViewById<TextView>(R.id.txtEditHeader).text = "Edit Manga"
            findViewById<TextView>(R.id.lblEditCreator).text = "Author/s"
        }
    }

    private fun loadData() {
        val api = RetrofitClient.getApiService(this)

        // CORRECCIÓN VARIANZA: Separamos las llamadas para que el tipo coincida exactamente
        if (isManga) {
            api.getMangaById(itemId).enqueue(object : Callback<Manga> {
                override fun onResponse(call: Call<Manga>, response: Response<Manga>) {
                    response.body()?.let { populateFields(it) }
                }
                override fun onFailure(call: Call<Manga>, t: Throwable) {}
            })
        } else {
            api.getGameById(itemId).enqueue(object : Callback<Game> {
                override fun onResponse(call: Call<Game>, response: Response<Game>) {
                    response.body()?.let { populateFields(it) }
                }
                override fun onFailure(call: Call<Game>, t: Throwable) {}
            })
        }
    }

    private fun populateFields(item: VaultItem) {
        etTitle.setText(item.title)
        etDate.setText(item.release_date)
        etImage.setText(item.image)

        if (item is Game) {
            etCreator.setText(item.developer_name)
            etGenres.setText(item.genre_name)
            etDesc.setText(item.description)
        } else if (item is Manga) {
            etCreator.setText(item.author_names)
            etGenres.setText(item.genres)
            etDesc.setText(item.synopsis)
        }

        val folder = if (isManga) "Mangas" else "Games"
        imgPreview.visibility = android.view.View.VISIBLE
        Glide.with(this)
            .load("http://10.0.2.2:3000/imgBBDD/$folder/${item.image}")
            .into(imgPreview)
    }

    private fun saveChanges() {
        val api = RetrofitClient.getApiService(this)

        val call = if (isManga) {
            val m = Manga(
                id = itemId,
                title = etTitle.text.toString(),
                release_date = etDate.text.toString(),
                image = etImage.text.toString(),
                synopsis = etDesc.text.toString(),
                author_names = etCreator.text.toString(),
                genres = etGenres.text.toString()
            )
            api.updateManga(itemId, m)
        } else {
            val g = Game(
                id = itemId,
                title = etTitle.text.toString(),
                release_date = etDate.text.toString(),
                image = etImage.text.toString(),
                description = etDesc.text.toString(),
                developer_name = etCreator.text.toString(),
                genre_name = etGenres.text.toString()
            )
            api.updateGame(itemId, g)
        }

        call.enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@EditGameActivity, "Updated successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                Toast.makeText(this@EditGameActivity, "Error saving changes", Toast.LENGTH_SHORT).show()
            }
        })
    }
}