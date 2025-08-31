package com.example.siaga.view.signup

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.siaga.R
import com.example.siaga.api.ApiClient
import com.example.siaga.api.ApiResponse
import com.example.siaga.api.SignupRequest
import com.example.siaga.databinding.ActivitySignupBinding
import com.example.siaga.view.login.LoginActivity
import de.hdodenhof.circleimageview.CircleImageView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.util.*

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private var profileImageUri: Uri? = null

    private val REQUEST_GALLERY = 100
    private val REQUEST_CAMERA = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Klik foto profil → pilih kamera/galeri
        binding.profileImage1.setOnClickListener {
            showImagePickerDialog()
        }

        // Klik tanggal lahir → buka date picker
        binding.inputTanggalLhr.setOnClickListener {
            showDatePicker()
        }

        // Tombol daftar
        binding.btnSignIn.setOnClickListener {
            registerUser()
        }

        //Dropdown Jabatan
        val jabatannList = listOf(
            "Kabag",
            "Kasubag",
            "Pelaksana"
        )

        val jabatanAdapter = ArrayAdapter(this, R.layout.list_item_dropdown, jabatannList)
        binding.inputJabatan.setAdapter(jabatanAdapter)

        //Dropdown Bagian
        val bagianList = listOf(
                "Keuangan, Umum, Logistik",
                "Teknis Penyelenggaraan Pemilu, ParHumas",
                "Perencanaan, Data dan Informasi",
                "Hukum dan Sumber Daya Manusia"
        )

        val bagianAdapter = ArrayAdapter(this, R.layout.list_item_dropdown, bagianList)
        binding.inputBagian.setAdapter(bagianAdapter)



    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Ambil Foto", "Pilih dari Galeri")
        AlertDialog.Builder(this)
            .setTitle("Pilih Foto Profil")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                        startActivityForResult(cameraIntent, REQUEST_CAMERA)
                    }
                    1 -> {
                        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        startActivityForResult(galleryIntent, REQUEST_GALLERY)
                    }
                }
            }
            .show()
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, y, m, d ->
            binding.inputTanggalLhr.setText(String.format("%02d/%02d/%d", d, m + 1, y))
        }, year, month, day).show()
    }

    private fun registerUser() {
        val nama = binding.inputNama.text.toString().trim()
        val nip = binding.inputNIP.text.toString().trim()
        val email = binding.inputEmail.text.toString().trim()
        val tanggalLahir = binding.inputTanggalLhr.text.toString().trim()
        val jabatan = binding.inputJabatan.text.toString().trim()
        val bagian = binding.inputBagian.text.toString().trim()
        val subBagian = binding.inputSubBagian.text.toString().trim()
        val password = binding.inputPassword.text.toString().trim()
        val confirmPassword = binding.ConfirmPassword.text.toString().trim()

        if (nama.isEmpty() || nip.isEmpty() || email.isEmpty() || tanggalLahir.isEmpty()
            || jabatan.isEmpty() || bagian.isEmpty() || subBagian.isEmpty()
            || password.isEmpty() || confirmPassword.isEmpty()
            || profileImageUri == null
        ) {
            Toast.makeText(this, "Semua kolom harus terisi", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Password tidak sama", Toast.LENGTH_SHORT).show()
            return
        }

        // Lanjutkan request API signup
        val request = SignupRequest(
            name = nama,
            email = email,
            password = password,
            password_confirmation = confirmPassword
            // tambahkan field lain ke request sesuai kebutuhan API
        )

        ApiClient.apiService.signup(request)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful && response.body()?.status == true) {
                        Toast.makeText(applicationContext, response.body()?.message ?: "Pendaftaran berhasil", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@SignUpActivity, LoginActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(applicationContext, response.body()?.message ?: "Pendaftaran gagal", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Toast.makeText(applicationContext, "Gagal terhubung ke server: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_GALLERY -> {
                    profileImageUri = data?.data
                    binding.profileImage1.setImageURI(profileImageUri)
                }
                REQUEST_CAMERA -> {
                    val photo: Bitmap? = data?.extras?.get("data") as? Bitmap
                    if (photo != null) {
                        binding.profileImage1.setImageBitmap(photo)
                        // Untuk menyimpan gambar kamera, perlu simpan ke file dan dapatkan URI
                    }
                }
            }
        }
    }
}
