package com.example.siaga.view.history

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.siaga.R
import com.example.siaga.databinding.ListHistoryAbsenBinding
import com.example.siaga.view.model.ModelDatabase
import com.example.siaga.view.utils.BitmapManager.base64ToBitmap

// Definisikan interface di adapter
interface HistoryAdapterCallback {
    fun onDelete(modelDatabase: ModelDatabase)
}

open class HistoryAdapter(
    private val mContext: Context,
    private val mAdapterCallback: HistoryActivity // Gunakan interface di sini
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    private val modelDatabase = mutableListOf<ModelDatabase>()

    fun setData(dataList: List<ModelDatabase>) { // Gunakan metode ini untuk mengatur data
        this.modelDatabase.clear()
        this.modelDatabase.addAll(dataList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ListHistoryAbsenBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = modelDatabase[position]
        with(holder.binding) {
            tvNomor.text = data.uid.toString()
            tvNama.text = data.nama
            tvNIP.text = data.nip
            tvLokasi.text = data.lokasi
            tvAbsenTime.text = data.tanggal
            tvStatusAbsen.text = data.keterangan

            Glide.with(mContext)
                .load(base64ToBitmap(data.fotoSelfie.toString()))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.ic_photo_camera)
                .into(imageProfile)

            val color = when (data.keterangan) {
                "Absen Masuk" -> Color.GREEN
                "Absen Keluar" -> Color.RED
                "Izin" -> Color.BLUE
                else -> Color.GRAY
            }
            colorStatus.setBackgroundResource(R.drawable.bg_circle_radius)
            colorStatus.setBackgroundColor(color)
        }
    }

    override fun getItemCount(): Int {
        return modelDatabase.size
    }

    inner class ViewHolder(val binding: ListHistoryAbsenBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.cvHistory.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    mAdapterCallback.onDelete(modelDatabase[position])
                }
            }
        }
    }
}