package com.example.siaga.view.login

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.siaga.api.ApiClient
import com.example.siaga.api.LoginResponse
import com.example.siaga.datastore.DataStoreManager
import com.example.siaga.databinding.ActivityLoginBinding
import com.example.siaga.view.main.MainActivity
import com.example.siaga.view.signup.SignUpActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var dataStoreManager: DataStoreManager

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Lokasi dibutuhkan untuk fitur di aplikasi", Toast.LENGTH_SHORT)
                .show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("LoginActivity", "onCreate called")
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dataStoreManager = DataStoreManager(this)

        // Cek apakah user sudah login
        checkLoginStatus()

        setupPermission()
        setupLoginButton()
        setupSignUpLink()
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
                // Izin sudah ada
            }

            else -> {
                locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun setupLoginButton() {
        binding.btnLogin.setOnClickListener {
            val email = binding.inputEmail.text.toString().trim()
            val password = binding.inputPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Semua kolom harus diisi!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            performLogin(email, password)
        }
    }

    private fun performLogin(email: String, password: String) {
        val call = ApiClient.instance.login(email, password)

        call.enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    val token = loginResponse?.token
                    val name = loginResponse?.user?.name ?: "User"
//                    val nip = "123456789" // Misal dari API, atau tambahkan ke LoginResponse

                    if (token != null) {
                        // Simpan semua data ke DataStore
                        CoroutineScope(Dispatchers.IO).launch {
                            dataStoreManager.saveUserData(
                                token = token,
                                name = name,
                                email = email
                            )
                        }

                        Toast.makeText(this@LoginActivity, "Login berhasil", Toast.LENGTH_SHORT)
                            .show()

                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    }
                } else {
                    Toast.makeText(
                        this@LoginActivity,
                        "Login gagal: ${response.message()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                Toast.makeText(this@LoginActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })

    }
    private fun setupSignUpLink() {
        binding.tvSignUp.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }
    }
}