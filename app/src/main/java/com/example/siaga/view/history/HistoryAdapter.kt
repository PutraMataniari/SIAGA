// adapter/HistoryAdapter.kt
package com.example.siaga.view.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.siaga.R
import com.example.siaga.databinding.ListHistoryAbsenBinding
import com.example.siaga.view.model.HistoryResponse

object Constants {
    const val BASE_URL_IMAGE = "http://192.168.1.11:8000/storage/"
}



class HistoryAdapter(
    private val items: MutableList<HistoryResponse>
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(val binding: ListHistoryAbsenBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ListHistoryAbsenBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = items[position]
        with(holder.binding) {
            tvNomor.text = (1 + position).toString()
            tvNama.text = data.nama
//            tvLaporan.text = data.laporan_kinerja
            tvLokasi.text = data.lokasi
            tvAbsenTime.text = data.waktu_absen
//            tvStatusAbsen.text = data.jenis
            tvStatusAbsen.text = data.jenis?.replaceFirstChar { it.uppercase() } ?: "-"

            // ✅ Gunakan holder.itemView.context sebagai context untuk Glide
            // Load gambar dari server
            val imageUrl = Constants.BASE_URL_IMAGE + (data.gambar ?: "")
            Glide.with(holder.itemView.context)
                .load(imageUrl)
                .placeholder(R.drawable.ic_photo_camera)
                .error(R.drawable.ic_broken_image)
                .into(imageProfile)

            // reset dulu semua supaya nggak kebawa
            layoutLaporan.visibility = View.GONE
            layoutJenisIzin.visibility = View.GONE
            layoutLampiran.visibility = View.GONE

            when (data.jenis?.lowercase()) {
                "masuk" -> {
                    colorStatus.setCardBackgroundColor(Color.parseColor("#4CAF50"))

                    // hanya tampil Nama, Lokasi, Waktu
//                    tvLaporan.visibility = View.GONE
//                    tvJenisIzin.visibility = View.GONE
//                    tvLampiran.visibility = View.GONE
                }

                "pulang" -> {
                    colorStatus.setCardBackgroundColor(Color.parseColor("#F44336"))

                    // tampil Laporan kinerja
                    layoutLaporan.visibility = View.VISIBLE
                    tvLaporan.text = data.laporan_kinerja ?: "-"
//                    tvJenisIzin.visibility = View.GONE
//                    tvLampiran.visibility = View.GONE
                }
                "izin" -> {
                    colorStatus.setCardBackgroundColor(Color.parseColor("#2196F3"))

                    // tampil Jenis Izin + Lampiran
                    layoutJenisIzin.visibility = View.VISIBLE
                    tvJenisIzin.text = data.jenis_izin ?: "-"
                    layoutLampiran.visibility = View.VISIBLE
                    tvLampiran.text = data.bukti_asli ?: "-"
//                    tvLaporan.visibility = View.GONE
                }
                else -> {
                    colorStatus.setCardBackgroundColor(Color.GRAY)
                }
            }
//            colorStatus.setBackgroundColor(color)
        }
    }

    override fun getItemCount() = items.size

    fun setData(newList: List<HistoryResponse>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }

    // Fungsi bantu untuk konversi base64 ke Bitmap
    private fun base64ToBitmap(base64String: String): android.graphics.Bitmap? {
        return try {
            val bytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)
            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }
}