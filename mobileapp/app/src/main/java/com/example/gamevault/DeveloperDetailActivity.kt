package com.example.gamevault

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DeveloperDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_developer_detail)

        val devId = intent.getIntExtra("DEVELOPER_ID", -1)
        val devName = intent.getStringExtra("DEVELOPER_NAME")

        findViewById<TextView>(R.id.txtDevNameDetail).text = devName
        findViewById<ImageView>(R.id.btnBackDev).setOnClickListener { finish() }

        if (devId != -1) {
            loadDeveloperData(devId)
        }
    }

    private fun loadDeveloperData(id: Int) {
        RetrofitClient.getApiService(this).getDeveloperDetail(id).enqueue(object : Callback<DeveloperResponse> {
            override fun onResponse(call: Call<DeveloperResponse>, response: Response<DeveloperResponse>) {
                if (response.isSuccessful) {
                    val developer = response.body() ?: return

                    val rv = findViewById<RecyclerView>(R.id.rvDevWorks)
                    // Usamos un Grid para mostrar los juegos
                    rv.layoutManager = GridLayoutManager(this@DeveloperDetailActivity, 2)

                    // IMPORTANTE: El adaptador debe recibir la lista de 'works'
                    rv.adapter = GameAdapter(developer.works)
                }
            }
            override fun onFailure(call: Call<DeveloperResponse>, t: Throwable) {
                Toast.makeText(this@DeveloperDetailActivity, "Error de red", Toast.LENGTH_SHORT).show()
            }
        })
    }
}