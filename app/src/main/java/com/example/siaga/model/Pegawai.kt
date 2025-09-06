package com.example.siaga.model

data class Pegawai(
    val id: Int,
    val nama: String,
    val nip: String,
    val email: String,
    val no_telp: String,
    val tanggal_lahir: String,
    val jabatan: String,
    val bagian: String,
    val sub_bagian: String,
    val foto_profil: String?
)