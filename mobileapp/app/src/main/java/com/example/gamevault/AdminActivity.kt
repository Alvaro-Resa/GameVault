package com.example.gamevault

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Actividad de administración para gestionar juegos y usuarios.
 * Incluye un sistema de pestañas para alternar entre las listas y un buscador en tiempo real.
 */
class AdminActivity : AppCompatActivity() {

    private lateinit var rvAdminContent: RecyclerView
    private lateinit var tabGames: TextView
    private lateinit var tabUsers: TextView
    private lateinit var etAdminSearch: EditText

    private var fullGameList: List<Game> = emptyList()
    private var fullUserList: List<User> = emptyList()
    private var currentTab: String = "GAMES"
    private var currentSearchQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        // Inicializar vistas
        rvAdminContent = findViewById(R.id.rvAdminContent)
        tabGames = findViewById(R.id.tabGames)
        tabUsers = findViewById(R.id.tabUsers)
        etAdminSearch = findViewById(R.id.etAdminSearch)
        rvAdminContent.layoutManager = LinearLayoutManager(this)

        // Configurar navegación y pestañas
        findViewById<ImageView>(R.id.btnBackAdmin).setOnClickListener { finish() }

        tabGames.setOnClickListener { switchTab("GAMES") }
        tabUsers.setOnClickListener { switchTab("USERS") }

        findViewById<View>(R.id.fabAddGame).setOnClickListener {
            Toast.makeText(this, "Funcionalidad de añadir juego próximamente", Toast.LENGTH_SHORT).show()
        }

        // Configurar el buscador
        setupSearchListener()

