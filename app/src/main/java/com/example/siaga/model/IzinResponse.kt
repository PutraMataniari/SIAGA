package com.example.siaga.model

import com.example.siaga.view.model.HistoryResponse

data class IzinResponse(
    val message: String,
    val data: HistoryResponse
)

data class IzinRequest(
    val nama: String,
    val waktu_absen: String,
    val lokasi: String,
    val gambar: String, // base64
    val jenis_izin: String,
    val bukti: String, // base64 atau path file
    val bukti_asli: String?
)

