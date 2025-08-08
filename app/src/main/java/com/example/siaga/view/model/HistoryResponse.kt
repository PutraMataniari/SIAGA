// file: com/example/siaga/view/model/HistoryResponse.kt

package com.example.siaga.view.model

import java.text.SimpleDateFormat
import java.util.Locale

data class HistoryResponse(
    val id: Int,
    val jenis: String,
    val nama: String,
    val waktuabsen: String,
    val lokasi: String,
    val gambar: String,
    val keterangan: String,
    val bukti: String
) {
    // ✅ Fungsi ini sekarang bagian dari kelas HistoryResponse
    fun getFormattedDate(): String {
        return try {
            val inputDf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH)
            val outputDf = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("id", "ID"))
            outputDf.format(inputDf.parse(waktuabsen) ?: return waktuabsen)
        } catch (e: Exception) {
            waktuabsen // fallback jika parsing gagal
        }
    }
}