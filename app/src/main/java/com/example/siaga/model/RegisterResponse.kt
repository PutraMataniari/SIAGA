package com.example.siaga.model

data class RegisterResponse(
    val status: Boolean,
    val message: String,
    val data: PegawaiData,
    val foto_url: String
)


data class PegawaiData(
    val id: Int,
    val user_id: Int,
    val nama: String,
    val nip: String,
    val email: String,
    val no_telp: String,
    val tanggal_lahir: String,
    val jabatan: String,
    val bagian: String,
    val sub_bagian: String,
    val foto_profil: String
)
