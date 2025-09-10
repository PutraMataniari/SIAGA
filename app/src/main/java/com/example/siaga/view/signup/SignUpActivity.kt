package com.example.siaga.view.signup

import android.app.Activity
import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.siaga.R
import com.example.siaga.api.ApiClient
import com.example.siaga.databinding.ActivitySignupBinding
import com.example.siaga.model.RegisterResponse
import com.example.siaga.view.login.LoginActivity
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.IOException
import java.util.*

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private var profileImageUri: Uri? = null
    private val PICK_IMAGE_REQUEST = 100
    private val REQUEST_GALLERY = 100
    private val REQUEST_CAMERA = 101
    private val MAX_IMAGE_SIZE = 5 * 1024 * 1024 // 5MB

    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        progressDialog = ProgressDialog(this).apply {
            setMessage("Mendaftarkan akun")
            setCancelable(false)
        }

        // Klik foto profil → pilih dari galeri
        binding.profileImage1.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        // Klik tanggal lahir → buka date picker
        binding.inputTanggalLhr.setOnClickListener {
            showDatePicker()
        }

        // Tombol daftar
        binding.btnSignIn.setOnClickListener {
            registerUser()
        }

        // Dropdown Jabatan
        val jabatannList = listOf("Kabag", "Kasubag", "Pelaksana")
        val jabatanAdapter = ArrayAdapter(this, R.layout.list_item_dropdown, jabatannList)
        binding.inputJabatan.setAdapter(jabatanAdapter)

        // Dropdown Bagian
        val bagianList = listOf(
            "Keuangan, Umum, Logistik",
            "Teknis Penyelenggaraan Pemilu, ParHumas",
            "Perencanaan, Data dan Informasi",
            "Hukum dan Sumber Daya Manusia"
        )
        val bagianAdapter = ArrayAdapter(this, R.layout.list_item_dropdown, bagianList)
        binding.inputBagian.setAdapter(bagianAdapter)
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, d, m, y ->
            //Format D-M-Y
            val tanggalFormat = String.format("%04d-%02d-%02d", d, m + 1, y)
            binding.inputTanggalLhr.setText(tanggalFormat)
        }, year, month, day).show()
    }

    private fun registerUser() {
        val nama = binding.inputNama.text.toString().trim()
        val nip = binding.inputNIP.text.toString().trim()
        val email = binding.inputEmail.text.toString().trim()
        val noTelp = binding.inputNoTelp.text.toString().trim()
        val tanggalLahir = binding.inputTanggalLhr.text.toString().trim()
        val jabatan = binding.inputJabatan.text.toString().trim()
        val bagian = binding.inputBagian.text.toString().trim()
        val subBagian = binding.inputSubBagian.text.toString().trim()
        val password = binding.inputPassword.text.toString().trim()
        val passwordConfirmation = binding.ConfirmPassword.text.toString().trim()

        //Validasi untuk field yang kosong
        if (nama.isEmpty() || nip.isEmpty() || email.isEmpty() || noTelp.isEmpty() ||
            tanggalLahir.isEmpty() || jabatan.isEmpty() || bagian.isEmpty() ||
            password.isEmpty() || passwordConfirmation.isEmpty()) {
            Toast.makeText(this, "Semua field wajib diisi!", Toast.LENGTH_SHORT).show()
            return
        }

        //Validasi password cocok
        if (password != passwordConfirmation) {
            Toast.makeText(this, "Password tidak cocok", Toast.LENGTH_SHORT).show()
            return
        }

        // Validasi foto profil wajib
        if (profileImageUri == null) {
            Toast.makeText(this, "Foto profil wajib diisi!", Toast.LENGTH_SHORT).show()
            return
        }

        //Tampilkan loading
        progressDialog.show()

        val namaBody = RequestBody.create("text/plain".toMediaTypeOrNull(), nama)
        val nipBody = RequestBody.create("text/plain".toMediaTypeOrNull(), nip)
        val emailBody = RequestBody.create("text/plain".toMediaTypeOrNull(), email)
        val noTelpBody = RequestBody.create("text/plain".toMediaTypeOrNull(), noTelp)
        val tglBody = RequestBody.create("text/plain".toMediaTypeOrNull(), tanggalLahir)
        val jabatanBody = RequestBody.create("text/plain".toMediaTypeOrNull(), jabatan)
        val bagianBody = RequestBody.create("text/plain".toMediaTypeOrNull(), bagian)
        val subBagianBody = RequestBody.create("text/plain".toMediaTypeOrNull(), subBagian)
        val passwordBody = RequestBody.create("text/plain".toMediaTypeOrNull(), password)
        val passwordConfirmationBody = RequestBody.create("text/plain".toMediaTypeOrNull(), passwordConfirmation)

        // Buat MultipartBody.Part untuk foto profil
        val file = uriToFile(profileImageUri!!)
        if (file == null || !file.exists()) {
            progressDialog.dismiss()
            Toast.makeText(this, "Gagal membaca file foto profil", Toast.LENGTH_SHORT).show()
            return
        }
        if (file.length() > MAX_IMAGE_SIZE) {
            progressDialog.dismiss()
            Toast.makeText(this, "Ukuran foto maksimal 5MB!", Toast.LENGTH_LONG).show()
            return
        }

        val requestFile = RequestBody.create("image/*".toMediaTypeOrNull(), file)
        val fotoPart = MultipartBody.Part.createFormData("foto_profil", file.name, requestFile)

        ApiClient.instance.register(
            fotoPart,
            namaBody,
            nipBody,
            emailBody,
            noTelpBody,
            tglBody,
            jabatanBody,
            bagianBody,
            subBagianBody,
            passwordBody,
            passwordConfirmationBody
        ).enqueue(object : Callback<RegisterResponse> {
            override fun onResponse(call: Call<RegisterResponse>, response: Response<RegisterResponse>) {

                // Selalu tutup progressDialog di awal onResponse
                progressDialog.dismiss()

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    Toast.makeText(this@SignUpActivity, "Register sukses: ${body.message}", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@SignUpActivity, LoginActivity::class.java))
                    finish()
                } else {

                    // Tangani error validasi atau error server
                    val errorMsg = response.errorBody()?.string() ?: "Terjadi kesalahan saat register"
                    Toast.makeText(this@SignUpActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {

                // Selalu tutup progressDialog di onFailure juga
                progressDialog.dismiss()
                Toast.makeText(this@SignUpActivity, "Gagal: ${t.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        })

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            profileImageUri = data.data
            binding.profileImage1.setImageURI(profileImageUri)
        }
    }

    private fun uriToFile(uri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val tempFile = File.createTempFile("profile_", ".jpg", cacheDir)
            tempFile.outputStream().use { fileOut ->
                inputStream?.copyTo(fileOut)
            }
            tempFile
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}
