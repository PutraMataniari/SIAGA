package com.example.siaga.view.main

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.siaga.databinding.ActivityMainBinding
import com.example.siaga.view.utils.SessionLogin
import com.example.siaga.viewmodel.absen.AbsenActivity
import com.example.siaga.view.history.HistoryActivity


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var session: SessionLogin

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setInitLayout()

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
        session = SessionLogin(this)
        session.checkLogin()

        binding.cvAbsenMasuk.setOnClickListener {
            val intent = Intent(this, AbsenActivity::class.java)
            intent.putExtra(AbsenActivity.DATA_TITLE, "Absen Masuk")
            startActivity(intent)
        }

        binding.cvAbsenKeluar.setOnClickListener {
            val intent = Intent(this, AbsenActivity::class.java)
            intent.putExtra(AbsenActivity.DATA_TITLE, "Absen Keluar")
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
}


