package com.example.siaga.view.main

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.siaga.R
import com.example.siaga.databinding.ActivityMainBinding
import com.example.siaga.datastore.DataStoreManager
import com.example.siaga.view.absen.AbsenPerizinanActivity
import com.example.siaga.view.absen.AbsenMasukActivity
import com.example.siaga.view.absen.AbsenPulangActivity
import com.example.siaga.view.history.HistoryActivity
import com.example.siaga.view.login.LoginActivity
import com.example.siaga.view.profil.ProfilActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var dataStoreManager: DataStoreManager
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inisialisasi DataStoreManager
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
            val intent = Intent(this, AbsenMasukActivity::class.java)
            intent.putExtra(AbsenMasukActivity.DATA_TITLE, "Absen Masuk")
            startActivity(intent)
        }

        binding.cvAbsenKeluar.setOnClickListener {
            val intent = Intent(this, AbsenPulangActivity::class.java)
            intent.putExtra(AbsenPulangActivity.DATA_TITLE, "Absen Pulang")
            startActivity(intent)
        }

        binding.cvPerizinan.setOnClickListener {
            val intent = Intent(this, AbsenPerizinanActivity::class.java)
            intent.putExtra(AbsenPerizinanActivity.DATA_TITLE, "Izin")
            startActivity(intent)
        }

        binding.cvHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
    }

    /**
     * Setup menu popup pada foto profil
     */
    private fun setupProfile() {
        binding.imageProfile.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menuInflater.inflate(R.menu.profile_menu, popup.menu)

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    // Menu Lihat Profil
                    R.id.menu_profile -> {
                        val intent = Intent(this, ProfilActivity::class.java)
                        startActivity(intent)
                        true
                    }

                    // Menu Logout
                    R.id.menu_logout -> {
                        showLogoutDialog()
                        true
                    }

                    else -> false
                }
            }
            popup.show()
        }
    }

    /**
     * Dialog konfirmasi untuk logout
     */
    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Konfirmasi Logout")
            .setMessage("Apakah Anda yakin ingin keluar dari aplikasi?")
            .setPositiveButton("Ya") { _, _ ->
                scope.launch {
                    dataStoreManager.logout()
                    val intent = Intent(this@MainActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}
