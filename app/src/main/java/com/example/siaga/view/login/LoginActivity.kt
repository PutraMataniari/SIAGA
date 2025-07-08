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
import com.example.siaga.databinding.ActivityLoginBinding
import com.example.siaga.view.main.MainActivity
import com.example.siaga.view.utils.SessionLogin
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var session: SessionLogin
    private lateinit var database: DatabaseReference

    // Modern permission request
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(
                this,
                "Location permission is required for app functionality",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase
//        database = Firebase.database.reference.child("users")

        session = SessionLogin(applicationContext as MainActivity)

        checkLoginStatus()
        setupPermission()
        setupLoginButton()
    }

    private fun checkLoginStatus() {
        if (session.isLoggedIn()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun setupPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission already granted
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                Toast.makeText(
                    this,
                    "Location permission is required for app functionality",
                    Toast.LENGTH_LONG
                ).show()
                locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            else -> {
                locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun setupLoginButton() {
        binding.btnLogin.setOnClickListener {
            val name = binding.inputNama.text.toString().trim()
            val nip = binding.inputNip.text.toString().trim()
            val password = binding.inputPassword.text.toString().trim()

            when {
                name.isEmpty() || nip.isEmpty() || password.isEmpty() -> {
                    Toast.makeText(
                        this, "All fields are required!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else -> {
                    val userId = database.push().key ?: run {
                        Log.e("LoginActivity", "Failed to generate user ID")
                        Toast.makeText(
                            this,
                            "Registration failed, please try again",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setOnClickListener
                    }

                    saveUserData(userId, name, nip, password)
                }
            }
        }
    }

    private fun saveUserData(userId: String, name: String, nip: String, password: String) {
        val user = User(name, nip, password)

        database.child(userId).setValue(user)
            .addOnSuccessListener {
                session.createLoginSession(name, nip, password)
                Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Login failed: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e("LoginActivity", "Failed to save data", e)
            }
    }

    data class User(
        val name: String,
        val nip: String,
        val password: String
    )
}

