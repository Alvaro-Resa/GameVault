package com.example.gamevault

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class MangaAdapter(
    private val mangas: List<Manga>,
    private val layoutId: Int = R.layout.item_game // Reutilizamos tu layout de items
) : RecyclerView.Adapter<MangaAdapter.MangaViewHolder>() {

    class MangaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgManga: ImageView = view.findViewById(R.id.imgItemJuego)
        val txtTitulo: TextView = view.findViewById(R.id.txtItemTitulo)
        val txtDetalles: TextView = view.findViewById(R.id.txtItemDetalles)
        val txtRating: TextView? = view.findViewById(R.id.txtRating)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MangaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return MangaViewHolder(view)
    }

    override fun onBindViewHolder(holder: MangaViewHolder, position: Int) {
        val manga = mangas[position]

        holder.txtTitulo.text = manga.title ?: "Sin título"
        val year = manga.release_date?.take(4) ?: "N/A"

        // CORRECCIÓN: Ahora mostramos el Autor y el año en la lista
        val author = manga.author_names ?: "Autor desconocido"
        holder.txtDetalles.text = "$year - $author"

        if (holder.txtRating != null) {
            val rating = manga.average_rating?.toDoubleOrNull() ?: 0.0
            holder.txtRating.text = "★ %.1f".format(rating)
        }

        // Ruta de imagen corregida para la carpeta /Mangas/
        val imageUrl = "http://10.0.2.2:3000/imgBBDD/Mangas/${manga.image}"

        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .placeholder(android.R.drawable.progress_horizontal)
            .error(android.R.drawable.ic_menu_gallery)
            .into(holder.imgManga)

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, MangaDetailActivity::class.java)
            intent.putExtra("EXTRA_MANGA_ID", manga.id)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount() = mangas.size
}