package com.example.siaga.view.profil

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.siaga.R
import com.example.siaga.api.ApiClient
import com.example.siaga.databinding.ActivityProfilBinding
import com.example.siaga.datastore.DataStoreManager
import com.example.siaga.model.ProfilResponse
import com.example.siaga.view.login.LoginActivity
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class ProfilActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfilBinding
    private lateinit var dataStoreManager: DataStoreManager
    private var loadingDialog: AlertDialog? = null

    private val editProfileLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data?.getBooleanExtra("refreshProfile", false) == true) {
            // 🔹 Ambil ulang data profil setelah update
            loadProfilData()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfilBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dataStoreManager = DataStoreManager(this)

        setupIcBack()
        setupEditProfilButton()
        setupLogoutButton()

        // 🔹 Ambil data profil saat Activity dibuka
        loadProfilData()
    }

    /**
     * Tombol kembali → kembali ke MainActivity
     */
    private fun setupIcBack() {
        binding.icback.setOnClickListener {
            finish() // lebih baik daripada startActivity(MainActivity)
        }
    }

    /**
     * Tombol untuk edit profil → buka EditProfilActivity
     */
    private fun setupEditProfilButton() {
        binding.EditProfil.setOnClickListener {
            val intent = Intent(this, EditProfilActivity::class.java)
            editProfileLauncher.launch(intent)
        }
    }


    /**
     * Tombol logout → hapus session + kembali ke LoginActivity
     */
    private fun setupLogoutButton() {
        binding.btnLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Keluar")
                .setMessage("Apakah Anda yakin ingin keluar dari aplikasi?")
                .setPositiveButton("Ya") { _, _ ->
                    lifecycleScope.launch {
                        dataStoreManager.logout()
                        val intent = Intent(this@ProfilActivity, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                }
                .setNegativeButton("Tidak") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    /** ==================== LOADING DIALOG ==================== */
    private fun showLoadingDialog(message: String) {
        if (loadingDialog == null) {
            val builder = AlertDialog.Builder(this)
            val inflater = layoutInflater
            val view = inflater.inflate(R.layout.dialog_loading, null)
            val textMessage = view.findViewById<TextView>(R.id.textMessage)
            textMessage.text = message
            builder.setView(view)
            builder.setCancelable(false)
            loadingDialog = builder.create()
        } else {
            val textMessage = loadingDialog?.findViewById<TextView>(R.id.textMessage)
            textMessage?.text = message
        }
        loadingDialog?.show()
    }

    private fun hideLoadingDialog() {
        loadingDialog?.dismiss()
    }

    /**
     * Ambil data profil dari API
     */
    private fun loadProfilData() {
        lifecycleScope.launch {
            try {
                // 🔹 Ambil token dari DataStore
                val token = dataStoreManager.getToken()
                if (token.isNullOrEmpty()) {
                    Toast.makeText(this@ProfilActivity, "Token tidak ditemukan, silakan login ulang", Toast.LENGTH_SHORT).show()
                    goToLogin()
                    return@launch
                }

                showLoadingDialog("Memuat data profil...")

                // 🔹 Ambil data profil dari API
                val response = ApiClient.instance.getProfil("Bearer $token")
                updateProfilUI(response)

            } catch (e: IOException) {
                Toast.makeText(this@ProfilActivity, "Gagal terhubung ke server", Toast.LENGTH_LONG).show()
            } catch (e: HttpException) {
                Toast.makeText(this@ProfilActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this@ProfilActivity, "Terjadi kesalahan: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                hideLoadingDialog()
            }
        }
    }

    /**
     * Update UI setelah data profil berhasil diambil
     */
    private fun updateProfilUI(response: ProfilResponse) {
        val profil = response.data
        if (profil?.pegawai == null) {
            Toast.makeText(this, "Data profil tidak ditemukan", Toast.LENGTH_SHORT).show()
            return
        }

        val pegawai = profil.pegawai

        // 🔹 Isi semua field profil
        binding.edtNama.setText(pegawai.nama ?: "")
        binding.edtNIP.setText(pegawai.nip ?: "")
        binding.edtJabatan.setText(pegawai.jabatan ?: "")
        binding.edtBagian.setText(pegawai.bagian ?: "")
        binding.edtNoTelp.setText(pegawai.no_telp ?: "")
        binding.edtEmail.setText(pegawai.email ?: "")

        // 🔹 Load foto profil dengan Glide
        Glide.with(this)
            .load(pegawai.foto_profil)
            .placeholder(R.drawable.ic_photo_camera)
            .error(R.drawable.ic_broken_image)
            .into(binding.profileImage)
    }


    /**
     * Navigasi ke halaman login jika token hilang / expired
     */
    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
