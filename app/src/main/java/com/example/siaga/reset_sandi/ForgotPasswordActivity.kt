package com.example.siaga.reset_sandi

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.siaga.api.ApiClient
import com.example.siaga.databinding.ActivityForgotPasswordBinding
import com.example.siaga.model.ResetResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ForgotPasswordActivity : AppCompatActivity() {
    private lateinit var binding: ActivityForgotPasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRequestOtp.setOnClickListener {
            val email = binding.inputEmail.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Masukkan email!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            ApiClient.instance.requestReset(email).enqueue(object : Callback<ResetResponse> {
                override fun onResponse(call: Call<ResetResponse>, response: Response<ResetResponse>) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        Toast.makeText(this@ForgotPasswordActivity, body?.message ?: "OTP dikirim ke email", Toast.LENGTH_SHORT).show()

                        val intent = Intent(this@ForgotPasswordActivity, VerifyOtpActivity::class.java)
                        intent.putExtra("email", email)
                        startActivity(intent)
                    } else {
                        Toast.makeText(this@ForgotPasswordActivity, "Gagal request OTP", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ResetResponse>, t: Throwable) {
                    Toast.makeText(this@ForgotPasswordActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}
