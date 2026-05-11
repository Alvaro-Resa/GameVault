package com.example.gamevault

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class GameAdapter(
    private val items: List<VaultItem>,
    private val layoutId: Int = R.layout.item_game
) : RecyclerView.Adapter<GameAdapter.GameViewHolder>() {

    class GameViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgItem: ImageView = view.findViewById(R.id.imgItemJuego)
        val txtTitulo: TextView = view.findViewById(R.id.txtItemTitulo)
        val txtDetalles: TextView = view.findViewById(R.id.txtItemDetalles)
        val txtRating: TextView? = view.findViewById(R.id.txtRating)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return GameViewHolder(view)
    }

    override fun onBindViewHolder(holder: GameViewHolder, position: Int) {
        val item = items[position]

        holder.txtTitulo.text = item.title ?: "Sin título"
        val year = item.release_date?.take(4) ?: "N/A"
        holder.txtDetalles.text = "$year - ${item.display_genres ?: "Desconocido"}"

        if (holder.txtRating != null) {
            val ratingValue = item.average_rating?.toDoubleOrNull() ?: 0.0
            holder.txtRating.visibility = View.VISIBLE
            holder.txtRating.text = "★ %.1f".format(ratingValue)
        }

        // --- LÓGICA DE IMAGEN CORREGIDA ---

        // 1. Usamos 'image' que es como se llama en tu interfaz VaultItem y en tu BBDD
        val fileName = item.image ?: ""

        // 2. Determinamos la carpeta (Games o Mangas)
        val folder = if (item is Manga) "Mangas" else "Games"

        // 3. Construimos la URL completa para el servidor local
        val finalUrl = when {
            fileName.isEmpty() -> ""
            fileName.startsWith("http") -> fileName // Por si alguna vez usas URLs externas
            else -> "http://10.0.2.2:3000/imgBBDD/$folder/$fileName"
        }

        Glide.with(holder.itemView.context)
            .load(finalUrl)
            .placeholder(android.R.drawable.progress_horizontal) // Rueda de carga
            .error(android.R.drawable.ic_menu_gallery)           // Icono si falla
            .into(holder.imgItem)

        // --- NAVEGACIÓN ---
        holder.itemView.setOnClickListener {
            val itemId = item.id ?: -1
            if (itemId != -1) {
                val intent = Intent(holder.itemView.context, GameDetailActivity::class.java)
                intent.putExtra("IS_MANGA", item is Manga)
                intent.putExtra("EXTRA_GAME_ID", itemId)
                holder.itemView.context.startActivity(intent)
            }
        }
    }

    override fun getItemCount() = items.size
}