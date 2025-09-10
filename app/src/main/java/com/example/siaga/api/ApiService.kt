package com.example.siaga.api

import com.example.siaga.model.*
import com.example.siaga.view.model.HistoryResponse
import com.example.siaga.view.model.HistoryWrapper
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

interface ApiService {


    data class UploadConfigResponse(
        val max_upload_size: Long,
        val allowed_image_types: List<String>,
        val allowed_doc_types: List<String>
    )


        @GET("upload-config")
        fun getUploadConfig(): Call<UploadConfigResponse>

    // ================= LOGIN =================
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
        @Part foto_profil: MultipartBody.Part,
        @Part("nama") nama: RequestBody,
        @Part("nip") nip: RequestBody,
        @Part("email") email: RequestBody,
        @Part("no_telp") noTelp: RequestBody,
        @Part("tanggal_lahir") tanggalLahir: RequestBody,
        @Part("jabatan") jabatan: RequestBody,
        @Part("bagian") bagian: RequestBody,
        @Part("sub_bagian") subBagian: RequestBody,
        @Part("password") password: RequestBody,
        @Part("password_confirmation") passwordConfirmation: RequestBody
    ): Call<RegisterResponse>

    // ================= ABSEN MASUK =================
    @Multipart
    @POST("absen/masuk")
    fun absenMasuk(
        @Header("Authorization") token: String,
        @Part("nama") nama: RequestBody,
        @Part gambar: MultipartBody.Part,
        @Part("lokasi") lokasi: RequestBody
    ): Call<AbsensiResponse>

    // ================= ABSEN PULANG =================
    @Multipart
    @POST("absen/pulang")
    fun absenPulang(
        @Header("Authorization") token: String,
        @Part("nama") nama: RequestBody,
        @Part gambar: MultipartBody.Part,
        @Part("lokasi") lokasi: RequestBody,
        @Part("laporan_kinerja") laporanHarian: RequestBody
    ): Call<AbsensiResponse>

    // ================= PERIZINAN =================
    @Multipart
    @POST("absen/izin")
    fun submitIzin(
        @Header("Authorization") token: String,
        @Part("nama") nama: RequestBody,
        @Part("lokasi") lokasi: RequestBody,
        @Part("jenis_izin") jenisIzin: RequestBody,
        @Part gambar: MultipartBody.Part,
        @Part bukti: MultipartBody.Part
//        @Part("bukti_asli") buktiAsli: RequestBody
    ): Call<IzinResponse>


    // ================= GET PROFIL =================
    @GET("profil")
    suspend fun getProfil(
        @Header("Authorization") token: String): ProfilResponse

    // ================= UPDATE PROFIL =================
    @Multipart
    @POST("profil/update")
    suspend fun updateProfil(
        @Header("Authorization") token: String,
        @Part("nama") nama: RequestBody,
        @Part("nip") nip: RequestBody,
        @Part("jabatan") jabatan: RequestBody,
        @Part("bagian") bagian: RequestBody,
        @Part("sub_bagian") subBagian: RequestBody? = null,
        @Part("no_telp") noTelp: RequestBody,
        @Part("email") email: RequestBody,
        @Part("tanggal_lahir") tanggalLahir: RequestBody? = null,
        @Part foto_profil: MultipartBody.Part? = null
    ): ProfilResponse

    // ================= HISTORY ABSEN =================
    @GET("absen/history")
    fun getAllHistory(
        @Header("Authorization") token: String
    ): Call<HistoryWrapper>

    // OPSIONAL - INSERT HISTORY MANUAL
    @POST("insert")
    fun insertData(@Body historyResponse: HistoryResponse): Call<Void>
}
