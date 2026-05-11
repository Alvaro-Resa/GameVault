package com.example.gamevault

import retrofit2.Call
import retrofit2.http.*

// --- MODELOS DE DATOS ---
data class LoginRequest(val username: String, val password: String)
data class LoginResponse(val message: String, val userId: Int)
data class OtpRequest(val userId: Int, val otpCode: String)
data class OtpResponse(val success: Boolean, val message: String, val token: String? = null)

data class ReviewPayload(
    val user_id: Int,
    val game_id: Int? = null,
    val manga_id: Int? = null,
    val rating: Int,
    val title: String,
    val content: String
)

@JvmSuppressWildcards
interface ApiService {

    // ==========================================
    // 1. USUARIOS & AUTH
    // ==========================================

    @GET("api/users")
    fun getAllUsers(): Call<List<User>>

    @POST("api/users/login")
    fun loginUser(@Body request: LoginRequest): Call<LoginResponse>

    @POST("api/users/verify-otp")
    fun verifyOtp(@Body request: OtpRequest): Call<OtpResponse>

    @POST("api/users/register")
    fun register(@Body userData: Map<String, String>): Call<Map<String, Any>>

    @GET("api/users/{userId}")
    fun getUserProfile(@Path("userId") userId: Int): Call<User>

    @PATCH("api/users/{userId}")
    fun updateUserProfile(@Path("userId") userId: Int, @Body profileData: Map<String, String?>): Call<Map<String, Any>>

    @GET("api/users/{userId}/stats")
    fun getUserStats(@Path("userId") userId: Int): Call<UserStats>

    @PUT("api/users/{userId}/ban")
    fun toggleUserStatus(
        @Path("userId") userId: Int,
        @Body statusData: Map<String, String>
    ): Call<Map<String, Any>>

    // ==========================================
    // 2. JUEGOS (Catálogo Global)
    // ==========================================
    @GET("api/games") // Para el MainActivity
    fun getAllGames(): Call<List<Game>>

    @GET("api/games/{id}")
    fun getGameById(@Path("id") id: Int): Call<Game>

    @POST("api/games")
    fun createGame(@Body game: Game): Call<Map<String, Any>>

    @PUT("api/games/{id}")
    fun updateGame(@Path("id") id: Int, @Body game: Game): Call<Map<String, Any>>

    @DELETE("api/games/{id}") // Útil para gestión
    fun deleteGame(@Path("id") id: Int): Call<Map<String, Any>>


    // ==========================================
    // 3. MANGAS (Catálogo Global)
    // ==========================================
    @GET("api/mangas") // Para el MainActivity
    fun getAllMangas(): Call<List<Manga>>

    @GET("api/mangas/{id}")
    fun getMangaById(@Path("id") id: Int): Call<Manga>

    @POST("api/mangas")
    fun createManga(@Body manga: Manga): Call<Map<String, Any>>

    @PUT("api/mangas/{id}")
    fun updateManga(@Path("id") id: Int, @Body manga: Manga): Call<Map<String, Any>>

    @DELETE("api/mangas/{id}")
    fun deleteManga(@Path("id") id: Int): Call<Map<String, Any>>


    // ==========================================
    // 4. BIBLIOTECA DEL USUARIO
    // ==========================================
    // Juegos del usuario
    @GET("api/library/{userId}")
    fun getUserLibrary(@Path("userId") userId: Int): Call<List<Game>>

    @POST("api/library")
    fun addToLibrary(@Body data: Map<String, Any>): Call<Map<String, Any>>

    // Mangas del usuario
    @GET("api/mangas/library/{userId}")
    fun getUserMangaLibrary(@Path("userId") userId: Int): Call<List<Manga>>

    @POST("api/mangas/library")
    fun addMangaToLibrary(@Body data: Map<String, Any>): Call<Map<String, Any>>


    // ==========================================
    // 5. RESEÑAS (REVIEWS)
    // ==========================================
    @GET("api/games/{gameId}/reviews")
    fun getReviewsByGame(@Path("gameId") gameId: Int): Call<List<Review>>

    @GET("api/mangas/{mangaId}/reviews") // Añadida por lógica de manga
    fun getReviewsByManga(@Path("mangaId") mangaId: Int): Call<List<Review>>

    @GET("api/users/{userId}/reviews")
    fun getUserReviews(@Path("userId") userId: Int): Call<List<Review>>

    @POST("api/reviews")
    fun postReview(@Body reviewData: ReviewPayload): Call<Map<String, Any>>


    // ==========================================
    // 6. DATOS AUXILIARES (Spinners/Filtros)
    // ==========================================
    @GET("api/authors")
    fun getAllAuthors(): Call<List<Author>>

    @GET("api/publishers") // Útil para el EditManga
    fun getAllPublishers(): Call<List<Publisher>>


}