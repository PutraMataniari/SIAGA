package com.example.siaga.view.login

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Patterns
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.siaga.R
import com.example.siaga.api.ApiClient
import com.example.siaga.datastore.DataStoreManager
import com.example.siaga.databinding.ActivityLoginBinding
import com.example.siaga.model.LoginResponse
import com.example.siaga.view.main.MainActivity
import com.example.siaga.view.signup.SignUpActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var dataStoreManager: DataStoreManager
    private lateinit var loadingDialog: androidx.appcompat.app.AlertDialog


    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Lokasi dibutuhkan untuk fitur di aplikasi", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dataStoreManager = DataStoreManager(this)

        // ✅ Inisialisasi loading dialog
        loadingDialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(layoutInflater.inflate(R.layout.dialog_loading, null))
            .setCancelable(false)
            .create()

        // Jika sudah login → langsung ke MainActivity
        checkLoginStatus()

        // Setup izin lokasi
        setupPermission()

        // Tombol login
        binding.btnLogin.setOnClickListener {
            val email = binding.inputEmail.text.toString().trim()
            val password = binding.inputPassword.text.toString().trim()

            // Validasi input email
            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Email tidak valid!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validasi password
            if (password.isEmpty() || password.length < 6) {
                Toast.makeText(this, "Password minimal 6 karakter!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Proses login
            performLogin(email, password)
        }

        // Link ke halaman daftar
        binding.tvSignUp.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }
    }

    private fun checkLoginStatus() = CoroutineScope(Dispatchers.IO).launch {
        val isLoggedIn = dataStoreManager.isLoggedIn.first()
        if (isLoggedIn) {
            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
            finish()
        }
    }

    private fun setupPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Izin sudah diberikan
            }
            else -> {
                locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun performLogin(email: String, password: String) {
        loadingDialog.show()

        ApiClient.instance.login(email, password).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                loadingDialog.dismiss()

                if (response.isSuccessful && response.body() != null) {
                    val loginResponse = response.body()!!
                    val token = loginResponse.token
                    val name = loginResponse.data.name
                    val nama = loginResponse.data.nama ?: loginResponse.data.name
                    val emailUser = loginResponse.data.email

                    // Simpan token & data user ke DataStore
                    lifecycleScope.launch(Dispatchers.IO) {
                        dataStoreManager.saveUserData(
                            token = token,
                            name = name,
                            nama = nama,
                            email = emailUser,
                            photo =  ""
                        )
                    }

                    Toast.makeText(this@LoginActivity, "Login berhasil", Toast.LENGTH_SHORT).show()

                    // Pindah ke MainActivity
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()

                } else {
                    val errorMsg = try {
                        val jObj = JSONObject(response.errorBody()?.string() ?: "{}")
                        jObj.getString("message")
                    } catch (e: Exception) {
                        "Login gagal, periksa email & password"
                    }
                    Toast.makeText(this@LoginActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                loadingDialog.dismiss()
                Toast.makeText(this@LoginActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

//    private fun showLoading() {
//        if (loadingDialog == null) {
//            val progressBar = ProgressBar(this)
//            val builder = AlertDialog.Builder(this)
//                .setView(progressBar)
//                .setCancelable(false)
//            loadingDialog = builder.create()
//        }
//        loadingDialog?.show()
//    }
//
//    private fun hideLoading() {
//        loadingDialog?.dismiss()
//    }
}
