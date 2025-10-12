package com.example.siaga.reset_sandi

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.siaga.R
import com.example.siaga.api.ApiClient
import com.example.siaga.databinding.ActivityVerifyOtpBinding
import com.example.siaga.model.ResetResponse
import com.example.siaga.view.login.LoginActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class VerifyOtpActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVerifyOtpBinding
    private var loadingDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerifyOtpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val email = intent.getStringExtra("email") ?: ""

        binding.btnResetPassword.setOnClickListener {
            val otp = binding.inputOtp.text.toString().trim()
            val newPass = binding.inputNewPassword.text.toString().trim()

            if (otp.isEmpty() || newPass.length < 6) {
                Toast.makeText(this, "Lengkapi data (OTP & password min 6 karakter)!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            //tampilkan loading dialog
            showLoadingDialog("Memperbarui password Anda...")

            ApiClient.instance.resetPassword(email, otp, newPass)
                .enqueue(object : Callback<ResetResponse> {
                    override fun onResponse(call: Call<ResetResponse>, response: Response<ResetResponse>) {
                        if (response.isSuccessful) {
                            val body = response.body()
                            Toast.makeText(
                                this@VerifyOtpActivity,
                                body?.message ?: "Password berhasil direset",
                                Toast.LENGTH_SHORT
                            ).show()

                            //Ke LoginActivity
                            val intent = Intent(this@VerifyOtpActivity, LoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(
                                this@VerifyOtpActivity,
                                "OTP salah atau kadaluwarsa",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onFailure(call: Call<ResetResponse>, t: Throwable) {
                        Toast.makeText(this@VerifyOtpActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
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
