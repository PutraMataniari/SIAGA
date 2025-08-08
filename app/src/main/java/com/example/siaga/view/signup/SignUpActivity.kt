package com.example.siaga.view.signup

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.siaga.databinding.ActivitySignupBinding
import com.example.siaga.view.login.LoginActivity

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

            // Proses pendaftaran bisa ditambahkan di sini (API call)
            Toast.makeText(this, "Pendaftaran berhasil, silakan login", Toast.LENGTH_SHORT).show()

            // Arahkan kembali ke LoginActivity
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
