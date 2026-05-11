package com.example.gamevault

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ReviewAdapter(private val reviews: List<Review>) : RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {

    class ReviewViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtUsername: TextView = view.findViewById(R.id.txtReviewUsername)
        val txtRating: TextView = view.findViewById(R.id.txtReviewRating)
        val txtTitle: TextView = view.findViewById(R.id.txtReviewTitle)
        val txtBody: TextView = view.findViewById(R.id.txtReviewBody)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_review, parent, false)
        return ReviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val review = reviews[position]
        
        // Determinar qué mostrar en el encabezado (Nombre de usuario o Nombre del juego)
        val headerText = review.game_name ?: review.user_name ?: "Anonymous"
        holder.txtUsername.text = headerText
        
        holder.txtRating.text = "★ ${review.rating}/100"
        
        if (review.title.isNullOrEmpty()) {
            holder.txtTitle.visibility = View.GONE
        } else {
            holder.txtTitle.visibility = View.VISIBLE
            holder.txtTitle.text = review.title
        }
        
        holder.txtBody.text = review.content
    }

    override fun getItemCount() = reviews.size
}
