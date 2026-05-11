package com.example.gamevault

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AuthorDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_author_detail)

        val authorId = intent.getIntExtra("AUTHOR_ID", -1)
        val authorName = intent.getStringExtra("AUTHOR_NAME")

        findViewById<TextView>(R.id.txtAuthorNameDetail).text = authorName
        findViewById<ImageView>(R.id.btnBackAuthor).setOnClickListener { finish() }

        if (authorId != -1) {
            loadAuthorData(authorId)
        }
    }

    private fun loadAuthorData(id: Int) {
        RetrofitClient.getApiService(this).getAuthorDetail(id).enqueue(object : Callback<AuthorResponse> {
            override fun onResponse(call: Call<AuthorResponse>, response: Response<AuthorResponse>) {
                if (response.isSuccessful) {
                    val author = response.body() ?: return
                    findViewById<TextView>(R.id.txtAuthorBio).text = author.biography

                    val rv = findViewById<RecyclerView>(R.id.rvAuthorWorks)
                    rv.layoutManager = GridLayoutManager(this@AuthorDetailActivity, 2)

                    // Adaptador de mangas para las obras del autor
                    rv.adapter = MangaAdapter(author.works)
                }
            }
            override fun onFailure(call: Call<AuthorResponse>, t: Throwable) {
                Toast.makeText(this@AuthorDetailActivity, "Error de red", Toast.LENGTH_SHORT).show()
            }
        })
    }
}