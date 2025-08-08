// adapter/HistoryAdapter.kt
package com.example.siaga.view.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.siaga.R
import com.example.siaga.databinding.ListHistoryAbsenBinding
import com.example.siaga.view.model.HistoryResponse

class HistoryAdapter(
    private val items: MutableList<HistoryResponse>
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(val binding: ListHistoryAbsenBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ListHistoryAbsenBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = items[position]
        with(holder.binding) {
            tvNomor.text = data.id.toString()
            tvNama.text = data.nama
            tvNIP.text = data.jenis
            tvLokasi.text = data.lokasi
            tvAbsenTime.text = data.waktuabsen
            tvStatusAbsen.text = data.keterangan

            // ✅ Gunakan holder.itemView.context sebagai context untuk Glide
            try {
                val bitmap = base64ToBitmap(data.gambar)
                if (bitmap != null) {
                    Glide.with(holder.itemView.context) // ✅ Perbaikan di sini
                        .load(bitmap)
                        .placeholder(R.drawable.ic_photo_camera)
                        .into(imageProfile)
                } else {
                    Glide.with(holder.itemView.context)
                        .load(R.drawable.ic_photo_camera)
                        .into(imageProfile)
                }
            } catch (e: Exception) {
                Glide.with(holder.itemView.context)
                    .load(R.drawable.ic_photo_camera)
                    .into(imageProfile)
            }

            val color = when (data.keterangan) {
                "Absen Masuk" -> Color.GREEN
                "Absen Keluar" -> Color.RED
                "Izin" -> Color.BLUE
                else -> Color.GRAY
            }
            colorStatus.setBackgroundColor(color)
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