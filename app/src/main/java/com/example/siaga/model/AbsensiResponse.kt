package com.example.siaga.model

data class AbsensiResponse(
    val message: String,
    val data: AbsenData
)

data class AbsenData(
    val id: Int,
    val jenis: String,
    val nama: String,
    val waktu_absen: String,
    val lokasi: String,
    val gambar: String?,
    val laporan_kinerja: String?,
    val created_at: String,
    val updated_at: String
)