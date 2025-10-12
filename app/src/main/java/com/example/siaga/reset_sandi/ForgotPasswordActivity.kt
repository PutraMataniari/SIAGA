package com.example.siaga.reset_sandi

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.siaga.api.ApiClient
import com.example.siaga.databinding.ActivityForgotPasswordBinding
import com.example.siaga.model.ResetResponse
import com.example.siaga.R
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ForgotPasswordActivity : AppCompatActivity() {
    private lateinit var binding: ActivityForgotPasswordBinding
    private var loadingDialog: AlertDialog? = null

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

            showLoadingDialog("Sedang mengirim kode OTP ke email Anda...")

            ApiClient.instance.requestReset(email).enqueue(object : Callback<ResetResponse> {
                override fun onResponse(call: Call<ResetResponse>, response: Response<ResetResponse>) {
                    hideLoadingDialog()
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
                    hideLoadingDialog()
                    Toast.makeText(this@ForgotPasswordActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    /** Menampilkan dialog loading */
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

    /** Menyembunyikan dialog loading */
    private fun hideLoadingDialog() {
        loadingDialog?.dismiss()
    }
}
