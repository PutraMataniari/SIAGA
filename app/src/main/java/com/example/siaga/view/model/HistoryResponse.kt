// file: com/example/siaga/view/model/HistoryResponse.kt

package com.example.siaga.view.model

import android.os.Message
import com.example.siaga.view.history.HistoryActivity
import java.text.SimpleDateFormat
import java.util.Locale

data class HistoryResponse(
    val id: Int,
    val jenis: String,
    val nama: String,
    val waktu_absen: String,
    val lokasi: String,
    val gambar: String,
    val jenis_izin: String?,
    val laporan_kinerja: String?,
    val bukti: String?,
    val bukti_asli: String?
)

{
    // ✅ Fungsi ini sekarang bagian dari kelas HistoryResponse
    fun getFormattedDate(): String {
        return try {
            val inputDf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH)
            val outputDf = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("id", "ID"))
            outputDf.format(inputDf.parse(waktu_absen) ?: return waktu_absen)
        } catch (e: Exception) {
            waktu_absen // fallback jika parsing gagal
        }
    }
}