package com.example.gamevault

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class GenreAdapter(
    private val genres: List<String>,
    private val onGenreClick: (String) -> Unit
) : RecyclerView.Adapter<GenreAdapter.GenreViewHolder>() {

    private var selectedPos = 0 // By default, "Todos" (index 0) is selected

    class GenreViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtGenre: TextView = view.findViewById(R.id.txtGenre)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenreViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_genre_filter, parent, false)
        return GenreViewHolder(view)
    }

    override fun onBindViewHolder(holder: GenreViewHolder, position: Int) {
        val genre = genres[position]
        holder.txtGenre.text = genre
        holder.txtGenre.isSelected = (position == selectedPos)

        holder.txtGenre.setOnClickListener {
            val oldPos = selectedPos
            selectedPos = holder.adapterPosition
            notifyItemChanged(oldPos)
            notifyItemChanged(selectedPos)
            onGenreClick(genre)
        }
    }

    override fun getItemCount() = genres.size
}
