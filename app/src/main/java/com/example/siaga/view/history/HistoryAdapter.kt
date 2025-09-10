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

    private val originalList: MutableList<HistoryResponse> = mutableListOf()

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
//            tvNomor.text = (1 + position).toString()
            tvNama.text = data.pegawai?.nama ?: "_"
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
            layoutStatusIzin.visibility = View.GONE
            layoutCatatanAdmin.visibility = View.GONE

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

                    // tampilkan Jenis Izin
                    layoutJenisIzin.visibility = View.VISIBLE
                    tvJenisIzin.text = data.jenis_izin ?: "-"

                    //Tampilkan Lampiran
                    layoutLampiran.visibility = View.VISIBLE
                    tvLampiran.text = data.bukti_asli ?: "-"

                    // ✅ tampilkan Status Izin
                    layoutStatusIzin.visibility = View.VISIBLE
                    when (data.status?.lowercase()) {
                        "pending" -> {
                            tvStatusIzin.text = "Pending"
                            tvStatusIzin.setTextColor(Color.parseColor("#FFC107")) // kuning
                            tvStatusIzin.setTypeface(null, android.graphics.Typeface.BOLD)
                        }
                        "ditolak" -> {
                            tvStatusIzin.text = "Ditolak"
                            tvStatusIzin.setTextColor(Color.parseColor("#F44336")) // merah
                            tvStatusIzin.setTypeface(null, android.graphics.Typeface.BOLD)
                        }
                        "disetujui" -> {
                            tvStatusIzin.text = "Disetujui"
                            tvStatusIzin.setTextColor(Color.parseColor("#2196F3")) // biru
                            tvStatusIzin.setTypeface(null, android.graphics.Typeface.BOLD)
                        }
                        else -> {
                            tvStatusIzin.text = "Pending"
                            tvStatusIzin.setTextColor(Color.GRAY)
                            tvStatusIzin.setTypeface(null, android.graphics.Typeface.NORMAL)
                        }
                    }
//                    tvStatusIzin.text = data.status?.replaceFirstChar { it.uppercase() } ?: "Pending"

                    // ✅ tampilkan Catatan Admin kalau ada
                    if (!data.catatan_admin.isNullOrEmpty()) {
                        layoutCatatanAdmin.visibility = View.VISIBLE
//                        tvCatatanAdmin.text = data.catatan_admin
                        when (data.status?.lowercase()) {
                            "disetujui" -> {
                                tvCatatanAdmin.text = data.catatan_admin
                                tvCatatanAdmin.setTextColor(Color.parseColor("#4CAF50")) // hijau
                                tvCatatanAdmin.setTypeface(null, android.graphics.Typeface.BOLD)
                            }
                            "ditolak" -> {
                                tvCatatanAdmin.text = data.catatan_admin
                                tvCatatanAdmin.setTextColor(Color.parseColor("#F44336")) // merah
                                tvCatatanAdmin.setTypeface(null, android.graphics.Typeface.BOLD)
                            }
                            else -> {
                                tvCatatanAdmin.text = data.catatan_admin
                                tvCatatanAdmin.setTextColor(Color.DKGRAY) // abu netral
                                tvCatatanAdmin.setTypeface(null, android.graphics.Typeface.NORMAL)
                            }
                        }
                    } else {
                        layoutCatatanAdmin.visibility = View.GONE
                    }

                    // 🔥 Tambahan indikator di pojok kanan atas
//                    indicatorStatus.visibility = View.VISIBLE
//                    when (data.status?.lowercase()) {
//                        "pending" -> indicatorStatus.setBackgroundColor(Color.parseColor("#FFC107")) // kuning
//                        "ditolak" -> indicatorStatus.setBackgroundColor(Color.parseColor("#F44336")) // merah
//                        "disetujui" -> indicatorStatus.setBackgroundColor(Color.parseColor("#2196F3")) // biru
//                        else -> indicatorStatus.setBackgroundColor(Color.GRAY) // default abu
//                    }
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
        originalList.clear()
        originalList.addAll(newList)

        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }

    /**
     * 🔍 Filter data berdasarkan query (jenis, nama, status) & tanggal
     */
    fun filterData(query: String?, tanggalFilter: (HistoryResponse) -> Boolean) {
        items.clear()
        items.addAll(
            originalList.filter { item ->
                val matchQuery = query.isNullOrEmpty() ||
                        item.jenis?.contains(query, ignoreCase = true) == true ||
                        item.pegawai?.nama?.contains(query, ignoreCase = true) == true ||
                        item.status?.contains(query, ignoreCase = true) == true

                val matchTanggal = tanggalFilter(item)

                matchQuery && matchTanggal
            }
        )
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