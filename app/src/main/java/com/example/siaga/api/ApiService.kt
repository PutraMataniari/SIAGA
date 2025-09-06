package com.example.siaga.api

import com.example.siaga.model.AbsensiResponse
import com.example.siaga.model.IzinResponse
import com.example.siaga.model.RegisterResponse
import com.example.siaga.view.model.HistoryResponse
import com.example.siaga.view.model.HistoryWrapper
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

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

// ===================
// Request Signup
// ===================
data class SignupRequest(
    val name: String,
    val email: String,
    val password: String,
    val password_confirmation: String
)

data class ApiResponse(
    val status: Boolean,
    val message: String
)

// Request body untuk absen
data class AbsenRequest(
    val nama: String,
    val tanggal: String,
    val lokasi: String,
    val gambar: String,
    val keterangan: String? = null,
    val laporanHarian: String? = null,
    val lampiran: String? = null // untuk perizinan
)

interface ApiService {

    // Login untuk dapat token
    @FormUrlEncoded
    @POST("login")
    fun login(
        @Field("email") email: String,
        @Field("password") password: String
    ): Call<LoginResponse>

    // ================= SIGNUP =================
    @Multipart
    @POST("register")
    fun register(
        @Part foto_profil: MultipartBody.Part?,
        @Part("nama") nama: RequestBody,
        @Part("nip") nip: RequestBody,
        @Part("email") email: RequestBody,
        @Part("no_telp") noTelp: RequestBody,
        @Part("tanggal_lahir") tanggalLahir: RequestBody,
        @Part("jabatan") jabatan: RequestBody,
        @Part("bagian") bagian: RequestBody,
        @Part("sub_bagian") subBagian: RequestBody,
        @Part("password") password: RequestBody,
    ): Call<RegisterResponse>

    // Absen masuk (dengan header token)
    @Multipart
    @POST("absen/masuk")
    fun absenMasuk(
        @Header("Authorization") token: String,
        @Part gambar: MultipartBody.Part,
        @Part("nama") nama: RequestBody,
        @Part("lokasi") lokasi: RequestBody,
    ): Call<AbsensiResponse>

    //Absen Pulang
    @Multipart
    @POST("absen/pulang")
    fun absenPulang(
        @Header("Authorization") token: String,
        @Part gambar: MultipartBody.Part,
        @Part("nama") nama: RequestBody,
        @Part("lokasi") lokasi: RequestBody,
        @Part("laporan_kinerja") laporanHarian: RequestBody,
    ): Call<AbsensiResponse>


    //Perizinan
    @Multipart
    @POST("absen/izin")
    fun Perizinan(
        @Header("Authorization") token: String,
        @Part("nama") nama: RequestBody,
        @Part("lokasi") lokasi: RequestBody,
        @Part("jenis_izin") jenisIzin: RequestBody,              // Foto selfie wajib
        @Part gambar: MultipartBody.Part,
        @Part bukti: MultipartBody.Part,
        @Part ("bukti_asli") buktiAsli: RequestBody// Wajib (lampiran pendukung)){}
    ): Call<IzinResponse>


    // Dapatkan history absen
//    @GET("absen/history")
//    suspend fun getAllHistory(): Response<HistoryWrapper>
    @GET("absen/history")
    fun getAllHistory(
        @Header("Authorization") token: String
    ): Call<HistoryWrapper>

    // Insert data (opsional, jika tetap dipakai)
    @POST("insert")
    fun insertData(@Body historyResponse: HistoryResponse): Call<Void>

//    fun getHistory(): Response<List<HistoryResponse>>
}