        // Carga inicial
        switchTab("GAMES")
    }

    override fun onResume() {
        super.onResume()
        if (currentTab == "GAMES") loadGames() else loadUsers()
    }

    /**
     * Alterna entre las pestañas actualizando la UI y reiniciando el filtro de búsqueda.
     */
    private fun switchTab(tab: String) {
        currentTab = tab
        currentSearchQuery = ""
        etAdminSearch.text.clear()

        if (tab == "GAMES") {
            tabGames.setTextColor(getColor(R.color.gv_red_glow))
            tabUsers.setTextColor(getColor(R.color.gv_text_secondary))
            findViewById<View>(R.id.fabAddGame).visibility = View.VISIBLE
            loadGames()
        } else {
            tabUsers.setTextColor(getColor(R.color.gv_red_glow))
            tabGames.setTextColor(getColor(R.color.gv_text_secondary))
            findViewById<View>(R.id.fabAddGame).visibility = View.GONE
            loadUsers()
        }
    }

    /**
     * Configura el listener del buscador para filtrar las listas en tiempo real.
     */
    private fun setupSearchListener() {
        etAdminSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                currentSearchQuery = s?.toString()?.trim() ?: ""
                applyFilter()
            }
        })
    }

    /**
     * Filtra la lista actual según el texto introducido en el buscador.
     */
    private fun applyFilter() {
        if (currentTab == "GAMES") {
            // SOLUCIÓN: Usamos ?. y == true para evitar que explote si el título es nulo
            val filtered = fullGameList.filter {
                it.title?.contains(currentSearchQuery, ignoreCase = true) == true
            }
            rvAdminContent.adapter = AdminGameAdapter(filtered)
        } else {
            val filtered = fullUserList.filter {
                // SOLUCIÓN: Protegemos username, nickname y email contra nulos
                val name = it.nickname ?: it.username ?: ""
                name.contains(currentSearchQuery, ignoreCase = true) ||
                        (it.email?.contains(currentSearchQuery, ignoreCase = true) == true)
            }
            rvAdminContent.adapter = AdminUserAdapter(filtered)
        }
    }

    private fun loadGames() {
        val apiService = RetrofitClient.getApiService(this)
        apiService.getAllGames().enqueue(object : Callback<List<Game>> {
            override fun onResponse(call: Call<List<Game>>, response: Response<List<Game>>) {
                if (response.isSuccessful) {
                    fullGameList = response.body() ?: emptyList()
                    applyFilter()
                }
            }
            override fun onFailure(call: Call<List<Game>>, t: Throwable) {
                Toast.makeText(this@AdminActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadUsers() {
        val apiService = RetrofitClient.getApiService(this)
        apiService.getAllUsers().enqueue(object : Callback<List<User>> {
            override fun onResponse(call: Call<List<User>>, response: Response<List<User>>) {
                if (response.isSuccessful) {
                    fullUserList = response.body() ?: emptyList()
                    applyFilter()
                }
            }
            override fun onFailure(call: Call<List<User>>, t: Throwable) {
                Toast.makeText(this@AdminActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // --- ADAPTADORES INTERNOS ---

    inner class AdminGameAdapter(private val games: List<Game>) : RecyclerView.Adapter<AdminGameAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val img = v.findViewById<ImageView>(R.id.imgAdminGame)
            val title = v.findViewById<TextView>(R.id.txtAdminGameTitle)
            val dev = v.findViewById<TextView>(R.id.txtAdminGameDev)
            val btnEdit = v.findViewById<View>(R.id.btnAdminEditGame)
            val btnDelete = v.findViewById<View>(R.id.btnAdminDeleteGame)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(LayoutInflater.from(parent.context).inflate(R.layout.item_admin_game, parent, false))
        override fun onBindViewHolder(holder: VH, position: Int) {
            val g = games[position]
            holder.title.text = g.title
            holder.dev.text = g.developer_name
            Glide.with(this@AdminActivity).load(g.image).into(holder.img)

            // SOLUCIÓN: Aseguramos que el ID sea Int (si viene nulo, pasamos 0 por seguridad)
            val safeGameId = g.id ?: 0

            holder.btnEdit.setOnClickListener {
                val intent = Intent(this@AdminActivity, EditGameActivity::class.java)
                intent.putExtra("EXTRA_GAME_ID", safeGameId)
                startActivity(intent)
            }

            holder.btnDelete.setOnClickListener {
                AlertDialog.Builder(this@AdminActivity)
                    .setTitle("Eliminar Juego")
                    .setMessage("¿Estás seguro de que quieres borrar ${g.title ?: "este juego"}?")
                    .setPositiveButton("Sí") { _, _ -> deleteGame(safeGameId) }
                    .setNegativeButton("No", null)
                    .show()
            }
        }
        override fun getItemCount() = games.size
    }

    inner class AdminUserAdapter(private val users: List<User>) : RecyclerView.Adapter<AdminUserAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val name = v.findViewById<TextView>(R.id.txtAdminUserName)
            val email = v.findViewById<TextView>(R.id.txtAdminUserEmail)
            val role = v.findViewById<TextView>(R.id.txtAdminRole)
            val state = v.findViewById<TextView>(R.id.txtAdminState)
            val btnBan = v.findViewById<Button>(R.id.btnAdminToggleBan)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(LayoutInflater.from(parent.context).inflate(R.layout.item_admin_user, parent, false))
        override fun onBindViewHolder(holder: VH, position: Int) {
            val u = users[position]
            holder.name.text = u.nickname ?: u.username
            holder.email.text = u.email

            // SOLUCIÓN: Protegemos el uppercase() por si el rol viene nulo
            holder.role.text = u.role?.uppercase() ?: "USER"

            val isActive = u.state == "1" || u.state == "active"
            holder.state.text = if (isActive) "ACTIVE" else "BANNED"
            holder.state.setTextColor(if (isActive) getColor(android.R.color.holo_green_dark) else getColor(android.R.color.holo_red_dark))

            // SOLUCIÓN: Aseguramos que el ID sea Int
            val safeUserId = u.id ?: 0

            holder.btnBan.text = if (isActive) "Ban" else "Unban"
            holder.btnBan.setOnClickListener { toggleUserBan(safeUserId) }
        }
        override fun getItemCount() = users.size
    }

    private fun deleteGame(id: Int) {
        RetrofitClient.getApiService(this).deleteGame(id).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@AdminActivity, "Juego eliminado", Toast.LENGTH_SHORT).show()
                    loadGames()
                }
            }
            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {}
        })
    }

    private fun toggleUserBan(id: Int) {
        // El endpoint PUT /api/users/:id/ban simplemente hace toggle del estado sin necesitar body
        RetrofitClient.getApiService(this).toggleUserStatus(id, emptyMap()).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@AdminActivity, "Estado de usuario actualizado", Toast.LENGTH_SHORT).show()
                    loadUsers()
                } else {
                    Toast.makeText(this@AdminActivity, "Error al actualizar estado", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                Toast.makeText(this@AdminActivity, "Fallo de red", Toast.LENGTH_SHORT).show()
            }
        })
    }
}