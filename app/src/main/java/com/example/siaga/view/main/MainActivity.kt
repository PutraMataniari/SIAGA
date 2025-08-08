package com.example.siaga.view.main

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.siaga.databinding.ActivityMainBinding
import com.example.siaga.datastore.DataStoreManager
import com.example.siaga.view.absen.AbsenActivity
import com.example.siaga.view.history.HistoryActivity
import com.example.siaga.view.profil.ProfilActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var dataStoreManager: DataStoreManager
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Inisialisasi DataStoreManager
        dataStoreManager = DataStoreManager(this)

        setInitLayout()
        setupProfile()

        if ("mediatek".equals(Build.MANUFACTURER, ignoreCase = true)) {
            try {
                val factory = MsyncFactory.getInstance()
                // Lanjutkan logika dengan factory
            } catch (e: ClassNotFoundException) {
                Log.e("MainActivity", "MsyncFactory not found", e)
            }
        }
    }

    private fun setInitLayout() {
        binding.cvAbsenMasuk.setOnClickListener {
            val intent = Intent(this, AbsenActivity::class.java)
            intent.putExtra(AbsenActivity.DATA_TITLE, "Absen Masuk")
            startActivity(intent)
        }

        binding.cvAbsenKeluar.setOnClickListener {
            val intent = Intent(this, AbsenActivity::class.java)
            intent.putExtra(AbsenActivity.DATA_TITLE, "Absen Pulang")
            startActivity(intent)
        }

        binding.cvPerizinan.setOnClickListener {
            val intent = Intent(this, AbsenActivity::class.java)
            intent.putExtra(AbsenActivity.DATA_TITLE, "Izin")
            startActivity(intent)
        }

        binding.cvHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
    }

    /**
     * Setup logout button with confirmation dialog
     */
    private fun setupProfile() {
        binding.imageProfile.setOnClickListener {
            // Langsung buka ProfileActivity tanpa konfirmasi
            val intent = Intent(this, ProfilActivity::class.java)
            startActivity(intent)
        }
    }
}


