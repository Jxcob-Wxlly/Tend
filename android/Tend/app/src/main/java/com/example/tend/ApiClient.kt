package com.example.tend

import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.IOException
import java.util.concurrent.TimeUnit

interface TendApi {
    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): Response<AuthResponse>

    @GET("api/profile")
    suspend fun getProfile(): Response<Profile>

    @PUT("api/profile")
    suspend fun updateProfile(@Body body: UpdateProfileRequest): Response<Profile>

    @PUT("api/profile/settings")
    suspend fun updateSettings(@Body body: UpdateSettingsRequest): Response<Profile>

    @POST("api/profile/password")
    suspend fun changePassword(@Body body: ChangePasswordRequest): Response<Unit>

    @Multipart
    @POST("api/profile/photo")
    suspend fun uploadPhoto(@Part file: MultipartBody.Part): Response<Profile>

    @GET("api/profile/photo")
    suspend fun getPhoto(): Response<ResponseBody>

    @GET("api/habits")
    suspend fun getHabits(
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("today") today: String
    ): Response<List<Habit>>

    @POST("api/habits")
    suspend fun createHabit(@Body body: CreateHabitRequest): Response<Habit>

    @DELETE("api/habits/{id}")
    suspend fun deleteHabit(@Path("id") id: Int): Response<Unit>

    @POST("api/habits/{id}/toggle")
    suspend fun toggle(@Path("id") id: Int, @Body body: ToggleRequest): Response<ToggleResult>
}

object ApiClient {
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val builder = chain.request().newBuilder()
                val token = Session.token
                if (!token.isNullOrEmpty()) builder.addHeader("Authorization", "Bearer $token")
                chain.proceed(builder.build())
            }
            .build()
    }

    val api: TendApi by lazy {
        Retrofit.Builder()
            .baseUrl(Config.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TendApi::class.java)
    }
}

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int = 0) : ApiResult<Nothing>()
}

/** Runs an API call and turns every outcome (success, API error, no connection) into an ApiResult. */
suspend fun <T> safeCall(block: suspend () -> Response<T>): ApiResult<T> {
    return try {
        val response = block()
        if (response.isSuccessful) {
            @Suppress("UNCHECKED_CAST")
            ApiResult.Success(response.body() as T)
        } else {
            val raw = try { response.errorBody()?.string() ?: "" } catch (e: Exception) { "" }
            val message = try { JSONObject(raw).optString("message") } catch (e: Exception) { "" }
            ApiResult.Error(
                message.ifBlank { "Something went wrong (error ${response.code()})." },
                response.code()
            )
        }
    } catch (e: IOException) {
        ApiResult.Error("Can't reach the server. Check that the API is running and the address in Config.kt is right.")
    } catch (e: Exception) {
        ApiResult.Error("Something went wrong: ${e.message}")
    }
}
