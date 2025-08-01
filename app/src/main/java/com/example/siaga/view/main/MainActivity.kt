package com.example.siaga.view.main

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.siaga.databinding.ActivityMainBinding
import com.example.siaga.view.utils.SessionLogin
import com.example.siaga.view.absen.AbsenActivity
import com.example.siaga.view.history.HistoryActivity
import com.example.siaga.view.login.LoginActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var session: SessionLogin

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionLogin(this)

        setInitLayout()
        setupLogout()

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
    private fun setupLogout() {
        binding.imageLogout.setOnClickListener {
            // Buat AlertDialog konfirmasi
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Keluar")
            builder.setMessage("Apakah Anda yakin ingin keluar dari aplikasi?")

            // Tombol "Ya"
            builder.setPositiveButton("Ya") { _, _ ->
                session.logout() // Hapus sesi

                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish() // Tutup MainActivity
            }

            // Tombol "Tidak"
            builder.setNegativeButton("Tidak") { dialog, _ ->
                dialog.dismiss() // Tutup dialog, tetap di MainActivity
            }

            // Tampilkan dialog
            builder.show()
        }
    }
}