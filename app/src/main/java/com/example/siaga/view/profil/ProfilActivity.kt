package com.example.siaga.view.profil

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.siaga.databinding.ActivityProfilBinding
import com.example.siaga.datastore.DataStoreManager
import com.example.siaga.view.login.LoginActivity
import com.example.siaga.view.main.MainActivity
import com.example.siaga.view.profil.EditProfilPhotoBottomSheetFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ProfilActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfilBinding
    private lateinit var dataStoreManager: DataStoreManager
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfilBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inisialisasi DataStoreManager
        dataStoreManager = DataStoreManager(this)

        // Setup UI
        setupIcBack()
        setupProfileImage()
        setupLogoutButton()
    }


    //   Mengatur icon back atau kembali
    private fun setupIcBack() {
        binding.icback.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    /**
     * Mengatur perilaku gambar profil
     */
    private fun setupProfileImage() {
        // Ketika gambar profil diklik, arahkan ke halaman edit profile/foto
        binding.profileImage.setOnClickListener {
            // Contoh: Arahkan ke halaman edit profile/foto
            val bottomSheet = EditProfilPhotoBottomSheetFragment()
            bottomSheet.show(supportFragmentManager, "EditProfilePhotoBottomSheet")
//            val intent = Intent(this, EditProfilPhotoBottomSheetFragment::class.java)
//            startActivity(intent)
        }
    }

    /**
     * Mengatur tombol Logout
     */
    private fun setupLogoutButton() {
        binding.btnLogout.setOnClickListener {
            // Buat AlertDialog konfirmasi
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Keluar")
            builder.setMessage("Apakah Anda yakin ingin keluar dari aplikasi?")

            // Tombol "Ya"
            builder.setPositiveButton("Ya") { _, _ ->
                scope.launch {
                    dataStoreManager.logout() // Hapus sesi

                    val intent = Intent(this@ProfilActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish() // Tutup ProfileActivity
                }
            }

            // Tombol "Tidak"
            builder.setNegativeButton("Tidak") { dialog, _ ->
                dialog.dismiss() // Tutup dialog, tetap di ProfileActivity
            }

            // Tampilkan dialog
            builder.show()
        }
    }
}