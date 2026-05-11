package com.example.gamevault

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class EditProfileActivity : AppCompatActivity() {

    private var currentUserId: Int = -1
    private lateinit var etNickname: EditText
    private lateinit var etBio: EditText
    private lateinit var imgEditProfile: ImageView
    private var selectedLocalPath: String? = null
    
    private var currentUser: User? = null

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val path = ProfileImageHelper.saveUriToInternalStorage(this, uri)
            if (path != null) {
                selectedLocalPath = path
                Glide.with(this)
                    .load(path)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .circleCrop()
                    .into(imgEditProfile)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        currentUserId = getSharedPreferences("GameVaultPrefs", MODE_PRIVATE).getInt("USER_ID", -1)
        
        etNickname = findViewById(R.id.etNickname)
        etBio = findViewById(R.id.etBio)
        imgEditProfile = findViewById(R.id.imgEditProfile)

        loadCurrentData()

        findViewById<Button>(R.id.btnSaveProfile).setOnClickListener {
            saveProfile()
        }

        findViewById<Button>(R.id.btnChangePhoto).setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    private fun loadCurrentData() {
        val apiService = RetrofitClient.getApiService(this)
        apiService.getUserProfile(currentUserId).enqueue(object : Callback<User> {
            override fun onResponse(call: Call<User>, response: Response<User>) {
                if (response.isSuccessful && response.body() != null) {
                    currentUser = response.body()
                    etNickname.setText(currentUser?.nickname ?: "")
                    etBio.setText(currentUser?.bio ?: "")
                    
                    Glide.with(this@EditProfileActivity)
                        .load(currentUser?.avatar_img)
                        .placeholder(R.drawable.bg_circle_red_stroke)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .circleCrop()
                        .into(imgEditProfile)
                }
            }
            override fun onFailure(call: Call<User>, t: Throwable) {
                Log.e("EDIT_PROFILE", "Error loading user data", t)
            }
        })
    }

    private fun saveProfile() {
        val nickname = etNickname.text.toString().trim()
        val bio = etBio.text.toString().trim()

        // Usamos 'avatar_img' para que coincida con tu nueva columna de la base de datos
        val profileData = mutableMapOf<String, String?>(
            "nickname" to if (nickname.isNotEmpty()) nickname else currentUser?.nickname,
            "bio" to if (bio.isNotEmpty()) bio else currentUser?.bio,
            "avatar_img" to (selectedLocalPath ?: currentUser?.avatar_img)
        )

        val apiService = RetrofitClient.getApiService(this)
        apiService.updateUserProfile(currentUserId, profileData).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@EditProfileActivity, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    val errorMsg = response.errorBody()?.string()
                    Log.e("EDIT_PROFILE", "Server Error: $errorMsg")
                    Toast.makeText(this@EditProfileActivity, "Error: $errorMsg", Toast.LENGTH_LONG).show()
                }
            }
            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                Log.e("EDIT_PROFILE", "Network Error", t)
                Toast.makeText(this@EditProfileActivity, "Network error", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
