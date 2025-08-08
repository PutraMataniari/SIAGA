package com.example.siaga.view.signup

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.siaga.api.ApiClient
import com.example.siaga.api.SignupRequest
import com.example.siaga.api.ApiResponse
import com.example.siaga.databinding.ActivitySignupBinding
import com.example.siaga.view.login.LoginActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSignIn.setOnClickListener {
            val nama = binding.inputNama.text.toString().trim()
            val email = binding.inputEmail.text.toString().trim()
            val password = binding.inputPassword.text.toString().trim()
            val confirmPassword = binding.ConfirmPassword.text.toString().trim()

            if (nama.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Semua kolom harus diisi!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Password dan konfirmasi tidak sama!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            signupUser(nama, email, password, confirmPassword)
        }
    }

    private fun signupUser(name: String, email: String, password: String, confirmPassword: String) {
        val request = SignupRequest(
            name = name,
            email = email,
            password = password,
            password_confirmation = confirmPassword
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
}
