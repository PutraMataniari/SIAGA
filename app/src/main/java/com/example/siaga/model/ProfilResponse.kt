package com.example.siaga.model

data class ProfilResponse(
    val success: Boolean,
    val message: String,
    val data: ProfilWrapper
)

data class ProfilWrapper(
    val pegawai: ProfilPegawai,
    val user: ProfilUser
)

data class ProfilPegawai(
    val id: Int,
    val nama: String?,
    val nip: String?,
    val jabatan: String?,
    val bagian: String?,
    val sub_bagian: String?,
    val no_telp: String?,
    val email: String?,
    val tanggal_lahir: String?,
    val foto_profil: String?
)

data class ProfilUser(
    val id: Int,
    val nama: String,
    val email: String
)
