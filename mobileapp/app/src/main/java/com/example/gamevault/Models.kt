package com.example.gamevault

// Interfaz común para que la UI pueda tratar juegos y mangas por igual
interface VaultItem {
    val id: Int?
    val title: String?
    val release_date: String?
    val image: String?
    val average_rating: String?
    val display_genres: String?
}

data class Game(
    override val id: Int?,
    override val title: String?,
    override val release_date: String?,
    override val image: String?,
    override val average_rating: String? = null,
    val description: String? = null,
    val developer_id: Int? = null,
    val developer_name: String? = null,
    val genre_name: String? = null,
    val rating: Double? = null,
    val status: String? = null,
    val hours_played: Int? = null
) : VaultItem {
    override val display_genres: String? get() = genre_name
}

data class Manga(
    override val id: Int?,
    override val title: String?,
    override val release_date: String?,
    override val image: String?,
    override val average_rating: String? = null,
    val synopsis: String? = null,
    val status: String? = null,       // Estado de publicación del manga (ongoing, completed…)
    val user_status: String? = null,  // Estado del usuario en su biblioteca (reading, completed…)
    val publisher_name: String? = null,
    val author_names: String? = null,
    val genres: String? = null,
    val demographic: String? = null,
    val volumes_read: Int? = 0,
    val primary_author_id: Int? = null
) : VaultItem {
    override val display_genres: String? get() {
        val parts = mutableListOf<String>()
        if (!demographic.isNullOrEmpty()) parts.add(demographic!!)
        if (!genres.isNullOrEmpty()) parts.add(genres!!)
        return if (parts.isEmpty()) "N/A" else parts.joinToString(" • ")
    }
}

data class LibraryPayload(
    val user_id: Int,
    val game_id: Int? = null,
    val manga_id: Int? = null,
    val status: String,
    val hours_played: Int? = null,
    val volumes_read: Int? = null
)

data class AuthorResponse(
    val id: Int,
    val name: String,
    val biography: String?,
    val works: List<Manga> // Lista de sus obras
)

data class DeveloperResponse(
    val id: Int,
    val name: String,
    val works: List<Game>
)

// Añadir a Models.kt
data class UserStats(
    val total_games: Int? = 0,
    val total_hours: Int? = 0,
    val completed_games: Int? = 0,
    val total_reviews: Int? = 0
)

// Modelos de soporte
data class Review(val id: Int, val user_id: Int, val game_id: Int?, val manga_id: Int?, val rating: Int, val title: String?, val content: String?, val user_name: String?, val game_name: String?)
data class User(val id: Int?, val username: String?, val nickname: String?, val email: String?, val role: String?, val state: String?, val bio: String? = null, val avatar_img: String? = null)
data class Author(val id: Int?, val name: String?, val biography: String?)
data class Publisher(val id: Int?, val name: String?, val country: String?)