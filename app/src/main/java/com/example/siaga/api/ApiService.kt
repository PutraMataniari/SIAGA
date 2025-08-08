package com.example.siaga.api

import com.example.siaga.view.model.HistoryResponse
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*
import java.sql.Timestamp

// Model data absen
//data class ModelDatabase(
//    val id: Int,
//    val jenis: String,
//    val nama: String,
//    val waktuabsen: Timestamp,
//    val lokasi: String,
//    val gambar: String,
//    val keterangan: String,
//    val bukti: String
//)

// Response login
data class LoginResponse(
    val user: User,
    val token: String
)

data class User(
    val id: Int,
    val name: String,
    val email: String
)

// Request body untuk absen
data class AbsenRequest(
    val nama: String,
    val tanggal: String,
    val lokasi: String,
    val keterangan: String,
    val gambar: String // base64
)

interface ApiService {

    // Login untuk dapat token
    @FormUrlEncoded
    @POST("login")
    fun login(
        @Field("email") email: String,
        @Field("password") password: String
    ): Call<LoginResponse>

    // Absen masuk (dengan header token)
    @POST("absen/masuk")
    @Headers("Accept: application/json")
    fun absenMasuk(
        @Header("Authorization") token: String,
        @Body request: AbsenRequest
    ): Call<HistoryResponse>

    // Dapatkan history absen
    @GET("absen/history")
    @Headers("Accept: application/json")
    fun getAllHistory(
        @Header("Authorization") token: String
    ): Call<List<HistoryResponse>>

    // Insert data (opsional, jika tetap dipakai)
    @POST("insert")
    fun insertData(@Body historyResponse: HistoryResponse): Call<Void>

//    fun getHistory(): Response<List<HistoryResponse>>
}